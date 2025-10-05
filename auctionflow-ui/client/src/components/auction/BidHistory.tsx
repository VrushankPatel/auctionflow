import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Skeleton } from '@/components/ui/skeleton';
import { CheckCircle, Clock, Trophy } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { formatDistanceToNow } from 'date-fns';
import type { Bid } from '@/types/entities';

interface BidHistoryProps {
  auctionId: string;
  currentUserId?: string;
}

export function BidHistory({ auctionId, currentUserId }: BidHistoryProps) {
  const { data, isLoading } = useQuery({
    queryKey: ['/auctions', auctionId, 'bids'],
    queryFn: () => api.getBidHistory(auctionId),
  });

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-start gap-3 pb-3">
            <Skeleton className="w-8 h-8 rounded-full" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-32" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  const bids: Bid[] = data?.bids || [];

  return (
    <div className="space-y-3 max-h-96 overflow-y-auto custom-scrollbar" data-testid="bid-history">
      {bids.map((bid, index) => {
        const isCurrentUser = bid.bidderId === currentUserId;
        const isWinning = index === 0;
        const isOutbid = !isWinning && bid.status === 'OUTBID';

        return (
          <div
            key={bid.id}
            className={`flex items-start gap-3 pb-3 border-b border-border last:border-0 ${
              isCurrentUser ? 'bg-primary/5 -mx-3 px-3 py-2 rounded-lg' : ''
            }`}
            data-testid={`bid-item-${bid.id}`}
          >
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 mt-1 ${
                isWinning
                  ? 'bg-success/20'
                  : isCurrentUser
                  ? 'bg-primary/20'
                  : 'bg-muted-foreground/20'
              }`}
            >
              {isWinning ? (
                <CheckCircle className="w-4 h-4 text-success" />
              ) : isCurrentUser ? (
                <span className="text-xs font-bold text-primary">You</span>
              ) : (
                <Clock className="w-4 h-4 text-muted-foreground" />
              )}
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-baseline justify-between gap-2 mb-1">
                <span className="font-semibold text-lg" data-testid="text-bid-amount">
                  ${bid.amount.toFixed(2)}
                </span>
                <span className="text-xs text-muted-foreground whitespace-nowrap" data-testid="text-bid-time">
                  {formatDistanceToNow(new Date(bid.timestamp), { addSuffix: true })}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground" data-testid="text-bidder-name">
                  {isCurrentUser ? 'Your Bid' : bid.bidderName || `bidder_${bid.bidderId.substring(0, 4)}`}
                </span>
                {isWinning && (
                  <Badge variant="default" className="text-xs bg-success" data-testid="badge-winning">
                    <Trophy className="w-3 h-3 mr-1" />
                    Winning
                  </Badge>
                )}
                {isOutbid && isCurrentUser && (
                  <span className="text-xs text-destructive" data-testid="text-outbid">
                    (Outbid)
                  </span>
                )}
              </div>
            </div>
          </div>
        );
      })}

      {bids.length === 0 && (
        <p className="text-center text-muted-foreground py-8" data-testid="text-no-bids">
          No bids yet. Be the first to bid!
        </p>
      )}
    </div>
  );
}
