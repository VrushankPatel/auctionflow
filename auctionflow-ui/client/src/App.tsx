import { Switch, Route, Router as WouterRouter } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/not-found";
import HomePage from "@/pages/home";
import DashboardPage from "@/pages/dashboard";
import CreateAuctionPage from "@/pages/create-auction";
import AuctionDetailPage from "@/pages/auction-detail";

// Custom hook to handle base path
const useHashLocation = (): [string, (to: string) => void] => {
  const location = window.location.pathname;
  const base = "/ui";
  
  // Remove base from location for routing
  const path = location.startsWith(base) 
    ? location.slice(base.length) || "/" 
    : location;
  
  const navigate = (to: string) => {
    window.history.pushState({}, "", base + to);
    window.dispatchEvent(new PopStateEvent("popstate"));
  };
  
  return [path, navigate];
};

function Router() {
  return (
    <WouterRouter hook={useHashLocation}>
      <Switch>
        <Route path="/" component={HomePage} />
        <Route path="/dashboard" component={DashboardPage} />
        <Route path="/create" component={CreateAuctionPage} />
        <Route path="/auction/:id" component={AuctionDetailPage} />
        <Route component={NotFound} />
      </Switch>
    </WouterRouter>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Router />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
