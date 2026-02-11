import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from "recharts";

interface HistoricalData {
  time: number;
  price: number;
  volume: number;
}

interface ForecastData {
  time: number;
  price: number;
  confidenceLower?: number;
  confidenceUpper?: number;
}

interface Metrics {
  predictedChange: number;
  predictedPrice: number;
  currentPrice: number;
  supportLevel?: number;
  resistanceLevel?: number;
}

interface PriceForecastPanelProps {
  historicalData: HistoricalData[];
  forecastData: ForecastData[];
  metrics: Metrics;
  confidence: string;
}

export const PriceForecastPanel = ({ historicalData, forecastData, metrics, confidence }: PriceForecastPanelProps) => {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(price);
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp * 1000);
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: true,
    });
  };

  // Combine historical and forecast data for the chart
  const chartData = [
    ...historicalData.map(d => ({ 
      time: d.time, 
      historical: d.price, 
      type: 'historical' 
    })),
    ...forecastData.map(d => ({ 
      time: d.time, 
      forecast: d.price, 
      type: 'forecast' 
    })),
  ];

  // Find the boundary between historical and forecast
  const boundaryTime = historicalData.length > 0 ? historicalData[historicalData.length - 1].time : 0;

  return (
    <div className="bg-card rounded-lg p-6 border border-border h-full">
      <h3 className="text-xl font-semibold mb-4">Price Forecast Model</h3>
      
      <div className="h-[300px] mb-6">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
            <XAxis 
              dataKey="time" 
              tickFormatter={formatTime}
              stroke="hsl(var(--muted-foreground))"
              style={{ fontSize: '12px' }}
            />
            <YAxis 
              tickFormatter={(value) => `$${value.toLocaleString()}`}
              stroke="hsl(var(--muted-foreground))"
              style={{ fontSize: '12px' }}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: 'hsl(var(--popover))',
                border: '1px solid hsl(var(--border))',
                borderRadius: '0.5rem',
              }}
              labelFormatter={formatTime}
              formatter={(value: number) => [formatPrice(value), '']}
            />
            <Legend />
            <ReferenceLine 
              x={boundaryTime} 
              stroke="hsl(var(--muted-foreground))" 
              strokeDasharray="5 5"
              label={{ value: 'Now', fill: 'hsl(var(--muted-foreground))' }}
            />
            <Line 
              type="monotone" 
              dataKey="historical" 
              stroke="hsl(var(--chart-1))" 
              strokeWidth={2}
              dot={false}
              name="Historical"
              connectNulls
            />
            <Line 
              type="monotone" 
              dataKey="forecast" 
              stroke="hsl(var(--chart-2))" 
              strokeWidth={2}
              strokeDasharray="5 5"
              dot={false}
              name="Forecast"
              connectNulls
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-background rounded-lg p-4 border border-border">
          <p className="text-xs text-muted-foreground mb-1">Current Price</p>
          <p className="text-lg font-bold">{formatPrice(metrics.currentPrice)}</p>
        </div>
        <div className="bg-background rounded-lg p-4 border border-border">
          <p className="text-xs text-muted-foreground mb-1">Predicted Price</p>
          <p className="text-lg font-bold">{formatPrice(metrics.predictedPrice)}</p>
        </div>
        <div className="bg-background rounded-lg p-4 border border-border">
          <p className="text-xs text-muted-foreground mb-1">Predicted Change</p>
          <p className={`text-lg font-bold ${metrics.predictedChange >= 0 ? 'text-success' : 'text-destructive'}`}>
            {metrics.predictedChange >= 0 ? '+' : ''}{metrics.predictedChange.toFixed(2)}%
          </p>
        </div>
        <div className="bg-background rounded-lg p-4 border border-border">
          <p className="text-xs text-muted-foreground mb-1">Confidence</p>
          <p className="text-lg font-bold">{confidence}</p>
        </div>
      </div>
    </div>
  );
};
