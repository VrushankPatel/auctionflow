package auctionflow

import (
	"strconv"
	"time"

	"github.com/auctionflow/sdk/models"
	"github.com/go-resty/resty/v2"
)

type Client struct {
	restyClient  *resty.Client
	accessToken  string
	refreshToken string
}

func NewClient(baseURL string) *Client {
	client := resty.New().
		SetBaseURL(baseURL).
		SetRetryCount(3).
		SetRetryWaitTime(1 * time.Second).
		SetRetryMaxWaitTime(30 * time.Second)

	return &Client{
		restyClient: client,
	}
}

func (c *Client) setAuthHeader() {
	if c.accessToken != "" {
		c.restyClient.SetAuthToken(c.accessToken)
	}
}

func (c *Client) Login(req models.AuthRequest) (*models.AuthResponse, error) {
	var resp models.AuthResponse
	_, err := c.restyClient.R().
		SetBody(req).
		SetResult(&resp).
		Post("/api/v1/auth/login")
	if err != nil {
		return nil, err
	}
	c.accessToken = resp.AccessToken
	c.refreshToken = resp.RefreshToken
	c.setAuthHeader()
	return &resp, nil
}

func (c *Client) Register(req models.RegisterRequest) (*models.RegisterResponse, error) {
	var resp models.RegisterResponse
	_, err := c.restyClient.R().
		SetBody(req).
		SetResult(&resp).
		Post("/api/v1/auth/register")
	return &resp, err
}

func (c *Client) CreateAuction(req models.CreateAuctionRequest) error {
	c.setAuthHeader()
	_, err := c.restyClient.R().
		SetBody(req).
		Post("/api/v1/auctions")
	return err
}

func (c *Client) ListAuctions(category, sellerID string, page, size int) ([]models.AuctionSummary, error) {
	c.setAuthHeader()
	var resp []models.AuctionSummary
	_, err := c.restyClient.R().
		SetQueryParams(map[string]string{
			"category": category,
			"sellerId": sellerID,
			"page":     strconv.Itoa(page),
			"size":     strconv.Itoa(size),
		}).
		SetResult(&resp).
		Get("/api/v1/auctions")
	return resp, err
}

func (c *Client) GetAuction(id string) (*models.AuctionDetails, error) {
	c.setAuthHeader()
	var resp models.AuctionDetails
	_, err := c.restyClient.R().
		SetResult(&resp).
		Get("/api/v1/auctions/" + id)
	return &resp, err
}

func (c *Client) PlaceBid(auctionID string, req models.PlaceBidRequest) error {
	c.setAuthHeader()
	_, err := c.restyClient.R().
		SetBody(req).
		Post("/api/v1/auctions/" + auctionID + "/bids")
	return err
}

func (c *Client) GetBidHistory(auctionID string, page, size int) (*models.BidHistory, error) {
	c.setAuthHeader()
	var resp models.BidHistory
	_, err := c.restyClient.R().
		SetQueryParams(map[string]string{
			"page": strconv.Itoa(page),
			"size": strconv.Itoa(size),
		}).
		SetResult(&resp).
		Get("/api/v1/auctions/" + auctionID + "/bids")
	return &resp, err
}
