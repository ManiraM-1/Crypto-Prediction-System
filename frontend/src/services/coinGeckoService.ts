const COINGECKO_API = "https://api.coingecko.com/api/v3";

// Map trading pairs to CoinGecko IDs
const SYMBOL_TO_COINGECKO_ID: Record<string, string> = {
  "BTC/USDT": "bitcoin",
  "ETH/USDT": "ethereum",
  "SOL/USDT": "solana",
  "XRP/USDT": "ripple",
  "ADA/USDT": "cardano",
  "DOGE/USDT": "dogecoin",
};

export interface CoinData {
  id: string;
  symbol: string;
  name: string;
  image: string;
  current_price: number;
  market_cap: number;
  market_cap_rank: number;
  total_volume: number;
  price_change_24h?: number;
  price_change_percentage_24h?: number;
  price_change_percentage_7d?: number;
  circulating_supply: number;
  total_supply?: number;
  ath: number;
  ath_change_percentage?: number;
  atl: number;
  atl_change_percentage?: number;
  sparkline_in_7d?: {
    price: number[];
  };
}

export interface ChartData {
  prices: [number, number][];
  market_caps: [number, number][];
  total_volumes: [number, number][];
}

export class CoinGeckoService {
  static getCoinGeckoId(symbol: string): string | null {
    return SYMBOL_TO_COINGECKO_ID[symbol] || null;
  }

  static async getCoinData(symbol: string): Promise<CoinData | null> {
    const coinId = this.getCoinGeckoId(symbol);
    if (!coinId) return null;

    try {
      const response = await fetch(
        `${COINGECKO_API}/coins/markets?vs_currency=usd&ids=${coinId}&order=market_cap_desc&sparkline=true&price_change_percentage=7d`,
        {
          headers: {
            'Accept': 'application/json',
          },
        }
      );
      
      if (!response.ok) {
        if (response.status === 429) {
          console.warn("CoinGecko rate limit reached");
        } else {
          console.error("CoinGecko API error:", response.status);
        }
        return null;
      }

      const data = await response.json();
      return data[0] || null;
    } catch (error) {
      console.error("Error fetching coin data:", error);
      return null;
    }
  }

  static async getChartData(symbol: string, days: number = 7): Promise<ChartData | null> {
    const coinId = this.getCoinGeckoId(symbol);
    if (!coinId) return null;

    try {
      const response = await fetch(
        `${COINGECKO_API}/coins/${coinId}/market_chart?vs_currency=usd&days=${days}`
      );
      
      if (!response.ok) {
        console.error("CoinGecko API error:", response.status);
        return null;
      }

      return await response.json();
    } catch (error) {
      console.error("Error fetching chart data:", error);
      return null;
    }
  }
}
