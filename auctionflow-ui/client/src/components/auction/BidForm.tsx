import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useToast } from '@/hooks/use-toast';
import { TrendingUp } from 'lucide-react';
import { nanoid } from 'nanoid';

interface BidFormProps {
  auctionId: string;
  currentBid: number;
  minBid: number;
  bidIncrement: number;
}

export function BidForm({ auctionId, currentBid, minBid, bidIncrement }: BidFormProps) {
  const [bidAmount, setBidAmount] = useState<string>(minBid.toFixed(2));
  const [maxBid, setMaxBid] = useState<string>('');
  const [useMaxBid, setUseMaxBid] = useState(false);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const bidMutation = useMutation({
    mutationFn: (amount: number) =>
      api.placeBid(auctionId, {
        amount,
        idempotencyKey: nanoid(),
      }),
    onSuccess: (data) => {
      toast({
        title: 'Bid Placed Successfully',
        description: `Your bid of $${data.amount.toFixed(2)} has been accepted`,
      });
      queryClient.invalidateQueries({ queryKey: ['/auctions', auctionId] });
      queryClient.invalidateQueries({ queryKey: ['/auctions'] });
    },
    onError: (error: any) => {
      let message = 'Failed to place bid';
      if (error.error === 'BID_TOO_LOW') {
        message = error.message || `Bid must be at least $${minBid.toFixed(2)}`;
      } else if (error.error === 'RATE_LIMIT_EXCEEDED') {
        message = 'Too many requests. Please wait a moment.';
      }
      toast({
        title: 'Bid Failed',
        description: message,
        variant: 'destructive',
      });
    },
  });

  const handleQuickBid = (increment: number) => {
    const newAmount = parseFloat(bidAmount) + increment;
    setBidAmount(newAmount.toFixed(2));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(bidAmount);
    
    if (amount < minBid) {
      toast({
        title: 'Invalid Bid',
        description: `Minimum bid is $${minBid.toFixed(2)}`,
        variant: 'destructive',
      });
      return;
    }

    bidMutation.mutate(amount);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4" data-testid="form-place-bid">
      <div className="grid grid-cols-3 gap-2">
        <Button
          type="button"
          variant="outline"
          onClick={() => handleQuickBid(bidIncrement)}
          data-testid="button-quick-bid-1"
        >
          +${bidIncrement}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => handleQuickBid(bidIncrement * 2)}
          data-testid="button-quick-bid-2"
        >
          +${(bidIncrement * 2).toFixed(0)}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => handleQuickBid(bidIncrement * 5)}
          data-testid="button-quick-bid-3"
        >
          +${(bidIncrement * 5).toFixed(0)}
        </Button>
      </div>

      <div>
        <Label htmlFor="bid-amount">Your Bid</Label>
        <div className="relative mt-2">
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground font-semibold">
            $
          </span>
          <Input
            id="bid-amount"
            type="number"
            step="0.01"
            min={minBid}
            value={bidAmount}
            onChange={(e) => setBidAmount(e.target.value)}
            className="pl-8 text-lg font-semibold"
            data-testid="input-bid-amount"
          />
        </div>
        <p className="text-xs text-muted-foreground mt-2">
          Minimum bid: ${minBid.toFixed(2)} (current + ${bidIncrement} increment)
        </p>
      </div>

      <div className="p-3 bg-muted rounded-lg">
        <div className="flex items-start gap-3">
          <Checkbox
            id="max-bid"
            checked={useMaxBid}
            onCheckedChange={(checked) => setUseMaxBid(checked as boolean)}
            data-testid="checkbox-max-bid"
          />
          <div className="flex-1">
            <Label htmlFor="max-bid" className="font-medium text-sm cursor-pointer">
              Set Maximum Bid
            </Label>
            <p className="text-xs text-muted-foreground">
              Automatically bid up to your max amount
            </p>
            {useMaxBid && (
              <Input
                type="number"
                step="0.01"
                min={minBid}
                value={maxBid}
                onChange={(e) => setMaxBid(e.target.value)}
                className="mt-2"
                placeholder="Max bid amount"
                data-testid="input-max-bid"
              />
            )}
          </div>
        </div>
      </div>

      <Button
        type="submit"
        className="w-full py-4 text-lg"
        disabled={bidMutation.isPending}
        data-testid="button-submit-bid"
      >
        <TrendingUp className="w-5 h-5 mr-2" />
        {bidMutation.isPending ? 'Placing Bid...' : `Place Bid - $${bidAmount}`}
      </Button>

      <p className="text-xs text-center text-muted-foreground">
        By bidding, you agree to our{' '}
        <a href="#" className="text-primary hover:underline">
          Terms & Conditions
        </a>
      </p>
    </form>
  );
}
