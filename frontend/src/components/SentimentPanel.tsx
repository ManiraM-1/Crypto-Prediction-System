import { CheckCircle, XCircle, Circle, TrendingUp, TrendingDown, Minus } from "lucide-react";

interface Headline {
  title: string;
  source?: string;
  timestamp: string;
  sentiment: string;
  relevanceScore?: number;
}

interface Statistics {
  totalArticles: number;
  positiveCount: number;
  negativeCount: number;
  neutralCount: number;
  sentimentTrend: string;
}

interface SentimentPanelProps {
  marketMoodScore: number;
  sentimentLabel: string;
  topHeadlines: Headline[];
  statistics: Statistics;
}

export const SentimentPanel = ({ marketMoodScore, sentimentLabel, topHeadlines, statistics }: SentimentPanelProps) => {
  const getSentimentColor = (score: number) => {
    if (score <= 1.0) return "#F44336"; // Red
    if (score <= 2.0) return "#FB8C00"; // Orange
    if (score <= 2.5) return "#FDD835"; // Yellow
    if (score <= 3.5) return "#66BB6A"; // Light green
    return "#4CAF50"; // Dark green
  };

  const getSentimentIcon = (sentiment: string) => {
    switch (sentiment.toLowerCase()) {
      case "positive":
        return <CheckCircle className="w-4 h-4 text-success" />;
      case "negative":
        return <XCircle className="w-4 h-4 text-destructive" />;
      default:
        return <Circle className="w-4 h-4 text-muted-foreground" />;
    }
  };

  const getTrendIcon = (trend: string) => {
    switch (trend.toLowerCase()) {
      case "improving":
        return <TrendingUp className="w-4 h-4 text-success" />;
      case "declining":
        return <TrendingDown className="w-4 h-4 text-destructive" />;
      default:
        return <Minus className="w-4 h-4 text-muted-foreground" />;
    }
  };

  const formatTimeAgo = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now.getTime() - date.getTime();
      const hours = Math.floor(diff / (1000 * 60 * 60));
      if (hours < 1) return "Just now";
      if (hours === 1) return "1 hour ago";
      if (hours < 24) return `${hours} hours ago`;
      return `${Math.floor(hours / 24)} days ago`;
    } catch {
      return "";
    }
  };

  // Calculate gauge rotation (180 degrees total, from -90 to 90)
  const gaugeRotation = ((marketMoodScore / 4) * 180) - 90;
  const color = getSentimentColor(marketMoodScore);

  return (
    <div className="bg-card rounded-lg p-6 border border-border h-full flex flex-col">
      <h3 className="text-xl font-semibold mb-4">Live Market Sentiment</h3>
      
      {/* Sentiment Gauge */}
      <div className="flex flex-col items-center mb-6">
        <div className="relative w-48 h-24">
          {/* Semi-circle background */}
          <svg viewBox="0 0 200 100" className="w-full h-full">
            <defs>
              <linearGradient id="gaugeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" style={{ stopColor: "#F44336", stopOpacity: 1 }} />
                <stop offset="25%" style={{ stopColor: "#FB8C00", stopOpacity: 1 }} />
                <stop offset="50%" style={{ stopColor: "#FDD835", stopOpacity: 1 }} />
                <stop offset="75%" style={{ stopColor: "#66BB6A", stopOpacity: 1 }} />
                <stop offset="100%" style={{ stopColor: "#4CAF50", stopOpacity: 1 }} />
              </linearGradient>
            </defs>
            <path
              d="M 20 90 A 80 80 0 0 1 180 90"
              fill="none"
              stroke="url(#gaugeGradient)"
              strokeWidth="12"
              strokeLinecap="round"
            />
            {/* Needle */}
            <line
              x1="100"
              y1="90"
              x2="100"
              y2="30"
              stroke={color}
              strokeWidth="3"
              strokeLinecap="round"
              style={{
                transformOrigin: "100px 90px",
                transform: `rotate(${gaugeRotation}deg)`,
                transition: "transform 0.5s ease-out",
              }}
            />
            <circle cx="100" cy="90" r="6" fill={color} />
          </svg>
        </div>
        <div className="text-center mt-2">
          <p className="text-4xl font-bold" style={{ color }}>
            {marketMoodScore.toFixed(1)}
          </p>
          <p className="text-lg font-semibold" style={{ color }}>
            {sentimentLabel}
          </p>
        </div>
      </div>

      {/* Recent News Headlines */}
      <div className="flex-1 mb-4">
        <h4 className="text-sm font-semibold mb-3 text-muted-foreground">Recent News</h4>
        <div className="space-y-2 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
          {topHeadlines.map((headline, index) => (
            <div key={index} className="bg-background rounded-lg p-3 border border-border hover:border-primary/50 transition-colors">
              <div className="flex items-start gap-2">
                {getSentimentIcon(headline.sentiment)}
                <div className="flex-1 min-w-0">
                  <p className="text-sm line-clamp-2">{headline.title}</p>
                  <div className="flex gap-2 mt-1 text-xs text-muted-foreground">
                    {headline.source && <span>{headline.source}</span>}
                    <span>•</span>
                    <span>{formatTimeAgo(headline.timestamp)}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Statistics */}
      <div className="bg-background rounded-lg p-4 border border-border">
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-muted-foreground">Total Articles</p>
            <p className="font-bold text-lg">{statistics.totalArticles}</p>
          </div>
          <div className="flex items-center gap-2">
            <p className="text-muted-foreground">Trend</p>
            <div className="flex items-center gap-1">
              {getTrendIcon(statistics.sentimentTrend)}
              <span className="font-semibold capitalize">{statistics.sentimentTrend}</span>
            </div>
          </div>
          <div>
            <p className="text-muted-foreground">Positive</p>
            <p className="font-bold text-success">{statistics.positiveCount}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Negative</p>
            <p className="font-bold text-destructive">{statistics.negativeCount}</p>
          </div>
        </div>
      </div>

      <style>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: hsl(var(--background));
          border-radius: 3px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: hsl(var(--border));
          border-radius: 3px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: hsl(var(--muted-foreground));
        }
      `}</style>
    </div>
  );
};
