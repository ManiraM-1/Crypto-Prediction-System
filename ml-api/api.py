import os
import sys
import numpy as np
import pandas as pd
import pickle
import requests
import tensorflow as tf
import xgboost as xgb
from flask import Flask, request, jsonify
from sklearn.preprocessing import RobustScaler
import warnings

# Configuration
app = Flask(__name__)
warnings.filterwarnings('ignore')

# Use current directory for compatibility
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BASE_MODEL_DIR = os.path.join(BASE_DIR, 'hybrid_models')
WINDOW_SIZE = 48

PREDICTION_HORIZONS = {
    1: '1h', 2: '2h', 3: '3h', 4: '4h', 6: '6h', 12: '12h'
}

# Global variables
lstm_models = {}
xgb_models = {}
scaler = None
FEATURES = []
N_FEATURES = 0

# ====================================================================
# MANUAL TECHNICAL INDICATORS
# ====================================================================

def calculate_rsi(series, period=14):
    delta = series.diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
    rs = gain / loss
    rsi = 100 - (100 / (1 + rs))
    return rsi

def calculate_macd(series, fast=12, slow=26, signal=9):
    ema_fast = series.ewm(span=fast, adjust=False).mean()
    ema_slow = series.ewm(span=slow, adjust=False).mean()
    macd_line = ema_fast - ema_slow
    signal_line = macd_line.ewm(span=signal, adjust=False).mean()
    macd_histogram = macd_line - signal_line
    return macd_line, signal_line, macd_histogram

def calculate_bollinger_bands(series, length=5, std=2):
    sma = series.rolling(window=length).mean()
    rolling_std = series.rolling(window=length).std()
    upper_band = sma + (rolling_std * std)
    lower_band = sma - (rolling_std * std)
    bandwidth = upper_band - lower_band
    return lower_band, sma, upper_band, bandwidth

def add_enhanced_features(df):
    # RSI
    df['RSI_14'] = calculate_rsi(df['Close'], period=14)
    
    # MACD
    macd, macd_signal, macd_hist = calculate_macd(df['Close'])
    df['MACD_12_26_9'] = macd
    df['MACDs_12_26_9'] = macd_signal
    df['MACDh_12_26_9'] = macd_hist
    
    # Bollinger Bands
    bbl, bbm, bbu, bbw = calculate_bollinger_bands(df['Close'], length=5, std=2)
    df['BBL_5_2.0'] = bbl
    df['BBM_5_2.0'] = bbm
    df['BBU_5_2.0'] = bbu
    df['BBB_5_2.0'] = bbw
    df['BBP_5_2.0'] = (df['Close'] - bbl) / bbw
    
    # Returns and Volatility
    df['Returns'] = df['Close'].pct_change()
    df['Log_Returns'] = np.log(df['Close'] / df['Close'].shift(1))
    df['Volatility_20'] = df['Returns'].rolling(window=20).std()
    
    # Momentum
    df['Momentum_10'] = df['Close'] - df['Close'].shift(10)
    df['ROC_10'] = (df['Close'] - df['Close'].shift(10)) / df['Close'].shift(10) * 100
    
    # Volume
    df['Volume_SMA_20'] = df['Volume'].rolling(window=20).mean()
    df['Volume_Ratio'] = df['Volume'] / df['Volume_SMA_20']
    
    return df

# ====================================================================
# LOAD MODELS
# ====================================================================

