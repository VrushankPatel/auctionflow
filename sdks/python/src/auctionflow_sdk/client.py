import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from typing import Optional, List
from .models import (
    AuthRequest, AuthResponse, RegisterRequest, RegisterResponse,
    CreateAuctionRequest, AuctionSummary, AuctionDetails,
    PlaceBidRequest, BidHistory
)

class AuctionFlowClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.access_token: Optional[str] = None
        self.refresh_token: Optional[str] = None

        # Setup retry strategy
        retry_strategy = Retry(
            total=3,
            status_forcelist=[429, 500, 502, 503, 504],
            backoff_factor=1
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        self.session.mount("http://", adapter)
        self.session.mount("https://", adapter)

    def _get_headers(self) -> dict:
        headers = {'Content-Type': 'application/json'}
        if self.access_token:
            headers['Authorization'] = f'Bearer {self.access_token}'
        return headers

    def login(self, request: AuthRequest) -> AuthResponse:
        url = f"{self.base_url}/api/v1/auth/login"
        response = self.session.post(url, json=request.dict(), headers={'Content-Type': 'application/json'})
        response.raise_for_status()
        data = response.json()
        self.access_token = data['accessToken']
        self.refresh_token = data['refreshToken']
        return AuthResponse(**data)

    def register(self, request: RegisterRequest) -> RegisterResponse:
        url = f"{self.base_url}/api/v1/auth/register"
        response = self.session.post(url, json=request.dict(), headers={'Content-Type': 'application/json'})
        response.raise_for_status()
        return RegisterResponse(**response.json())

    def create_auction(self, request: CreateAuctionRequest) -> None:
        self._ensure_authenticated()
        url = f"{self.base_url}/api/v1/auctions"
        response = self.session.post(url, json=request.dict(), headers=self._get_headers())
        response.raise_for_status()

    def list_auctions(self, category: Optional[str] = None, seller_id: Optional[str] = None,
                     page: int = 0, size: int = 10) -> List[AuctionSummary]:
        self._ensure_authenticated()
        url = f"{self.base_url}/api/v1/auctions"
        params = {'category': category, 'sellerId': seller_id, 'page': page, 'size': size}
        params = {k: v for k, v in params.items() if v is not None}
        response = self.session.get(url, params=params, headers=self._get_headers())
        response.raise_for_status()
        return [AuctionSummary(**item) for item in response.json()]

    def get_auction(self, auction_id: str) -> AuctionDetails:
        self._ensure_authenticated()
        url = f"{self.base_url}/api/v1/auctions/{auction_id}"
        response = self.session.get(url, headers=self._get_headers())
        response.raise_for_status()
        return AuctionDetails(**response.json())

    def place_bid(self, auction_id: str, request: PlaceBidRequest) -> None:
        self._ensure_authenticated()
        url = f"{self.base_url}/api/v1/auctions/{auction_id}/bids"
        response = self.session.post(url, json=request.dict(), headers=self._get_headers())
        response.raise_for_status()

    def get_bid_history(self, auction_id: str, page: int = 0, size: int = 10) -> BidHistory:
        self._ensure_authenticated()
        url = f"{self.base_url}/api/v1/auctions/{auction_id}/bids"
        params = {'page': page, 'size': size}
        response = self.session.get(url, params=params, headers=self._get_headers())
        response.raise_for_status()
        return BidHistory(**response.json())

    def _ensure_authenticated(self):
        if not self.access_token:
            raise ValueError("Not authenticated. Please login first.")
        # TODO: check token expiry and refresh