import { useEffect, useState } from "react";
import { CoinGeckoService, CoinData } from "@/services/coinGeckoService";
import { LineChart, Line, ResponsiveContainer, Tooltip } from "recharts";
import { TrendingUp, TrendingDown, DollarSign, BarChart3, Activity, Coins } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";

interface CoinInfoPanelProps {
  symbol: string;
}

// Timeframe configuration
const TIMEFRAMES = [
  { label: '1H', value: '1h', days: 1 },
  { label: '2H', value: '2h', days: 1 },
  { label: '4H', value: '4h', days: 2 },
  { label: '6H', value: '6h', days: 3 },
  { label: '12H', value: '12h', days: 4 },
  { label: '24H', value: '24h', days: 7 },
  { label: '7D', value: '7d', days: 7 },
  { label: '30D', value: '30d', days: 30 }
];

export const CoinInfoPanel = ({ symbol }: CoinInfoPanelProps) => {
  const [coinData, setCoinData] = useState<CoinData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRateLimited, setIsRateLimited] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  
  // New state for timeframe selection
  const [selectedTimeframe, setSelectedTimeframe] = useState('7d');
  const [chartData, setChartData] = useState<any[]>([]);
  const [isLoadingChart, setIsLoadingChart] = useState(false);

  // Fetch coin basic data (existing logic)
  useEffect(() => {
    const CACHE_KEY = `coin_data_${symbol}`;
    const CACHE_TIMESTAMP_KEY = `coin_data_timestamp_${symbol}`;
    const CACHE_DURATION = 60000;

    const fetchCoinData = async (isInitialLoad = false) => {
      const cachedData = localStorage.getItem(CACHE_KEY);
      const cachedTimestamp = localStorage.getItem(CACHE_TIMESTAMP_KEY);
      
      if (cachedData && cachedTimestamp) {
        const cacheAge = Date.now() - parseInt(cachedTimestamp);
        if (cacheAge < CACHE_DURATION) {
          setCoinData(JSON.parse(cachedData));
          setLastUpdated(new Date(parseInt(cachedTimestamp)));
          setIsLoading(false);
          setIsRateLimited(false);
          return;
        }
      }

      if (isInitialLoad) {
        setIsLoading(true);
      }

      const data = await CoinGeckoService.getCoinData(symbol);
      
      if (data) {
        setCoinData(data);
        setIsRateLimited(false);
        const now = Date.now();
        setLastUpdated(new Date(now));
        localStorage.setItem(CACHE_KEY, JSON.stringify(data));
        localStorage.setItem(CACHE_TIMESTAMP_KEY, now.toString());
      } else {
        setIsRateLimited(true);
        if (!coinData && cachedData) {
          setCoinData(JSON.parse(cachedData));
          if (cachedTimestamp) {
            setLastUpdated(new Date(parseInt(cachedTimestamp)));
          }
        }
      }
      
      setIsLoading(false);
    };

    fetchCoinData(true);
    const interval = setInterval(() => fetchCoinData(false), 60000);
    return () => clearInterval(interval);
  }, [symbol]);

  // Fetch chart data based on selected timeframe
  useEffect(() => {
    const fetchChartData = async () => {
      if (!coinData) return;
      
      setIsLoadingChart(true);
      
      try {
        const config = TIMEFRAMES.find(tf => tf.value === selectedTimeframe);
        if (!config) return;
        
        const coinId = coinData.id;
        const url = `https://api.coingecko.com/api/v3/coins/${coinId}/market_chart?vs_currency=usd&days=${config.days}`;
        
        const response = await fetch(url);
        const data = await response.json();
        
        if (data.prices) {
          // Process data based on timeframe
          let processedData = data.prices;
          
          // For hourly views, limit to specific hours
          if (selectedTimeframe === '1h') {
            processedData = data.prices.slice(-60);
          } else if (selectedTimeframe === '2h') {
            processedData = data.prices.slice(-120);
          } else if (selectedTimeframe === '4h') {
            processedData = data.prices.slice(-240);
          } else if (selectedTimeframe === '6h') {
            processedData = data.prices.slice(-360);
          } else if (selectedTimeframe === '12h') {
            processedData = data.prices.slice(-720);
          } else if (selectedTimeframe === '24h') {
            processedData = data.prices.slice(-1440);
          }
          
          const formatted = processedData.map(([timestamp, price]: [number, number], index: number) => ({
            value: price,
            index,
            timestamp
          }));
          
          setChartData(formatted);
        }
      } catch (error) {
        console.error('Error fetching chart data:', error);
      } finally {
        setIsLoadingChart(false);
      }
    };
    
    fetchChartData();
  }, [selectedTimeframe, coinData]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: price < 1 ? 6 : 2,
    }).format(price);
  };

  const formatLargeNumber = (num: number) => {
    if (num >= 1e12) return `$${(num / 1e12).toFixed(2)}T`;
    if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`;
    if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`;
    return `$${num.toLocaleString()}`;
  };

  const formatSupply = (num: number) => {
    if (num >= 1e9) return `${(num / 1e9).toFixed(2)}B`;
    if (num >= 1e6) return `${(num / 1e6).toFixed(2)}M`;
    return num.toLocaleString();
  };

  const formatLastUpdated = () => {
    if (!lastUpdated) return '';
    const seconds = Math.floor((Date.now() - lastUpdated.getTime()) / 1000);
    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ago`;
  };

  // Calculate price change for selected timeframe
  const calculatePriceChange = () => {
    if (chartData.length < 2) return 0;
    const firstPrice = chartData[0].value;
    const lastPrice = chartData[chartData.length - 1].value;
    return ((lastPrice - firstPrice) / firstPrice) * 100;
  };

  if (isLoading) {
    return (
      <div className="bg-card rounded-lg p-6 border border-border">
        <div className="flex items-center gap-4 mb-6">
          <Skeleton className="w-12 h-12 rounded-full" />
          <div className="space-y-2">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-24" />
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      </div>
    );
  }

  if (!coinData) {
    return (
      <div className="bg-card rounded-lg p-6 border border-border">
        <div className="text-center space-y-2">
          <p className="text-destructive font-semibold">Unable to load coin data</p>
          <p className="text-xs text-muted-foreground">
            This is likely due to CoinGecko API rate limits. Please wait a moment and try selecting a different coin.
          </p>
        </div>
      </div>
    );
  }

  const priceChange24h = coinData.price_change_percentage_24h ?? 0;
  const isPositive24h = priceChange24h >= 0;
  
  const priceChangeSelected = calculatePriceChange();
  const isPositiveSelected = priceChangeSelected >= 0;

  return (
    <div className="bg-card rounded-lg p-6 border border-border animate-in fade-in duration-300">
      {/* Rate limit warning banner */}
      {isRateLimited && (
        <div className="mb-4 bg-warning/10 border border-warning rounded-lg p-3 text-sm">
          <p className="text-warning font-semibold">⚠️ Showing cached data</p>
          <p className="text-xs text-muted-foreground mt-1">
            CoinGecko API rate limit reached. Data will auto-refresh when available.
          </p>
        </div>
      )}

      {/* Header with coin info */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <img
            src={coinData.image}
            alt={coinData.name}
            className="w-12 h-12 rounded-full"
          />
          <div>
            <h3 className="text-2xl font-bold">{coinData.name}</h3>
            <p className="text-sm text-muted-foreground uppercase">{coinData.symbol}</p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-3xl font-bold">{formatPrice(coinData.current_price)}</p>
          <div className="flex items-center gap-2 justify-end mt-1">
            <div className={`flex items-center gap-1 ${isPositive24h ? 'text-success' : 'text-destructive'}`}>
              {isPositive24h ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
              <span className="text-sm font-semibold">
                {isPositive24h ? '+' : ''}{priceChange24h.toFixed(2)}%
              </span>
            </div>
            <span className="text-xs text-muted-foreground">24h</span>
          </div>
        </div>
      </div>

      {/* TIMEFRAME SELECTOR BUTTONS */}
      <div className="mb-4">
        <div className="flex flex-wrap gap-2 justify-center bg-background/50 rounded-lg p-3 border border-border">
          {TIMEFRAMES.map((tf) => (
            <Button
              key={tf.value}
              variant={selectedTimeframe === tf.value ? "default" : "ghost"}
              size="sm"
              onClick={() => setSelectedTimeframe(tf.value)}
              className={`min-w-[60px] transition-all ${
                selectedTimeframe === tf.value 
                  ? 'bg-primary text-primary-foreground shadow-md' 
                  : 'hover:bg-accent'
              }`}
              disabled={isLoadingChart}
            >
              {tf.label}
            </Button>
          ))}
        </div>
      </div>

      {/* Price chart with selected timeframe */}
      {chartData.length > 0 && (
        <div className="h-24 mb-6 relative">
          {isLoadingChart && (
            <div className="absolute inset-0 flex items-center justify-center bg-background/50 backdrop-blur-sm rounded-lg z-10">
              <div className="text-sm text-muted-foreground">Loading chart...</div>
            </div>
          )}
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <Tooltip
                contentStyle={{
                  backgroundColor: 'hsl(var(--popover))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '0.5rem',
                  fontSize: '12px',
                }}
                formatter={(value: number) => [formatPrice(value), '']}
                labelFormatter={(label) => {
                  const point = chartData[label];
                  if (point?.timestamp) {
                    return new Date(point.timestamp).toLocaleString();
                  }
                  return `${selectedTimeframe} trend`;
                }}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke={isPositiveSelected ? 'hsl(var(--success))' : 'hsl(var(--destructive))'}
                strokeWidth={2}
                dot={false}
                animationDuration={300}
              />
            </LineChart>
          </ResponsiveContainer>
          <div className="absolute top-0 right-0 bg-background/80 backdrop-blur-sm px-3 py-1.5 rounded text-xs font-semibold">
            <span className={isPositiveSelected ? 'text-success' : 'text-destructive'}>
              {isPositiveSelected ? '+' : ''}{priceChangeSelected.toFixed(2)}% ({selectedTimeframe})
            </span>
          </div>
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <BarChart3 className="w-4 h-4" />
            <p className="text-xs">Market Cap</p>
          </div>
          <p className="text-lg font-bold">{formatLargeNumber(coinData.market_cap)}</p>
          <p className="text-xs text-muted-foreground">Rank #{coinData.market_cap_rank}</p>
        </div>

        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <Activity className="w-4 h-4" />
            <p className="text-xs">24h Volume</p>
          </div>
          <p className="text-lg font-bold">{formatLargeNumber(coinData.total_volume)}</p>
        </div>

        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <Coins className="w-4 h-4" />
            <p className="text-xs">Circulating Supply</p>
          </div>
          <p className="text-lg font-bold">{formatSupply(coinData.circulating_supply)}</p>
          {coinData.total_supply && (
            <p className="text-xs text-muted-foreground">
              of {formatSupply(coinData.total_supply)}
            </p>
          )}
        </div>

        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <TrendingUp className="w-4 h-4" />
            <p className="text-xs">All-Time High</p>
          </div>
          <p className="text-lg font-bold">{formatPrice(coinData.ath)}</p>
          <p className="text-xs text-destructive">
            {(coinData.ath_change_percentage ?? 0).toFixed(2)}% from ATH
          </p>
        </div>

        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <TrendingDown className="w-4 h-4" />
            <p className="text-xs">All-Time Low</p>
          </div>
          <p className="text-lg font-bold">{formatPrice(coinData.atl)}</p>
          <p className="text-xs text-success">
            +{(coinData.atl_change_percentage ?? 0).toFixed(2)}% from ATL
          </p>
        </div>

        <div className="bg-background rounded-lg p-4 border border-border">
          <div className="flex items-center gap-2 text-muted-foreground mb-1">
            <DollarSign className="w-4 h-4" />
            <p className="text-xs">Price Change</p>
          </div>
          <p className={`text-lg font-bold ${(coinData.price_change_24h ?? 0) >= 0 ? 'text-success' : 'text-destructive'}`}>
            {(coinData.price_change_24h ?? 0) >= 0 ? '+' : ''}{formatPrice(coinData.price_change_24h ?? 0)}
          </p>
          <p className="text-xs text-muted-foreground">24h</p>
        </div>
      </div>

      {/* Data source attribution */}
      <div className="mt-4 pt-4 border-t border-border">
        <p className="text-xs text-muted-foreground text-center">
          Live data powered by{' '}
          <a
            href="https://www.coingecko.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary hover:underline"
          >
            CoinGecko
          </a>
          {' '}• Updates every 60 seconds
          {lastUpdated && (
            <span className="ml-2">• Last updated: {formatLastUpdated()}</span>
          )}
        </p>
      </div>
    </div>
  );
};