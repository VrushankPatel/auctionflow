import { Card, CardContent } from '@/components/ui/card';
import { TrendingUp, Heart, Trophy, DollarSign } from 'lucide-react';

interface DashboardStatsProps {
  activeBids: number;
  watchlistCount: number;
  wonAuctions: number;
  totalSpent: number;
}

export function DashboardStats({ activeBids, watchlistCount, wonAuctions, totalSpent }: DashboardStatsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="dashboard-stats">
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-muted-foreground">Active Bids</h3>
            <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
              <TrendingUp className="w-5 h-5 text-primary" />
            </div>
          </div>
          <div className="text-3xl font-bold mb-1" data-testid="stat-active-bids">{activeBids}</div>
          <p className="text-sm text-success">3 winning</p>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-muted-foreground">Watchlist</h3>
            <div className="w-10 h-10 rounded-lg bg-destructive/10 flex items-center justify-center">
              <Heart className="w-5 h-5 text-destructive" />
            </div>
          </div>
          <div className="text-3xl font-bold mb-1" data-testid="stat-watchlist">{watchlistCount}</div>
          <p className="text-sm text-accent">2 ending soon</p>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-muted-foreground">Won Auctions</h3>
            <div className="w-10 h-10 rounded-lg bg-success/10 flex items-center justify-center">
              <Trophy className="w-5 h-5 text-success" />
            </div>
          </div>
          <div className="text-3xl font-bold mb-1" data-testid="stat-won-auctions">{wonAuctions}</div>
          <p className="text-sm text-muted-foreground">All time</p>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-muted-foreground">Total Spent</h3>
            <div className="w-10 h-10 rounded-lg bg-accent/10 flex items-center justify-center">
              <DollarSign className="w-5 h-5 text-accent" />
            </div>
          </div>
          <div className="text-3xl font-bold mb-1" data-testid="stat-total-spent">${(totalSpent / 1000).toFixed(1)}K</div>
          <p className="text-sm text-muted-foreground">This year</p>
        </CardContent>
      </Card>
    </div>
  );
}
