import { TrendingUp, TrendingDown, Minus } from "lucide-react";

interface DecisionPanelProps {
  signal: "BUY" | "SELL" | "HOLD";
  confidence: string;
  summary: string;
  timestamp: string;
  symbol: string;
  minutes: number;
}

export const DecisionPanel = ({ signal, confidence, summary, timestamp, symbol, minutes }: DecisionPanelProps) => {
  const getSignalStyles = () => {
    switch (signal) {
      case "BUY":
        return {
          bg: "bg-success",
          icon: TrendingUp,
          text: "BUY",
        };
      case "SELL":
        return {
          bg: "bg-destructive",
          icon: TrendingDown,
          text: "SELL",
        };
      case "HOLD":
        return {
          bg: "bg-warning",
          icon: Minus,
          text: "HOLD",
        };
    }
  };

  const styles = getSignalStyles();
  const Icon = styles.icon;
  
  const formatTimestamp = (ts: string) => {
    try {
      return new Date(ts).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return ts;
    }
  };

  const formatTimeHorizon = (mins: number) => {
    if (mins < 60) return `${mins} minutes`;
    if (mins === 60) return "1 hour";
    const hours = mins / 60;
    return hours === Math.floor(hours) ? `${hours} hours` : `${mins} minutes`;
  };

  return (
    <div className="bg-card rounded-lg p-8 border border-border animate-in fade-in duration-500">
      <div className="flex flex-col items-center text-center space-y-6">
        <div className={`${styles.bg} rounded-2xl p-8 w-full max-w-md relative`}>
          <div className="flex items-center justify-center gap-4 mb-2">
            <Icon className="w-12 h-12 text-white" strokeWidth={3} />
            <h2 className="text-6xl font-bold text-white">{styles.text}</h2>
          </div>
          <div className="absolute top-4 right-4">
            <span className="bg-white/20 px-3 py-1 rounded-full text-xs font-semibold text-white">
              {confidence} CONFIDENCE
            </span>
          </div>
        </div>

        <p className="text-muted-foreground text-lg leading-relaxed max-w-2xl">
          {summary}
        </p>

        <div className="flex flex-wrap justify-center gap-4 text-sm text-muted-foreground">
          <span>📊 {symbol}</span>
          <span>•</span>
          <span>⏱️ {formatTimeHorizon(minutes)} forecast</span>
          <span>•</span>
          <span>📅 {formatTimestamp(timestamp)}</span>
        </div>
      </div>
    </div>
  );
};
