import type { Express } from "express";
import { createServer, type Server } from "http";
import { WebSocketServer, WebSocket } from "ws";

interface BidMessage {
  type: string;
  auctionId: string;
  bidderId: string;
  amount: number;
  timestamp: string;
}

export async function registerRoutes(app: Express): Promise<Server> {
  const httpServer = createServer(app);
  
  // WebSocket server on /ws path
  const wss = new WebSocketServer({ server: httpServer, path: '/ws' });
  
  const clients = new Map<string, Set<WebSocket>>();
  
  wss.on('connection', (ws: WebSocket) => {
    console.log('WebSocket client connected');
    
    ws.on('message', (message: string) => {
      try {
        const data = JSON.parse(message.toString());
        
        if (data.type === 'subscribe' && data.auctionId) {
          if (!clients.has(data.auctionId)) {
            clients.set(data.auctionId, new Set());
          }
          clients.get(data.auctionId)?.add(ws);
          console.log(`Client subscribed to auction: ${data.auctionId}`);
        }
        
        if (data.type === 'unsubscribe' && data.auctionId) {
          clients.get(data.auctionId)?.delete(ws);
          console.log(`Client unsubscribed from auction: ${data.auctionId}`);
        }
      } catch (error) {
        console.error('WebSocket message error:', error);
      }
    });
    
    ws.on('close', () => {
      clients.forEach((subscribers) => {
        subscribers.delete(ws);
      });
      console.log('WebSocket client disconnected');
    });
    
    ws.on('error', (error) => {
      console.error('WebSocket error:', error);
    });
  });
  
  // Broadcast function for bid updates
  app.locals.broadcastBid = (message: BidMessage) => {
    const subscribers = clients.get(message.auctionId);
    if (subscribers) {
      const payload = JSON.stringify(message);
      subscribers.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      });
    }
  };

  return httpServer;
}
