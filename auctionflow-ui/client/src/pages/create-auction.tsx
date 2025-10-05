import { Header } from '@/components/layout/Header';
import { CreateAuctionForm } from '@/components/create/CreateAuctionForm';

export default function CreateAuctionPage() {
  return (
    <div className="min-h-screen bg-background" data-testid="page-create-auction">
      <Header />

      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-2">Create New Auction</h1>
          <p className="text-muted-foreground">List your item and start receiving bids in minutes</p>
        </div>

        <CreateAuctionForm />
      </div>
    </div>
  );
}
