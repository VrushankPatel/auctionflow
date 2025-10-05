import { useAuthStore } from '@/stores/auth';

export interface WebSocketMessage {
  type: string;
  auctionId: string;
  data?: any;
  timestamp?: string;
}

export class WebSocketService {
  private socket: WebSocket | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private messageHandlers: Set<(message: WebSocketMessage) => void> = new Set();
  private statusHandlers: Set<(status: 'connected' | 'disconnected' | 'reconnecting') => void> = new Set();
  private subscribedAuctions: Set<string> = new Set();

  connect() {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.notifyStatus('connected');
        if (this.reconnectTimer) {
          clearInterval(this.reconnectTimer);
          this.reconnectTimer = null;
        }

        // Resubscribe to auctions
        this.subscribedAuctions.forEach((auctionId) => {
          this.subscribe(auctionId);
        });
      };

      this.socket.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);
          this.messageHandlers.forEach((handler) => handler(message));
        } catch (error) {
          console.error('WebSocket message parse error:', error);
        }
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };

      this.socket.onclose = () => {
        console.log('WebSocket disconnected');
        this.notifyStatus('disconnected');
        this.attemptReconnect();
      };
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.attemptReconnect();
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearInterval(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  subscribe(auctionId: string) {
    this.subscribedAuctions.add(auctionId);
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify({ type: 'subscribe', auctionId }));
    }
  }

  unsubscribe(auctionId: string) {
    this.subscribedAuctions.delete(auctionId);
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify({ type: 'unsubscribe', auctionId }));
    }
  }

  onMessage(handler: (message: WebSocketMessage) => void) {
    this.messageHandlers.add(handler);
    return () => this.messageHandlers.delete(handler);
  }

  onStatusChange(handler: (status: 'connected' | 'disconnected' | 'reconnecting') => void) {
    this.statusHandlers.add(handler);
    return () => this.statusHandlers.delete(handler);
  }

  private attemptReconnect() {
    if (this.reconnectTimer) {
      return;
    }

    this.notifyStatus('reconnecting');
    this.reconnectTimer = setInterval(() => {
      console.log('Attempting to reconnect WebSocket...');
      this.connect();
    }, 5000);
  }

  private notifyStatus(status: 'connected' | 'disconnected' | 'reconnecting') {
    this.statusHandlers.forEach((handler) => handler(status));
  }
}

export const websocketService = new WebSocketService();
