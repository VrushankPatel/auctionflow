import axios, { AxiosInstance } from 'axios';
import axiosRetry from 'axios-retry';
import {
  AuthRequest,
  AuthResponse,
  RegisterRequest,
  RegisterResponse,
  CreateAuctionRequest,
  AuctionSummary,
  AuctionDetails,
  PlaceBidRequest,
  BidHistory
} from './types';

export class AuctionFlowClient {
  private axiosInstance: AxiosInstance;
  private accessToken?: string;
  private refreshToken?: string;

  constructor(baseURL: string) {
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000,
    });

    // Add retry logic
    axiosRetry(this.axiosInstance, {
      retries: 3,
      retryDelay: axiosRetry.exponentialDelay,
      retryCondition: (error) => {
        return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.response?.status === 429;
      },
    });

    // Add auth interceptor
    this.axiosInstance.interceptors.request.use((config) => {
      if (this.accessToken) {
        config.headers.Authorization = `Bearer ${this.accessToken}`;
      }
      return config;
    });
  }

  async login(request: AuthRequest): Promise<AuthResponse> {
    const response = await this.axiosInstance.post<AuthResponse>('/api/v1/auth/login', request);
    this.accessToken = response.data.accessToken;
    this.refreshToken = response.data.refreshToken;
    return response.data;
  }

  async register(request: RegisterRequest): Promise<RegisterResponse> {
    const response = await this.axiosInstance.post<RegisterResponse>('/api/v1/auth/register', request);
    return response.data;
  }

  async createAuction(request: CreateAuctionRequest): Promise<void> {
    await this.axiosInstance.post('/api/v1/auctions', request);
  }

  async listAuctions(category?: string, sellerId?: string, page = 0, size = 10): Promise<AuctionSummary[]> {
    const params = { category, sellerId, page, size };
    const response = await this.axiosInstance.get<AuctionSummary[]>('/api/v1/auctions', { params });
    return response.data;
  }

  async getAuction(id: string): Promise<AuctionDetails> {
    const response = await this.axiosInstance.get<AuctionDetails>(`/api/v1/auctions/${id}`);
    return response.data;
  }

  async placeBid(auctionId: string, request: PlaceBidRequest): Promise<void> {
    await this.axiosInstance.post(`/api/v1/auctions/${auctionId}/bids`, request);
  }

  async getBidHistory(auctionId: string, page = 0, size = 10): Promise<BidHistory> {
    const params = { page, size };
    const response = await this.axiosInstance.get<BidHistory>(`/api/v1/auctions/${auctionId}/bids`, { params });
    return response.data;
  }

  // TODO: implement refresh token
}