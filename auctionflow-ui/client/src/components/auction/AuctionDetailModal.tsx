import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useWebSocket } from '@/hooks/use-websocket';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/auth';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { CountdownTimer } from './CountdownTimer';
import { BidForm } from './BidForm';
import { BidHistory } from './BidHistory';
import { Heart, X, ShieldCheck, Truck, RotateCcw } from 'lucide-react';
import type { Auction } from '@/types/entities';

export function AuctionDetailModal() {
  const { selectedAuctionId, setSelectedAuctionId } = useUIStore();
  const { user } = useAuthStore();
  const [selectedImage, setSelectedImage] = useState(0);

  const { data: auction, isLoading } = useQuery({
    queryKey: ['/auctions', selectedAuctionId],
    queryFn: () => api.getAuction(selectedAuctionId!),
    enabled: !!selectedAuctionId,
  });

  const { onMessage } = useWebSocket(selectedAuctionId || undefined);

  useEffect(() => {
    if (!selectedAuctionId) return;

    const unsubscribe = onMessage((message) => {
      if (message.type === 'bid_placed' && message.auctionId === selectedAuctionId) {
        // Optimistically update the UI
        console.log('New bid received:', message.data);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [selectedAuctionId, onMessage]);

  if (!selectedAuctionId) return null;

  const auctionData: Auction = auction;
  const minBid = (auctionData?.currentHighestBid || auctionData?.startingPrice || 0) + 10;

  return (
    <Dialog open={!!selectedAuctionId} onOpenChange={() => setSelectedAuctionId(null)}>
      <DialogContent className="max-w-7xl max-h-[90vh] overflow-hidden p-0" data-testid="modal-auction-detail">
        <div className="flex flex-col h-full">
          <DialogHeader className="p-6 border-b border-border">
            <div className="flex items-center justify-between">
              <DialogTitle className="text-2xl font-bold">Auction Details</DialogTitle>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setSelectedAuctionId(null)}
                data-testid="button-close-modal"
              >
                <X className="w-6 h-6" />
              </Button>
            </div>
          </DialogHeader>

          <div className="flex-1 overflow-auto custom-scrollbar">
            {isLoading ? (
              <div className="grid lg:grid-cols-2 gap-8 p-6">
                <Skeleton className="aspect-square" />
                <div className="space-y-4">
                  <Skeleton className="h-8 w-3/4" />
                  <Skeleton className="h-24 w-full" />
                </div>
              </div>
            ) : (
              <div className="grid lg:grid-cols-2 gap-8 p-6">
                {/* Left Column - Images */}
                <div className="space-y-4">
                  <div className="aspect-square rounded-lg overflow-hidden bg-muted">
                    <img
                      src={
                        auctionData?.images[selectedImage] ||
                        'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=1200&h=1200&fit=crop'
                      }
                      alt={auctionData?.title}
                      className="w-full h-full object-cover"
                      data-testid="img-auction-main"
                    />
                  </div>

                  <div className="grid grid-cols-4 gap-3">
                    {auctionData?.images.map((image, index) => (
                      <button
                        key={index}
                        onClick={() => setSelectedImage(index)}
                        className={`aspect-square rounded-lg overflow-hidden border-2 ${
                          selectedImage === index ? 'border-primary' : 'border-transparent'
                        }`}
                        data-testid={`button-thumbnail-${index}`}
                      >
                        <img src={image} alt={`View ${index + 1}`} className="w-full h-full object-cover" />
                      </button>
                    ))}
                  </div>

                  {/* Item Details Card */}
                  <div className="bg-muted rounded-lg p-6">
                    <h3 className="font-semibold mb-4">Item Details</h3>
                    <dl className="space-y-3 text-sm">
                      <div className="flex justify-between">
                        <dt className="text-muted-foreground">Condition</dt>
                        <dd className="font-medium" data-testid="text-condition">{auctionData?.condition || 'Excellent'}</dd>
                      </div>
                      <div className="flex justify-between">
                        <dt className="text-muted-foreground">Category</dt>
                        <dd className="font-medium" data-testid="text-category">{auctionData?.category}</dd>
                      </div>
                      <div className="flex justify-between">
                        <dt className="text-muted-foreground">Auction Type</dt>
                        <dd className="font-medium" data-testid="text-auction-type">{auctionData?.auctionType}</dd>
                      </div>
                    </dl>
                  </div>
                </div>

                {/* Right Column - Bidding */}
                <div className="space-y-6">
                  <div>
                    <div className="flex items-start justify-between mb-2">
                      <h2 className="text-2xl font-bold flex-1" data-testid="text-auction-title">
                        {auctionData?.title}
                      </h2>
                      <Button variant="ghost" size="icon" data-testid="button-add-watchlist">
                        <Heart className={auctionData?.isWatched ? 'fill-destructive text-destructive' : ''} />
                      </Button>
                    </div>
                    <div className="flex items-center gap-3">
                      <Badge data-testid="badge-category">{auctionData?.category}</Badge>
                      <span className="text-sm text-muted-foreground">
                        Auction ID: #{auctionData?.id.substring(0, 8)}
                      </span>
                    </div>
                  </div>

                  {/* Timer */}
                  {auctionData?.endTime && (
                    <div className="bg-destructive/10 border-2 border-destructive rounded-lg p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-sm text-muted-foreground mb-1">Auction Ends In</div>
                          <CountdownTimer
                            endTime={auctionData.endTime}
                            className="text-3xl font-bold text-destructive"
                          />
                        </div>
                        <Badge className="bg-success text-success-foreground" data-testid="badge-live">
                          <span className="w-2 h-2 bg-success-foreground rounded-full animate-pulse mr-1" />
                          Live
                        </Badge>
                      </div>
                    </div>
                  )}

                  {/* Current Bid */}
                  <div className="bg-muted rounded-lg p-6">
                    <div className="space-y-4">
                      <div>
                        <div className="text-sm text-muted-foreground mb-1">Current Highest Bid</div>
                        <div className="text-4xl font-bold" data-testid="text-highest-bid">
                          ${auctionData?.currentHighestBid?.toFixed(2) || auctionData?.startingPrice.toFixed(2)}
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <div className="text-muted-foreground">Total Bids</div>
                          <div className="text-lg font-semibold" data-testid="text-total-bids">{auctionData?.bidCount}</div>
                        </div>
                        <div>
                          <div className="text-muted-foreground">Bid Increment</div>
                          <div className="text-lg font-semibold text-accent">+$10</div>
                        </div>
                      </div>
                      {auctionData?.reservePrice && (
                        <div className="pt-3 border-t border-border">
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-muted-foreground">Reserve Price</span>
                            <span
                              className={
                                (auctionData.currentHighestBid || 0) >= auctionData.reservePrice
                                  ? 'text-success font-semibold'
                                  : 'text-warning'
                              }
                              data-testid="text-reserve-status"
                            >
                              {(auctionData.currentHighestBid || 0) >= auctionData.reservePrice ? '✓ Met' : 'Not Met'}
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Bid Form */}
                  <div className="bg-card border-2 border-primary rounded-lg p-6">
                    <h3 className="font-semibold mb-4">Place Your Bid</h3>
                    <BidForm
                      auctionId={auctionData?.id || ''}
                      currentBid={auctionData?.currentHighestBid || auctionData?.startingPrice || 0}
                      minBid={minBid}
                      bidIncrement={10}
                    />
                  </div>

                  {/* Bid History */}
                  <div className="bg-muted rounded-lg p-6">
                    <h3 className="font-semibold mb-4">Bid History</h3>
                    <BidHistory auctionId={auctionData?.id || ''} currentUserId={user?.id} />
                  </div>

                  {/* Trust Indicators */}
                  <div className="space-y-3 text-sm">
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <ShieldCheck className="w-4 h-4 text-success" />
                      <span>Buyer Protection Guarantee</span>
                    </div>
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <Truck className="w-4 h-4 text-success" />
                      <span>Free Insured Shipping</span>
                    </div>
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <RotateCcw className="w-4 h-4 text-success" />
                      <span>7-Day Return Policy</span>
                    </div>
                  </div>

                  {/* Description */}
                  <div className="bg-muted rounded-lg p-6">
                    <h3 className="font-semibold mb-3">Description</h3>
                    <div className="text-sm text-muted-foreground space-y-3" data-testid="text-description">
                      <p>{auctionData?.description}</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
