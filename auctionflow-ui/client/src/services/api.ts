import { useAuthStore } from '@/stores/auth';
import type { LoginRequest, LoginResponse, RegisterRequest, PlaceBidRequest, PlaceBidResponse, AuctionListResponse } from '@/types/api';
import type { Auction, Bid } from '@/types/entities';
import {
  getMockAuctions,
  getMockAuction,
  getMockBidHistory,
  placeMockBid,
  mockLogin,
  mockRegister,
  mockCategories,
  mockBidIncrements,
  generateMockResponse,
} from './mockData';

const API_BASE_URL = '/api/v1';
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true';

function toNumber(value: any): number | undefined {
  if (value === null || value === undefined) return undefined;
  const n = typeof value === 'number' ? value : parseFloat(String(value));
  return Number.isFinite(n) ? n : undefined;
}

function mapAuctionTypeForUi(serverType?: string): Auction['auctionType'] {
  if (!serverType) return 'ENGLISH';
  switch (serverType) {
    case 'ENGLISH_OPEN':
      return 'ENGLISH';
    case 'DUTCH':
      return 'DUTCH';
    case 'SEALED_BID':
      return 'SEALED';
    default:
      return 'ENGLISH';
  }
}

function normalizeStatus(serverStatus?: string): Auction['status'] {
  if (!serverStatus) return 'ACTIVE';
  switch (serverStatus) {
    case 'OPEN':
      return 'ACTIVE';
    case 'ENDED':
      return 'CLOSED';
    default:
      return serverStatus as Auction['status'];
  }
}

function mapAuctionSummary(server: any): Auction {
  return {
    id: server.id ?? server.auctionId ?? server.uuid ?? String(server.id ?? ''),
    itemId: server.itemId ?? '',
    sellerId: (server.sellerId ?? '')?.toString?.() ?? '',
    title: server.title ?? '',
    description: server.description ?? '',
    category: server.category ?? server.categoryId ?? 'misc',
    condition: server.condition ?? undefined,
    images: Array.isArray(server.images) ? server.images : [],
    auctionType: mapAuctionTypeForUi(server.auctionType),
    startingPrice: toNumber(server.startingPrice) ?? 0,
    reservePrice: toNumber(server.reservePrice),
    buyNowPrice: toNumber(server.buyNowPrice),
    currentHighestBid: toNumber(server.currentHighestBid),
    bidCount: server.bidCount ?? 0,
    hiddenReserve: Boolean(server.hiddenReserve),
    startTime: (server.startTime ?? server.startTs ?? server.start)?.toString?.() ?? new Date().toISOString(),
    endTime: (server.endTime ?? server.endTs ?? server.end)?.toString?.() ?? new Date(Date.now() + 3600000).toISOString(),
    status: normalizeStatus(server.status),
    seller: server.seller
      ? { id: String(server.seller.id ?? ''), displayName: server.seller.displayName ?? '' }
      : undefined,
    isWatched: server.isWatched ?? false,
  };
}

function mapAuctionDetails(server: any): Auction {
  // Server details dto uses auctionId/startTs/endTs fields
  return mapAuctionSummary({
    id: server.auctionId ?? server.id,
    itemId: server.itemId,
    sellerId: server.sellerId,
    title: server.title,
    description: server.description,
    status: server.status,
    startTs: server.startTs,
    endTs: server.endTs,
    reservePrice: server.reservePrice,
    buyNowPrice: server.buyNowPrice,
    currentHighestBid: server.currentHighestBid,
    highestBidderId: server.highestBidderId,
    lastBidTs: server.lastBidTs,
  });
}

async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const token = useAuthStore.getState().token;
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers,
      signal: AbortSignal.timeout(2000),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'UNKNOWN_ERROR', message: response.statusText }));
      throw error;
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    // For non-JSON (e.g., empty body 200 OK)
    return null;
  } catch (error: any) {
    if (USE_MOCKS && (error.name === 'TypeError' || error.name === 'TimeoutError')) {
      console.warn('Backend unavailable, using mock data');
      throw { useMock: true };
    }
    throw error;
  }
}

