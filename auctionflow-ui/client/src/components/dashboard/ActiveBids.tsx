import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { CountdownTimer } from '@/components/auction/CountdownTimer';
import type { BidHistoryItem } from '@/types/entities';

interface ActiveBidsProps {
  bids: BidHistoryItem[];
}

export function ActiveBids({ bids }: ActiveBidsProps) {
  return (
    <div className="space-y-4" data-testid="active-bids-list">
      {bids.map((bid) => (
        <Card
          key={bid.id}
          className={`p-4 ${
            bid.status === 'ACCEPTED'
              ? 'border-2 border-success/30 bg-success/5'
              : 'hover:border-primary/50'
          }`}
          data-testid={`bid-card-${bid.id}`}
        >
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-lg overflow-hidden bg-muted flex-shrink-0">
              <img
                src="https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=200&h=200&fit=crop"
                alt={bid.auctionTitle}
                className="w-full h-full object-cover"
                data-testid="img-bid-auction"
              />
            </div>

            <div className="flex-1 min-w-0">
              <h4 className="font-semibold mb-1 truncate" data-testid="text-bid-auction-title">
                {bid.auctionTitle || 'Auction Item'}
              </h4>
              <div className="flex items-center gap-4 text-sm">
                <Badge
                  variant={bid.status === 'ACCEPTED' ? 'default' : 'secondary'}
                  className={bid.status === 'ACCEPTED' ? 'bg-success' : ''}
                  data-testid="badge-bid-status"
                >
                  {bid.status === 'ACCEPTED' ? 'Winning' : bid.status === 'OUTBID' ? 'Outbid' : 'Active'}
                </Badge>
                <span className="text-muted-foreground">
                  Your bid: <span className="font-semibold text-foreground" data-testid="text-your-bid">${bid.amount.toFixed(2)}</span>
                </span>
                <CountdownTimer endTime={new Date(Date.now() + 1000 * 60 * 60 * 2).toISOString()} />
              </div>
            </div>

            <div className="text-right">
              <div className="text-sm text-muted-foreground mb-1">Current Highest</div>
              <div className="text-xl font-bold" data-testid="text-current-highest">${bid.amount.toFixed(2)}</div>
              <Button className="mt-2" data-testid="button-increase-bid">
                {bid.status === 'OUTBID' ? 'Rebid Now' : 'Increase Bid'}
              </Button>
            </div>
          </div>
        </Card>
      ))}

      {bids.length === 0 && (
        <div className="text-center py-12 text-muted-foreground" data-testid="text-no-active-bids">
          <p>No active bids yet</p>
          <p className="text-sm mt-2">Start bidding on auctions to see them here</p>
        </div>
      )}
    </div>
  );
}
