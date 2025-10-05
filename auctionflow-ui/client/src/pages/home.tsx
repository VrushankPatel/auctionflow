import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Header } from '@/components/layout/Header';
import { AuctionCard } from '@/components/auction/AuctionCard';
import { AuctionFilters } from '@/components/auction/AuctionFilters';
import { AuctionDetailModal } from '@/components/auction/AuctionDetailModal';
import { AuthModal } from '@/components/auth/AuthModal';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { TrendingUp, DollarSign, Users, Package } from 'lucide-react';
import type { Auction } from '@/types/entities';

export default function HomePage() {
  const [filters, setFilters] = useState({
    category: '',
    status: 'ACTIVE',
    priceRange: '',
    sortBy: 'ending',
  });

  const { data, isLoading } = useQuery({
    queryKey: ['/auctions', filters],
    queryFn: () => api.getAuctions(filters),
  });

  const handleFilterChange = (key: string, value: string) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleClearFilters = () => {
    setFilters({
      category: '',
      status: 'ACTIVE',
      priceRange: '',
      sortBy: 'ending',
    });
  };

  const auctions: Auction[] = data?.auctions || [];

  return (
    <div className="min-h-screen bg-background" data-testid="page-home">
      <Header />
      <AuthModal />
      <AuctionDetailModal />

      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary/5 via-background to-accent/5 border-b border-border">
        <div className="container mx-auto px-4 py-12">
          <div className="text-center mb-8">
            <h1 className="text-4xl md:text-5xl font-bold mb-4">Live Auctions</h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Discover exclusive items and place real-time bids on thousands of active auctions
            </p>
          </div>

          {/* Quick Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-4xl mx-auto">
            <div className="bg-card rounded-lg p-4 border border-border text-center">
              <div className="flex items-center justify-center mb-2">
                <Package className="w-5 h-5 text-primary mr-2" />
                <div className="text-2xl font-bold text-primary">12,453</div>
              </div>
              <div className="text-sm text-muted-foreground">Active Auctions</div>
            </div>
            <div className="bg-card rounded-lg p-4 border border-border text-center">
              <div className="flex items-center justify-center mb-2">
                <TrendingUp className="w-5 h-5 text-success mr-2" />
                <div className="text-2xl font-bold text-success">89%</div>
              </div>
              <div className="text-sm text-muted-foreground">Success Rate</div>
            </div>
            <div className="bg-card rounded-lg p-4 border border-border text-center">
              <div className="flex items-center justify-center mb-2">
                <DollarSign className="w-5 h-5 text-accent mr-2" />
                <div className="text-2xl font-bold text-accent">$2.4M</div>
              </div>
              <div className="text-sm text-muted-foreground">Today's Volume</div>
            </div>
            <div className="bg-card rounded-lg p-4 border border-border text-center">
              <div className="flex items-center justify-center mb-2">
                <Users className="w-5 h-5 text-foreground mr-2" />
                <div className="text-2xl font-bold text-foreground">156K</div>
              </div>
              <div className="text-sm text-muted-foreground">Active Bidders</div>
            </div>
          </div>
        </div>
      </section>

      {/* Auctions */}
      <section className="py-8">
        <div className="container mx-auto px-4">
          <AuctionFilters
            filters={filters}
            onFilterChange={handleFilterChange}
            onClearFilters={handleClearFilters}
          />

          {/* Auction Grid */}
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <div key={i} className="space-y-3">
                  <Skeleton className="aspect-square" />
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
              ))}
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6" data-testid="auction-grid">
                {auctions.map((auction) => (
                  <AuctionCard key={auction.id} auction={auction} />
                ))}
              </div>

              {auctions.length === 0 && (
                <div className="text-center py-12 text-muted-foreground" data-testid="text-no-auctions">
                  <p className="text-lg mb-2">No auctions found</p>
                  <p className="text-sm">Try adjusting your filters</p>
                </div>
              )}
            </>
          )}

          {/* Load More */}
          {auctions.length > 0 && (
            <div className="mt-8 flex justify-center">
              <Button variant="outline" data-testid="button-load-more">
                Load More Auctions
              </Button>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
