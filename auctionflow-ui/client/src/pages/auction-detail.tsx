import { useEffect, useState } from 'react';
import { useParams } from 'wouter';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useWebSocket } from '@/hooks/use-websocket';
import { useAuthStore } from '@/stores/auth';
import { Header } from '@/components/layout/Header';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent } from '@/components/ui/card';
import { CountdownTimer } from '@/components/auction/CountdownTimer';
import { BidForm } from '@/components/auction/BidForm';
import { BidHistory } from '@/components/auction/BidHistory';
import { Heart, ShieldCheck, Truck, RotateCcw, ChevronLeft } from 'lucide-react';
import { Link } from 'wouter';
import type { Auction } from '@/types/entities';

export default function AuctionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuthStore();
  const [selectedImage, setSelectedImage] = useState(0);

  const { data: auction, isLoading } = useQuery({
    queryKey: ['/auctions', id],
    queryFn: () => api.getAuction(id!),
    enabled: !!id,
  });

  const { onMessage } = useWebSocket(id || undefined);

  useEffect(() => {
    if (!id) return;

    const unsubscribe = onMessage((message) => {
      if (message.type === 'bid_placed' && message.auctionId === id) {
        console.log('New bid received:', message.data);
      }
    });

    return () => {
      unsubscribe();
    };
  }, [id, onMessage]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background" data-testid="page-auction-detail">
        <Header />
        <div className="container mx-auto px-4 py-8">
          <div className="grid lg:grid-cols-2 gap-8">
            <Skeleton className="aspect-square" />
            <div className="space-y-4">
              <Skeleton className="h-8 w-3/4" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!auction) {
    return (
      <div className="min-h-screen bg-background" data-testid="page-auction-detail">
        <Header />
        <div className="container mx-auto px-4 py-8 text-center">
          <h1 className="text-2xl font-bold mb-4">Auction Not Found</h1>
          <p className="text-muted-foreground mb-6">The auction you're looking for doesn't exist or has been removed.</p>
          <Link href="/">
            <Button>Back to Auctions</Button>
          </Link>
        </div>
      </div>
    );
  }

  const auctionData: Auction = auction;
  const minBid = (auctionData?.currentHighestBid || auctionData?.startingPrice || 0) + 10;

  return (
    <div className="min-h-screen bg-background" data-testid="page-auction-detail">
      <Header />

      <div className="container mx-auto px-4 py-8">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-sm text-muted-foreground mb-6" data-testid="breadcrumb">
          <Link href="/">
            <a className="hover:text-foreground transition-colors">Auctions</a>
          </Link>
          <span>/</span>
          <span className="text-foreground">{auctionData?.category}</span>
        </nav>

        <div className="grid lg:grid-cols-2 gap-8">
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
              {(auctionData?.images || [
                'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=300&h=300&fit=crop',
                'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=300&h=300&fit=crop',
                'https://pixabay.com/get/gc9038780b08315aab3248b854b7c5564b4b5c54d8f38702e3a1188736b82385134858052f2517ece94ae9a2ff605c0691ecf643395c3a6b5ce6646198e8de0d8_1280.jpg',
                'https://images.unsplash.com/photo-1606925797300-0b35e9d1794e?w=300&h=300&fit=crop',
              ]).map((image, index) => (
                <button
                  key={index}
                  onClick={() => setSelectedImage(index)}
                  className={`aspect-square rounded-lg overflow-hidden border-2 transition-colors ${
                    selectedImage === index ? 'border-primary' : 'border-transparent hover:border-border'
                  }`}
                  data-testid={`button-thumbnail-${index}`}
                >
                  <img src={image} alt={`View ${index + 1}`} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>

            {/* Item Details Card */}
            <Card>
              <CardContent className="p-6">
                <h3 className="font-semibold mb-4">Item Details</h3>
                <dl className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">Condition</dt>
                    <dd className="font-medium" data-testid="text-condition">
                      {auctionData?.condition || 'Excellent'}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">Category</dt>
                    <dd className="font-medium" data-testid="text-category">
                      {auctionData?.category}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">Auction Type</dt>
                    <dd className="font-medium" data-testid="text-auction-type">
                      {auctionData?.auctionType}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">Item Location</dt>
                    <dd className="font-medium">New York, USA</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted-foreground">Shipping</dt>
                    <dd className="font-medium">$25 (Worldwide)</dd>
                  </div>
                </dl>
              </CardContent>
            </Card>

            {/* Seller Info */}
            <Card>
              <CardContent className="p-6">
                <h3 className="font-semibold mb-4">Seller Information</h3>
                <div className="flex items-center gap-4 mb-4">
                  <div className="w-12 h-12 bg-gradient-to-br from-primary to-accent rounded-full flex items-center justify-center text-lg font-bold text-primary-foreground">
                    {auctionData?.seller?.displayName?.substring(0, 2).toUpperCase() || 'PE'}
                  </div>
                  <div>
                    <div className="font-semibold">{auctionData?.seller?.displayName || 'PhotoExpert'}</div>
                    <div className="text-sm text-muted-foreground">Member since 2019</div>
                  </div>
                </div>
                <div className="flex items-center gap-6 text-sm">
                  <div>
                    <div className="text-2xl font-bold text-success">98.5%</div>
                    <div className="text-muted-foreground">Positive</div>
                  </div>
                  <div className="h-10 w-px bg-border"></div>
                  <div>
                    <div className="text-2xl font-bold">1,247</div>
                    <div className="text-muted-foreground">Reviews</div>
                  </div>
                  <div className="h-10 w-px bg-border"></div>
                  <div>
                    <div className="text-2xl font-bold">842</div>
                    <div className="text-muted-foreground">Sold Items</div>
                  </div>
                </div>
                <Button className="w-full mt-4" variant="outline" data-testid="button-contact-seller">
                  Contact Seller
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Right Column - Bidding */}
          <div className="space-y-6">
            <div>
              <div className="flex items-start justify-between mb-2">
                <h1 className="text-2xl md:text-3xl font-bold flex-1" data-testid="text-auction-title">
                  {auctionData?.title}
                </h1>
                <Button variant="ghost" size="icon" data-testid="button-add-watchlist">
                  <Heart className={auctionData?.isWatched ? 'fill-destructive text-destructive' : ''} />
                </Button>
              </div>
              <div className="flex items-center gap-3">
                <Badge data-testid="badge-category">{auctionData?.category}</Badge>
                <span className="text-sm text-muted-foreground">Auction ID: #{auctionData?.id.substring(0, 8)}</span>
              </div>
            </div>

            {/* Timer */}
            {auctionData?.endTime && (
              <Card className="bg-destructive/10 border-destructive">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-sm text-muted-foreground mb-1">Auction Ends In</div>
                      <CountdownTimer endTime={auctionData.endTime} className="text-3xl font-bold text-destructive" />
                    </div>
                    <Badge className="bg-success text-success-foreground" data-testid="badge-live">
                      <span className="w-2 h-2 bg-success-foreground rounded-full animate-pulse mr-1" />
                      Live
                    </Badge>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Current Bid */}
            <Card className="bg-muted">
              <CardContent className="p-6">
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
                      <div className="text-lg font-semibold" data-testid="text-total-bids">
                        {auctionData?.bidCount}
                      </div>
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
              </CardContent>
            </Card>

            {/* Bid Form */}
            <Card className="border-2 border-primary">
              <CardContent className="p-6">
                <h3 className="font-semibold mb-4">Place Your Bid</h3>
                <BidForm
                  auctionId={auctionData?.id || ''}
                  currentBid={auctionData?.currentHighestBid || auctionData?.startingPrice || 0}
                  minBid={minBid}
                  bidIncrement={10}
                />
              </CardContent>
            </Card>

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

            {/* Bid History */}
            <Card className="bg-muted">
              <CardContent className="p-6">
                <h3 className="font-semibold mb-4">Bid History</h3>
                <BidHistory auctionId={auctionData?.id || ''} currentUserId={user?.id} />
              </CardContent>
            </Card>

            {/* Description */}
            <Card className="bg-muted">
              <CardContent className="p-6">
                <h3 className="font-semibold mb-3">Description</h3>
                <div className="text-sm text-muted-foreground space-y-3" data-testid="text-description">
                  <p>{auctionData?.description}</p>
                </div>
              </CardContent>
            </Card>

            {/* Watching Counter */}
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Heart className="w-4 h-4 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">243 people watching</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-warning">Hot item</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