export const api = {
  // Auth
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    try {
      return await fetchWithAuth('/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) return mockLogin(data.email, data.password);
      throw error;
    }
  },

  register: async (data: RegisterRequest) => {
    try {
      return await fetchWithAuth('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    } catch (error: any) {
      if (error.useMock) return mockRegister(data);
      throw error;
    }
  },

  // Auctions
  getAuctions: async (params?: { category?: string; status?: string; limit?: number; offset?: number }): Promise<AuctionListResponse> => {
    try {
      const queryParams = new URLSearchParams();
      if (params?.category) queryParams.append('category', params.category);
      if (params?.status) queryParams.append('status', params.status);
      // Backend uses page/size; map limit/offset if present
      if (params?.limit) queryParams.append('size', params.limit.toString());
      if (params?.offset) queryParams.append('page', Math.floor((params.offset ?? 0) / (params.limit ?? 10)).toString());
      
      const query = queryParams.toString();
      const data = await fetchWithAuth(`/auctions${query ? `?${query}` : ''}`);
      // Support both documented and implemented shapes
      if (!data) return { auctions: [], total: 0, hasMore: false };
      const auctionsRaw = Array.isArray(data.auctions) ? data.auctions : [];
      const auctions: Auction[] = auctionsRaw.map(mapAuctionSummary);
      const total = (data.totalElements ?? data.total ?? auctions.length) as number;
      const page = (data.page ?? 0) as number;
      const totalPages = (data.totalPages ?? (total && params?.limit ? Math.ceil(total / params.limit) : 1)) as number;
      return { auctions, total, hasMore: page + 1 < totalPages };
    } catch (error: any) {
      if (error.useMock) return getMockAuctions(params);
      throw error;
    }
  },

  getAuction: async (id: string) => {
    try {
      const data = await fetchWithAuth(`/auctions/${id}`);
      return data ? mapAuctionDetails(data) : undefined;
    } catch (error: any) {
      if (error.useMock) return getMockAuction(id);
      throw error;
    }
  },

  createAuction: async (data: any) => {
    try {
      // If the caller passed full form data (no itemId), create the item first
      let itemId = data.itemId as string | undefined;
      if (!itemId && data?.title && data?.description) {
        const itemPayload = {
          title: data.title,
          description: data.description,
          categoryId: data.category || data.categoryId,
          brand: data.brand || 'Generic',
          serialNumber: data.serialNumber || 'N/A',
          images: data.images || [],
          metadata: data.metadata || null,
        };
        const createdItem = await fetchWithAuth('/items', {
          method: 'POST',
          body: JSON.stringify(itemPayload),
        });
        itemId = createdItem?.id ?? createdItem?.itemId;
      }

      const auctionPayload = {
        itemId: itemId ?? data.itemId,
        categoryId: data.category || data.categoryId,
        auctionType: ((): string => {
          const t = (data.auctionType || '').toString().toUpperCase();
          if (t === 'ENGLISH' || t === 'ENGLISH_OPEN') return 'ENGLISH_OPEN';
          if (t === 'DUTCH') return 'DUTCH';
          if (t === 'SEALED' || t === 'SEALED_BID') return 'SEALED_BID';
          return 'ENGLISH_OPEN';
        })(),
        reservePrice: data.reservePrice ?? undefined,
        buyNowPrice: data.buyNowPrice ?? undefined,
        hiddenReserve: Boolean(data.hiddenReserve),
        startTime: new Date(data.startTime).toISOString(),
        endTime: new Date(data.endTime).toISOString(),
      };

      return await fetchWithAuth('/auctions', {
        method: 'POST',
        body: JSON.stringify(auctionPayload),
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({
          id: 'auction-' + Date.now(),
          ...data,
          status: 'PENDING',
          currentHighestBid: null,
          bidCount: 0,
          createdAt: new Date().toISOString(),
        });
      }
      throw error;
    }
  },

  // Bidding
  placeBid: async (auctionId: string, data: PlaceBidRequest): Promise<PlaceBidResponse> => {
    try {
      const serverResp = await fetchWithAuth(`/auctions/${auctionId}/bids`, {
        method: 'POST',
        body: JSON.stringify(data),
      });
      // Map server response (BidResponse) to client PlaceBidResponse
      const mapped: PlaceBidResponse = {
        bidId: String(serverResp?.sequenceNumber ?? Date.now()),
        auctionId,
        amount: data.amount,
        status: serverResp?.accepted ? 'ACCEPTED' : 'REJECTED',
        timestamp: (serverResp?.serverTimestamp ?? new Date().toISOString()).toString(),
      };
      return mapped;
    } catch (error: any) {
      if (error.useMock) return placeMockBid(auctionId, data.amount, data.idempotencyKey);
      throw error;
    }
  },

  getBidHistory: async (auctionId: string) => {
    try {
      const data = await fetchWithAuth(`/auctions/${auctionId}/bids`);
      const bids: Bid[] = Array.isArray(data?.bids)
        ? data.bids.map((b: any, index: number) => ({
            id: `${auctionId}-${b.seqNo ?? index}`,
            auctionId,
            bidderId: String(b.bidderId ?? ''),
            amount: toNumber(b.amount) ?? 0,
            status: b.accepted ? (index === 0 ? 'ACCEPTED' : 'OUTBID') : 'REJECTED',
            timestamp: (b.serverTs ?? b.timestamp ?? new Date().toISOString()).toString(),
            bidderName: undefined,
          }))
        : [];
      return { bids };
    } catch (error: any) {
      if (error.useMock) return getMockBidHistory(auctionId);
      throw error;
    }
  },

  // User
  getUserBids: async (userId: string) => {
    try {
      return await fetchWithAuth(`/users/${userId}/bids`);
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({
          bids: [
            {
              id: 'bid-1',
              auctionId: 'auction-1',
              auctionTitle: 'Vintage Leica M3 Camera',
              amount: 650,
              status: 'ACCEPTED',
              timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
              auctionStatus: 'ACTIVE',
            },
          ],
          total: 1,
        });
      }
      throw error;
    }
  },

  // Watchlist
  addToWatchlist: async (auctionId: string) => {
    try {
      const user = useAuthStore.getState().user;
      return await fetchWithAuth(`/auctions/${auctionId}/watch`, {
        method: 'POST',
        body: JSON.stringify({ userId: user?.id }),
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ message: 'Auction added to watchlist' });
      }
      throw error;
    }
  },

  removeFromWatchlist: async (auctionId: string) => {
    try {
      const user = useAuthStore.getState().user;
      return await fetchWithAuth(`/auctions/${auctionId}/watch`, {
        method: 'DELETE',
        body: JSON.stringify({ userId: user?.id }),
      });
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ message: 'Auction removed from watchlist' });
      }
      throw error;
    }
  },

  // Reference data
  getCategories: async () => {
    try {
      const data = await fetchWithAuth('/reference/categories');
      // Backend returns an array; normalize to { categories }
      if (Array.isArray(data)) return { categories: data };
      return data;
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ categories: mockCategories });
      }
      throw error;
    }
  },

  getBidIncrements: async () => {
    try {
      const data = await fetchWithAuth('/reference/bid-increments');
      if (Array.isArray(data)) return { increments: data };
      return data;
    } catch (error: any) {
      if (error.useMock) {
        return generateMockResponse({ increments: mockBidIncrements });
      }
      throw error;
    }
  },

  // Buy Now
  buyNow: async (auctionId: string) => {
    const user = useAuthStore.getState().user;
    return fetchWithAuth(`/auctions/${auctionId}/buy-now`, {
      method: 'POST',
      body: JSON.stringify({ userId: user?.id }),
    });
  },
};