def load_all_models():
    global lstm_models, xgb_models, scaler, FEATURES, N_FEATURES

    print("Loading models from:", BASE_MODEL_DIR)

    # Load config
    config_path = os.path.join(BASE_MODEL_DIR, 'model_config.pkl')
    try:
        with open(config_path, 'rb') as f:
            config = pickle.load(f)
    except FileNotFoundError:
        print(f"❌ CRITICAL: model_config.pkl not found at {config_path}")
        print(f"Current dir: {os.getcwd()}")
        print(f"Directory listing: {os.listdir(os.getcwd())}")
        raise

    FEATURES = config['features']
    N_FEATURES = len(FEATURES)
    print(f"✅ Features loaded: {N_FEATURES}")

    # Load scaler
    scaler_path = os.path.join(BASE_MODEL_DIR, 'scaler.pkl')
    with open(scaler_path, 'rb') as f:
        scaler = pickle.load(f)
    print("✅ Scaler loaded")

    # Load models
    for h_hours, h_name in PREDICTION_HORIZONS.items():
        try:
            # Load LSTM
            lstm_path = os.path.join(BASE_MODEL_DIR, f'lstm_{h_name}.h5')
            lstm_model = tf.keras.models.load_model(lstm_path, compile=False)
            lstm_models[h_hours] = lstm_model

            # Load XGBoost
            xgb_path = os.path.join(BASE_MODEL_DIR, f'xgboost_{h_name}.json')
            xgb_model = xgb.Booster()
            xgb_model.load_model(xgb_path)
            xgb_models[h_hours] = xgb_model

            print(f"✅ Loaded {h_name} models")
        except Exception as e:
            print(f"⚠️ Warning loading {h_name}: {e}")
            pass

    print("\n✅ All models loaded successfully!")

# ====================================================================
# DATA FETCHING
# ====================================================================

def get_live_data(symbol):
    url = "https://public.coindcx.com/market_data/candles"
    api_symbol = "B-" + symbol.replace("/", "_")
    params = {
        'pair': api_symbol,
        'interval': '1h',
        'limit': WINDOW_SIZE + 50
    }
    response = requests.get(url, params=params, timeout=10)
    if response.status_code != 200:
        raise Exception(f"CoinDCX API Error: {response.status_code}")

    data = response.json()
    df = pd.DataFrame(data)
    df = df[['time', 'open', 'high', 'low', 'close', 'volume']].copy()
    df.columns = ['Timestamp', 'Open', 'High', 'Low', 'Close', 'Volume']
    for col in ['Open', 'High', 'Low', 'Close', 'Volume']:
        df[col] = df[col].astype(float)
    return df

def prepare_features(df):
    df = add_enhanced_features(df)
    df.dropna(inplace=True)

    if len(df) < WINDOW_SIZE:
        raise Exception(f"Need {WINDOW_SIZE} candles, got {len(df)}")

    df_window = df.tail(WINDOW_SIZE).copy()
    X = []
    
    for feat in FEATURES:
        if feat in df_window.columns:
            X.append(df_window[feat].values)
        else:
            X.append(np.zeros(WINDOW_SIZE))

    X = np.array(X).T
    return X

# ====================================================================
# API ENDPOINTS
# ====================================================================

@app.route('/', methods=['GET'])
def home():
    return jsonify({'status': 'running', 'msg': 'Crypto Prediction API'})

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'models_loaded': len(lstm_models)
    })

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.json
        symbol = data.get('symbol', 'BTC/USDT')
        minutes = int(data.get('minutes', 120))
        
        target_hours = round(minutes / 60)
        if target_hours == 0: target_hours = 1
        
        available = list(PREDICTION_HORIZONS.keys())
        horizon_hours = min(available, key=lambda x: abs(x - target_hours))

        if horizon_hours not in lstm_models:
            return jsonify({'error': f'Model for {horizon_hours}h not found'}), 400

        df_raw = get_live_data(symbol)
        X = prepare_features(df_raw)
        X_scaled = scaler.transform(X).reshape(1, WINDOW_SIZE, N_FEATURES)

        lstm_model = lstm_models[horizon_hours]
        lstm_features = lstm_model.predict(X_scaled, verbose=0)

        xgb_model = xgb_models[horizon_hours]
        dmatrix = xgb.DMatrix(lstm_features)
        prob_up = xgb_model.predict(dmatrix)[0]

        prediction = 'UP' if prob_up > 0.5 else 'DOWN'
        confidence = float(prob_up if prediction == 'UP' else 1 - prob_up)

        return jsonify({
            'symbol': symbol,
            'prediction': prediction,
            'confidence': round(confidence, 4),
            'current_price': float(df_raw['Close'].iloc[-1]),
            'status': 'success'
        })

    except Exception as e:
        return jsonify({'error': str(e), 'status': 'failed'}), 500

# ====================================================================
# MAIN
# ====================================================================

# Load models immediately
load_all_models()

if __name__ == '__main__':
    print("🚀 Starting Flask server on port 7860...")
    app.run(host='0.0.0.0', port=7860, debug=False)