import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2 } from "lucide-react";

interface ControlPanelProps {
  onAnalyze: (symbol: string, minutes: number) => void;
  onCryptoChange: (symbol: string) => void;
  isLoading: boolean;
}

const CRYPTO_OPTIONS = [
  { value: "BTC/USDT", label: "Bitcoin (BTC/USDT)" },
  { value: "ETH/USDT", label: "Ethereum (ETH/USDT)" },
  { value: "SOL/USDT", label: "Solana (SOL/USDT)" },
  { value: "XRP/USDT", label: "Ripple (XRP/USDT)" },
  { value: "ADA/USDT", label: "Cardano (ADA/USDT)" },
  { value: "DOGE/USDT", label: "Dogecoin (DOGE/USDT)" },
];

const TIME_OPTIONS = [
  { value: "60", label: "1 Hour" },
  { value: "120", label: "2 Hours" },
  { value: "240", label: "4 Hours" },
  { value: "360", label: "6 Hours" },
  { value: "720", label: "12 Hours" },
  { value: "1440", label: "24 Hours" },
  { value: "custom", label: "Custom..." },
];

export const ControlPanel = ({ onAnalyze, onCryptoChange, isLoading }: ControlPanelProps) => {
  const [selectedCrypto, setSelectedCrypto] = useState("BTC/USDT");
  const [selectedTime, setSelectedTime] = useState("240");
  const [customMinutes, setCustomMinutes] = useState("");
  const [showCustom, setShowCustom] = useState(false);
  const [error, setError] = useState("");

  // Notify parent of crypto selection changes immediately
  const handleCryptoChange = (value: string) => {
    setSelectedCrypto(value);
    onCryptoChange(value);
  };

  const handleTimeChange = (value: string) => {
    if (value === "custom") {
      setShowCustom(true);
      setSelectedTime("");
    } else {
      setShowCustom(false);
      setSelectedTime(value);
      setError("");
    }
  };

  const handleCustomSet = () => {
    const minutes = parseInt(customMinutes);
    if (isNaN(minutes) || minutes < 30 || minutes > 2880) {
      setError("Please enter between 30 and 2880 minutes");
      return;
    }
    setSelectedTime(customMinutes);
    setShowCustom(false);
    setError("");
  };

  const handleAnalyze = () => {
    const minutes = parseInt(selectedTime);
    if (isNaN(minutes)) {
      setError("Please select a time horizon");
      return;
    }
    onAnalyze(selectedCrypto, minutes);
  };

  return (
    <div className="bg-card rounded-lg p-6 border border-border">
      <div className="flex flex-col md:flex-row gap-4 items-end">
        <div className="flex-1 space-y-2">
          <Label htmlFor="crypto-select">Cryptocurrency</Label>
          <Select value={selectedCrypto} onValueChange={handleCryptoChange} disabled={isLoading}>
            <SelectTrigger id="crypto-select" className="bg-input">
              <SelectValue />
            </SelectTrigger>
            <SelectContent className="bg-popover border-border">
              {CRYPTO_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex-1 space-y-2">
          <Label htmlFor="time-select">Time Horizon</Label>
          {!showCustom ? (
            <Select value={selectedTime || "custom"} onValueChange={handleTimeChange} disabled={isLoading}>
              <SelectTrigger id="time-select" className="bg-input">
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="bg-popover border-border">
                {TIME_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          ) : (
            <div className="flex gap-2">
              <Input
                type="number"
                placeholder="Minutes"
                value={customMinutes}
                onChange={(e) => setCustomMinutes(e.target.value)}
                className="bg-input"
                min={30}
                max={2880}
              />
              <Button variant="success" onClick={handleCustomSet} size="sm">
                Set
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  setShowCustom(false);
                  setSelectedTime("240");
                  setError("");
                }}
                size="sm"
              >
                Cancel
              </Button>
            </div>
          )}
        </div>

        <Button
          onClick={handleAnalyze}
          disabled={isLoading || !selectedTime}
          variant="success"
          size="lg"
          className="md:min-w-[200px]"
        >
          {isLoading ? (
            <>
              <Loader2 className="animate-spin" />
              Analyzing...
            </>
          ) : (
            "Analyze Market"
          )}
        </Button>
      </div>
      {error && <p className="text-destructive text-sm mt-2">{error}</p>}
    </div>
  );
};
