import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent } from '@/components/ui/card';
import { Camera, Upload } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useToast } from '@/hooks/use-toast';
import { useLocation } from 'wouter';

const createAuctionSchema = z.object({
  title: z.string().min(10, 'Title must be at least 10 characters'),
  description: z.string().min(50, 'Description must be at least 50 characters'),
  category: z.string().min(1, 'Please select a category'),
  condition: z.string().optional(),
  startingPrice: z.string().min(1, 'Starting price is required'),
  reservePrice: z.string().optional(),
  buyNowPrice: z.string().optional(),
  auctionType: z.string().default('ENGLISH'),
  startTime: z.string().min(1, 'Start time is required'),
  endTime: z.string().min(1, 'End time is required'),
});

type CreateAuctionForm = z.infer<typeof createAuctionSchema>;

export function CreateAuctionForm() {
  const [, setLocation] = useLocation();
  const { toast } = useToast();

  const form = useForm<CreateAuctionForm>({
    resolver: zodResolver(createAuctionSchema),
    defaultValues: {
      auctionType: 'ENGLISH',
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => api.createAuction(data),
    onSuccess: () => {
      toast({
        title: 'Auction created successfully!',
        description: 'Your auction is now live and accepting bids.',
      });
      setLocation('/dashboard');
    },
    onError: (error: any) => {
      toast({
        title: 'Failed to create auction',
        description: error.message || 'Please try again',
        variant: 'destructive',
      });
    },
  });

  const onSubmit = (data: CreateAuctionForm) => {
    const auctionData = {
      ...data,
      itemId: 'item-' + Date.now(),
      sellerId: 'seller-123',
      images: [],
      startingPrice: parseFloat(data.startingPrice),
      reservePrice: data.reservePrice ? parseFloat(data.reservePrice) : undefined,
      buyNowPrice: data.buyNowPrice ? parseFloat(data.buyNowPrice) : undefined,
    };

    createMutation.mutate(auctionData);
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8" data-testid="form-create-auction">
      {/* Images */}
      <Card>
        <CardContent className="p-6">
          <Label className="text-lg font-semibold mb-4 block">Item Images</Label>
          <div className="grid grid-cols-4 gap-4">
            <div className="col-span-2 aspect-square rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer bg-muted/30 flex items-center justify-center group">
              <div className="text-center">
                <Camera className="w-12 h-12 mx-auto text-muted-foreground group-hover:text-primary transition-colors mb-3" />
                <p className="text-sm font-medium text-muted-foreground group-hover:text-primary transition-colors">
                  Upload main image
                </p>
                <p className="text-xs text-muted-foreground mt-1">or drag and drop</p>
              </div>
            </div>
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="aspect-square rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer bg-muted/30 flex items-center justify-center"
              >
                <Upload className="w-8 h-8 text-muted-foreground" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Item Details */}
      <Card>
        <CardContent className="p-6 space-y-4">
          <h3 className="text-lg font-semibold">Item Details</h3>

          <div>
            <Label htmlFor="title">Auction Title *</Label>
            <Input
              id="title"
              placeholder="e.g., Vintage Leica M3 Camera - 1960s Collector's Edition"
              {...form.register('title')}
              data-testid="input-title"
            />
            {form.formState.errors.title && (
              <p className="text-sm text-destructive mt-1">{form.formState.errors.title.message}</p>
            )}
          </div>

          <div>
            <Label htmlFor="category">Category *</Label>
            <Select onValueChange={(value) => form.setValue('category', value)}>
              <SelectTrigger id="category" data-testid="select-category">
                <SelectValue placeholder="Select a category" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="electronics">Electronics</SelectItem>
                <SelectItem value="art">Art & Collectibles</SelectItem>
                <SelectItem value="jewelry">Jewelry & Watches</SelectItem>
                <SelectItem value="vehicles">Vehicles</SelectItem>
                <SelectItem value="furniture">Furniture & Home</SelectItem>
              </SelectContent>
            </Select>
            {form.formState.errors.category && (
              <p className="text-sm text-destructive mt-1">{form.formState.errors.category.message}</p>
            )}
          </div>

          <div>
            <Label htmlFor="description">Description *</Label>
            <Textarea
              id="description"
              rows={6}
              placeholder="Provide a detailed description..."
              {...form.register('description')}
              data-testid="textarea-description"
            />
            {form.formState.errors.description && (
              <p className="text-sm text-destructive mt-1">{form.formState.errors.description.message}</p>
            )}
          </div>

          <div>
            <Label htmlFor="condition">Condition</Label>
            <Select onValueChange={(value) => form.setValue('condition', value)}>
              <SelectTrigger id="condition" data-testid="select-condition">
                <SelectValue placeholder="Select condition" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="new">New</SelectItem>
                <SelectItem value="like-new">Like New</SelectItem>
                <SelectItem value="excellent">Excellent</SelectItem>
                <SelectItem value="good">Good</SelectItem>
                <SelectItem value="fair">Fair</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Pricing */}
      <Card>
        <CardContent className="p-6 space-y-4">
          <h3 className="text-lg font-semibold">Pricing & Settings</h3>

          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <Label htmlFor="starting-price">Starting Price *</Label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                <Input
                  id="starting-price"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  className="pl-8"
                  {...form.register('startingPrice')}
                  data-testid="input-starting-price"
                />
              </div>
              {form.formState.errors.startingPrice && (
                <p className="text-sm text-destructive mt-1">{form.formState.errors.startingPrice.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="reserve-price">Reserve Price (Optional)</Label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                <Input
                  id="reserve-price"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  className="pl-8"
                  {...form.register('reservePrice')}
                  data-testid="input-reserve-price"
                />
              </div>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <Label htmlFor="start-time">Start Date & Time *</Label>
              <Input
                id="start-time"
                type="datetime-local"
                {...form.register('startTime')}
                data-testid="input-start-time"
              />
              {form.formState.errors.startTime && (
                <p className="text-sm text-destructive mt-1">{form.formState.errors.startTime.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="end-time">End Date & Time *</Label>
              <Input
                id="end-time"
                type="datetime-local"
                {...form.register('endTime')}
                data-testid="input-end-time"
              />
              {form.formState.errors.endTime && (
                <p className="text-sm text-destructive mt-1">{form.formState.errors.endTime.message}</p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Actions */}
      <div className="flex items-center justify-between">
        <Button type="button" variant="outline" data-testid="button-save-draft">
          Save as Draft
        </Button>
        <div className="flex gap-3">
          <Button type="button" variant="outline" data-testid="button-preview">
            Preview
          </Button>
          <Button type="submit" disabled={createMutation.isPending} data-testid="button-create-auction">
            {createMutation.isPending ? 'Creating...' : 'Create Auction'}
          </Button>
        </div>
      </div>
    </form>
  );
}
