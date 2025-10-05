import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface AuctionFiltersProps {
  filters: {
    category?: string;
    status?: string;
    priceRange?: string;
    sortBy?: string;
  };
  onFilterChange: (key: string, value: string) => void;
  onClearFilters: () => void;
}

export function AuctionFilters({ filters, onFilterChange, onClearFilters }: AuctionFiltersProps) {
  const hasActiveFilters = Object.values(filters).some((value) => value);

  return (
    <>
      <Card className="mb-6">
        <CardContent className="p-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <Label className="text-sm font-medium mb-2" htmlFor="category-filter">
                Category
              </Label>
              <Select
                value={filters.category}
                onValueChange={(value) => onFilterChange('category', value)}
              >
                <SelectTrigger id="category-filter" data-testid="select-category">
                  <SelectValue placeholder="All Categories" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Categories</SelectItem>
                  <SelectItem value="electronics">Electronics</SelectItem>
                  <SelectItem value="art">Art & Collectibles</SelectItem>
                  <SelectItem value="jewelry">Jewelry</SelectItem>
                  <SelectItem value="vehicles">Vehicles</SelectItem>
                  <SelectItem value="furniture">Furniture</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-sm font-medium mb-2" htmlFor="status-filter">
                Status
              </Label>
              <Select
                value={filters.status}
                onValueChange={(value) => onFilterChange('status', value)}
              >
                <SelectTrigger id="status-filter" data-testid="select-status">
                  <SelectValue placeholder="Active Auctions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">Active Auctions</SelectItem>
                  <SelectItem value="ending-soon">Ending Soon</SelectItem>
                  <SelectItem value="new">New Listings</SelectItem>
                  <SelectItem value="no-reserve">No Reserve</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-sm font-medium mb-2" htmlFor="price-filter">
                Price Range
              </Label>
              <Select
                value={filters.priceRange}
                onValueChange={(value) => onFilterChange('priceRange', value)}
              >
                <SelectTrigger id="price-filter" data-testid="select-price-range">
                  <SelectValue placeholder="Any Price" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Any Price</SelectItem>
                  <SelectItem value="0-100">$0 - $100</SelectItem>
                  <SelectItem value="100-500">$100 - $500</SelectItem>
                  <SelectItem value="500-1000">$500 - $1,000</SelectItem>
                  <SelectItem value="1000+">$1,000+</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-sm font-medium mb-2" htmlFor="sort-filter">
                Sort By
              </Label>
              <Select
                value={filters.sortBy}
                onValueChange={(value) => onFilterChange('sortBy', value)}
              >
                <SelectTrigger id="sort-filter" data-testid="select-sort">
                  <SelectValue placeholder="Ending Soonest" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ending">Ending Soonest</SelectItem>
                  <SelectItem value="new">Newly Listed</SelectItem>
                  <SelectItem value="price-low">Price: Low to High</SelectItem>
                  <SelectItem value="price-high">Price: High to Low</SelectItem>
                  <SelectItem value="bids">Most Bids</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {hasActiveFilters && (
        <div className="flex items-center gap-2 mb-6 flex-wrap">
          <span className="text-sm text-muted-foreground">Active Filters:</span>
          {filters.category && filters.category !== 'all' && (
            <Badge variant="secondary" className="gap-1" data-testid="badge-filter-category">
              {filters.category}
              <Button
                variant="ghost"
                size="icon"
                className="h-4 w-4 p-0 hover:bg-transparent"
                onClick={() => onFilterChange('category', 'all')}
              >
                <X className="h-3 w-3" />
              </Button>
            </Badge>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={onClearFilters}
            className="text-destructive hover:text-destructive"
            data-testid="button-clear-filters"
          >
            Clear All
          </Button>
        </div>
      )}
    </>
  );
}
