# Go SDK

## Install

```bash
go get github.com/auctionflow/auctionflow-go
```

## Usage

```go
import (
  "github.com/auctionflow/auctionflow-go"
)

client := auctionflow.NewClient("http://localhost:8080/api/v1", "<api-key>")
auctions, err := client.ListAuctions(nil)
```

See `go.mod` and `client.go` for details.
