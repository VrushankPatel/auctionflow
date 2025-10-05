import { Heart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { CountdownTimer } from '@/components/auction/CountdownTimer';
import type { Auction } from '@/types/entities';

interface WatchlistProps {
  items: Auction[];
  onRemove: (auctionId: string) => void;
}

export function Watchlist({ items, onRemove }: WatchlistProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4" data-testid="watchlist-grid">
      {items.map((item) => (
        <div
          key={item.id}
          className="border border-border rounded-lg p-4 hover:shadow-md transition-all"
          data-testid={`watchlist-item-${item.id}`}
        >
          <div className="flex items-start gap-3">
            <div className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0 bg-muted">
              <img
                src={item.images[0] || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=200&h=200&fit=crop'}
                alt={item.title}
                className="w-full h-full object-cover"
                data-testid="img-watchlist"
              />
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-sm line-clamp-2 hover:text-primary cursor-pointer" data-testid="text-watchlist-title">
                  {item.title}
                </h3>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-destructive hover:text-destructive/80 flex-shrink-0 ml-2 h-8 w-8"
                  onClick={() => onRemove(item.id)}
                  data-testid="button-remove-watchlist"
                >
                  <Heart className="fill-destructive" />
                </Button>
              </div>

              <div className="mt-2 flex items-center justify-between text-sm">
                <div>
                  <p className="text-muted-foreground text-xs">Current Bid</p>
                  <p className="font-mono font-semibold" data-testid="text-watchlist-bid">
                    ${item.currentHighestBid?.toFixed(2) || item.startingPrice.toFixed(2)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-muted-foreground text-xs">Time Left</p>
                  <CountdownTimer endTime={item.endTime} showIcon={false} />
                </div>
              </div>
            </div>
          </div>
        </div>
      ))}

      {items.length === 0 && (
        <div className="col-span-2 text-center py-12 text-muted-foreground" data-testid="text-no-watchlist">
          <Heart className="w-12 h-12 mx-auto mb-4 opacity-50" />
          <p>Your watchlist is empty</p>
          <p className="text-sm mt-2">Add auctions to keep track of items you're interested in</p>
        </div>
      )}
    </div>
  );
}
