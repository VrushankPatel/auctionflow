import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/stores/auth';
import { Header } from '@/components/layout/Header';
import { DashboardStats } from '@/components/dashboard/DashboardStats';
import { ActiveBids } from '@/components/dashboard/ActiveBids';
import { Watchlist } from '@/components/dashboard/Watchlist';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function DashboardPage() {
  const { user } = useAuthStore();

  const { data: bidsData } = useQuery({
    queryKey: ['/users', user?.id, 'bids'],
    queryFn: () => api.getUserBids(user?.id || ''),
    enabled: !!user?.id,
  });

  const bids = bidsData?.bids || [];
  const activeBids = bids.filter((b: any) => b.auctionStatus === 'ACTIVE');

  return (
    <div className="min-h-screen bg-background" data-testid="page-dashboard">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-2">My Dashboard</h1>
          <p className="text-muted-foreground">Manage your bids, watchlist, and auction activity</p>
        </div>

        <DashboardStats
          activeBids={activeBids.length}
          watchlistCount={12}
          wonAuctions={24}
          totalSpent={18200}
        />

        <Card className="mt-8">
          <Tabs defaultValue="active-bids">
            <CardHeader className="border-b border-border pb-0">
              <TabsList className="w-full justify-start border-b-0">
                <TabsTrigger value="active-bids" data-testid="tab-active-bids">Active Bids</TabsTrigger>
                <TabsTrigger value="watchlist" data-testid="tab-watchlist">Watchlist</TabsTrigger>
                <TabsTrigger value="won" data-testid="tab-won">Won Items</TabsTrigger>
                <TabsTrigger value="history" data-testid="tab-history">Bid History</TabsTrigger>
              </TabsList>
            </CardHeader>

            <CardContent className="p-6">
              <TabsContent value="active-bids">
                <ActiveBids bids={activeBids} />
              </TabsContent>

              <TabsContent value="watchlist">
                <Watchlist items={[]} onRemove={(id) => console.log('Remove', id)} />
              </TabsContent>

              <TabsContent value="won">
                <div className="text-center py-12 text-muted-foreground" data-testid="text-no-won-items">
                  <p>No won items yet</p>
                </div>
              </TabsContent>

              <TabsContent value="history">
                <div className="text-center py-12 text-muted-foreground" data-testid="text-no-history">
                  <p>No bid history</p>
                </div>
              </TabsContent>
            </CardContent>
          </Tabs>
        </Card>
      </div>
    </div>
  );
}
