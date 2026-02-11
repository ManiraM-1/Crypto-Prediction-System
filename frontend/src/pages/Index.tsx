import { useState, useRef, useEffect } from "react"; // <-- CHANGE 1: Import useRef and useEffect
import axios from "axios";
import { ControlPanel } from "@/components/ControlPanel";
import { DecisionPanel } from "@/components/DecisionPanel";
import { PriceForecastPanel } from "@/components/PriceForecastPanel";
import { SentimentPanel } from "@/components/SentimentPanel";
import { CoinInfoPanel } from "@/components/CoinInfoPanel";
import { useToast } from "@/hooks/use-toast";

// The interface from your file, unchanged
interface AnalysisResult {
  signal: "BUY" | "SELL" | "HOLD";
  confidence: string;
  summary: string;
  timestamp: string;
  parameters: {
    symbol: string;
    minutes: number;
    analysisTime: string;
  };
  quantitativeAnalysis: {
    modelVersion: string;
    historicalData: Array<{
      time: number;
      price: number;
      volume: number;
    }>;
    forecastData: Array<{
      time: number;
      price: number;
      confidenceLower?: number;
      confidenceUpper?: number;
    }>;
    metrics: {
      predictedChange: number;
      predictedPrice: number;
      currentPrice: number;
      supportLevel?: number;
      resistanceLevel?: number;
    };
  };
  sentimentAnalysis: {
    marketMoodScore: number;
    sentimentLabel: string;
    topHeadlines: Array<{
      title: string;
      source?: string;
      timestamp: string;
      sentiment: string;
      relevanceScore?: number;
    }>;
    statistics: {
      totalArticles: number;
      positiveCount: number;
      negativeCount: number;
      neutralCount: number;
      sentimentTrend: string;
    };
  };
}

const Index = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [data, setData] = useState<AnalysisResult | null>(null);
  const [selectedCrypto, setSelectedCrypto] = useState<string>("BTC/USDT");
  const { toast } = useToast();

  // <-- CHANGE 2: Create a ref for our results section
  const resultsRef = useRef<HTMLDivElement>(null);

  // <-- CHANGE 3: Add an effect to scroll when data changes
  useEffect(() => {
    // If 'data' is not null (meaning we have results) and the ref is attached...
    if (data && resultsRef.current) {
      // ...scroll to it smoothly.
      resultsRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [data]); // This effect runs every time the 'data' state changes

  const handleAnalyze = async (symbol: string, minutes: number) => {
    setIsLoading(true);
    setData(null); // Clear previous results
    try {
      // This is the clean, original API call.
      const response = await axios.get<AnalysisResult>("https://maniram-crypto-backend.hf.space/api/v1/decision", {
        params: { symbol, minutes },
      });
      setData(response.data); // Set the REAL data
    } catch (error: any) {
      console.error("Error fetching analysis:", error);
      
      let errorMessage = "Failed to fetch analysis. Please try again.";
      
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message.includes("Network Error")) {
        errorMessage = "Unable to connect to the server. Please ensure the backend is running on https://maniram-crypto-backend.hf.space";
      }
      
      toast({
        title: "Analysis Failed",
        description: errorMessage,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Header (unchanged) */}
        <header className="mb-8 text-center">
          <h1 className="text-4xl font-bold mb-2 bg-gradient-to-r from-primary via-success to-primary bg-clip-text text-transparent">
            Crypto Trading Recommendation System
          </h1>
          <p className="text-muted-foreground">
            AI-powered cryptocurrency analysis combining price forecasts and market sentiment
          </p>
        </header>

        {/* Control Panel (unchanged) */}
        <div className="mb-8">
          <ControlPanel 
            onAnalyze={handleAnalyze} 
            onCryptoChange={setSelectedCrypto}
            isLoading={isLoading} 
          />
        </div>

        {/* Live Coin Info Panel (unchanged) */}
        {selectedCrypto && (
          <div className="mb-8">
            <CoinInfoPanel symbol={selectedCrypto} />
          </div>
        )}

        {/* <-- CHANGE 4: Wrap the results/placeholder in a div and attach the ref --> */}
        <div ref={resultsRef} className="results-section">
          {data ? (
            <>
              {/* Decision Panel (unchanged) */}
              <div className="mb-8">
                <DecisionPanel
                  signal={data.signal}
                  confidence={data.confidence}
                  summary={data.summary}
                  timestamp={data.timestamp}
                  symbol={data.parameters.symbol}
                  minutes={data.parameters.minutes}
                />
              </div>

              {/* Evidence Panels (unchanged) */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <PriceForecastPanel
                  historicalData={data.quantitativeAnalysis.historicalData}
                  forecastData={data.quantitativeAnalysis.forecastData}
                  metrics={data.quantitativeAnalysis.metrics}
                  confidence={data.confidence}
                />
                <SentimentPanel
                  marketMoodScore={data.sentimentAnalysis.marketMoodScore}
                  sentimentLabel={data.sentimentAnalysis.sentimentLabel}
                  topHeadlines={data.sentimentAnalysis.topHeadlines}
                  statistics={data.sentimentAnalysis.statistics}
                />
              </div>
            </>
          ) : (
            <div className="text-center py-20">
              <div className="bg-card rounded-lg p-12 border border-border inline-block">
                <p className="text-xl text-muted-foreground">
                  {isLoading ? 'Analyzing...' : 'Select a cryptocurrency and click "Analyze Market" to begin'}
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Index;