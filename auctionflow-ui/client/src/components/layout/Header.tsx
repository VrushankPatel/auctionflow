import { Link } from 'wouter';
import { Search, Heart, Bell, Menu, Gavel } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useAuthStore } from '@/stores/auth';
import { useUIStore } from '@/stores/ui';

export function Header() {
  const { user, isAuthenticated, clearAuth } = useAuthStore();
  const { setAuthModalOpen, notificationCount } = useUIStore();

  const handleLogout = () => {
    clearAuth();
  };

  return (
    <header className="sticky top-0 z-40 bg-card border-b border-border" data-testid="header-navigation">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center gap-8">
            <Link href="/" className="flex items-center gap-2" data-testid="link-home">
              <div className="w-8 h-8 bg-gradient-to-br from-primary to-secondary rounded-lg flex items-center justify-center">
                <Gavel className="w-5 h-5 text-primary-foreground" />
              </div>
              <span className="text-xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
                AuctionFlow
              </span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-6">
              <Link href="/" className="text-sm font-medium text-foreground hover:text-primary transition-colors" data-testid="link-auctions">
                Auctions
              </Link>
              <Link href="/create" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors" data-testid="link-sell">
                Sell
              </Link>
              <Link href="/dashboard" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors" data-testid="link-dashboard">
                Dashboard
              </Link>
            </nav>
          </div>

          {/* Search Bar */}
          <div className="hidden lg:flex flex-1 max-w-md mx-8">
            <div className="relative w-full">
              <Input
                type="text"
                placeholder="Search auctions..."
                className="w-full pl-10"
                data-testid="input-search"
              />
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            </div>
          </div>

          {/* User Actions */}
          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <Link href="/create">
                  <Button className="hidden md:inline-flex" data-testid="button-create-auction">
                    <Gavel className="w-4 h-4 mr-2" />
                    Create Auction
                  </Button>
                </Link>

                <Button variant="ghost" size="icon" className="relative" data-testid="button-watchlist">
                  <Heart className="w-5 h-5" />
                  <Badge className="absolute -top-1 -right-1 w-5 h-5 flex items-center justify-center p-0" data-testid="badge-watchlist-count">
                    3
                  </Badge>
                </Button>

                <Button variant="ghost" size="icon" className="relative" data-testid="button-notifications">
                  <Bell className="w-5 h-5" />
                  {notificationCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-2 h-2 bg-destructive rounded-full" />
                  )}
                </Button>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="flex items-center gap-2" data-testid="button-user-menu">
                      <Avatar className="w-8 h-8">
                        <AvatarFallback className="bg-gradient-to-br from-primary to-accent text-primary-foreground">
                          {user?.displayName.substring(0, 2).toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <span className="text-sm font-medium hidden md:inline" data-testid="text-username">
                        {user?.displayName}
                      </span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <Link href="/dashboard">
                      <DropdownMenuItem data-testid="link-my-dashboard">My Dashboard</DropdownMenuItem>
                    </Link>
                    <DropdownMenuItem data-testid="button-settings">Settings</DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={handleLogout} data-testid="button-logout">
                      Logout
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </>
            ) : (
              <>
                <Button variant="ghost" onClick={() => setAuthModalOpen(true)} data-testid="button-login">
                  Login
                </Button>
                <Button onClick={() => { setAuthModalOpen(true); }} data-testid="button-signup">
                  Sign Up
                </Button>
              </>
            )}

            <Button variant="ghost" size="icon" className="md:hidden" data-testid="button-mobile-menu">
              <Menu className="w-5 h-5" />
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
}
