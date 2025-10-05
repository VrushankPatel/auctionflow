import { Link } from 'wouter';
import { Heart, TrendingUp, Clock } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { CountdownTimer } from './CountdownTimer';
import { useUIStore } from '@/stores/ui';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useToast } from '@/hooks/use-toast';
import type { Auction } from '@/types/entities';

interface AuctionCardProps {
  auction: Auction;
}

export function AuctionCard({ auction }: AuctionCardProps) {
  const { setSelectedAuctionId } = useUIStore();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const watchlistMutation = useMutation({
    mutationFn: (isWatched: boolean) =>
      isWatched ? api.removeFromWatchlist(auction.id) : api.addToWatchlist(auction.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['/auctions'] });
      toast({
        title: auction.isWatched ? 'Removed from watchlist' : 'Added to watchlist',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error.message || 'Failed to update watchlist',
        variant: 'destructive',
      });
    },
  });

  const handleCardClick = () => {
    setSelectedAuctionId(auction.id);
  };

  const handleWatchlistClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    watchlistMutation.mutate(auction.isWatched || false);
  };

  const timeRemaining = new Date(auction.endTime).getTime() - new Date().getTime();
  const isEndingSoon = timeRemaining < 60 * 60 * 1000; // Less than 1 hour

  return (
    <Card
      className="group cursor-pointer hover:shadow-xl transition-all duration-300 hover:-translate-y-1"
      onClick={handleCardClick}
      data-testid={`card-auction-${auction.id}`}
    >
      <div className="relative aspect-square overflow-hidden bg-muted">
        <img
          src={auction.images[0] || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&h=600&fit=crop'}
          alt={auction.title}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
          data-testid="img-auction"
        />

        {/* Status Badge */}
        {isEndingSoon && (
          <Badge
            className="absolute top-3 left-3 bg-destructive text-destructive-foreground timer-urgent"
            data-testid="badge-ending-soon"
          >
            <Clock className="w-3 h-3 mr-1" />
            Ending Soon
          </Badge>
        )}

        {auction.status === 'ACTIVE' && !isEndingSoon && (
          <Badge
            className="absolute top-3 left-3 bg-success text-success-foreground"
            data-testid="badge-live"
          >
            <span className="w-2 h-2 bg-success-foreground rounded-full animate-pulse mr-1" />
            Live
          </Badge>
        )}

        {/* Watchlist Button */}
        <Button
          variant="ghost"
          size="icon"
          className="absolute top-3 right-3 w-8 h-8 bg-card/90 backdrop-blur-sm hover:scale-110"
          onClick={handleWatchlistClick}
          data-testid="button-watchlist"
        >
          <Heart
            className={auction.isWatched ? 'fill-destructive text-destructive' : 'text-muted-foreground'}
          />
        </Button>
      </div>

      <CardContent className="p-4">
        <h3 className="font-semibold text-base line-clamp-2 mb-2" data-testid="text-auction-title">
          {auction.title}
        </h3>

        <div className="flex items-center gap-2 mb-3">
          <Badge variant="outline" className="text-xs" data-testid="text-category">
            {auction.category}
          </Badge>
          <span className="text-xs text-muted-foreground" data-testid="text-bid-count">
            {auction.bidCount} bids
          </span>
        </div>

        <div className="space-y-2 mb-4">
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-muted-foreground">Current Bid</span>
            <span className="text-2xl font-bold" data-testid="text-current-bid">
              ${auction.currentHighestBid?.toFixed(2) || auction.startingPrice.toFixed(2)}
            </span>
          </div>
          <CountdownTimer endTime={auction.endTime} showIcon={false} />
        </div>

        <Button className="w-full" data-testid="button-place-bid">
          <TrendingUp className="w-4 h-4 mr-2" />
          Place Bid
        </Button>
      </CardContent>
    </Card>
  );
}
