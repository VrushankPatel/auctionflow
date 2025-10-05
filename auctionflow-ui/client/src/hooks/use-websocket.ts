import { useEffect, useState } from 'react';
import { websocketService, type WebSocketMessage } from '@/services/websocket';
import { useToast } from '@/hooks/use-toast';

export function useWebSocket(auctionId?: string) {
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'reconnecting'>('disconnected');
  const { toast } = useToast();

  useEffect(() => {
    websocketService.connect();

    const unsubscribeStatus = websocketService.onStatusChange((newStatus) => {
      setStatus(newStatus);
      
      if (newStatus === 'connected') {
        toast({
          title: 'Connected',
          description: 'Real-time updates active',
        });
      } else if (newStatus === 'reconnecting') {
        toast({
          title: 'Reconnecting...',
          description: 'Live updates temporarily unavailable',
          variant: 'destructive',
        });
      }
    });

    return () => {
      unsubscribeStatus();
    };
  }, [toast]);

  useEffect(() => {
    if (auctionId) {
      websocketService.subscribe(auctionId);
      return () => {
        websocketService.unsubscribe(auctionId);
      };
    }
  }, [auctionId]);

  const onMessage = (handler: (message: WebSocketMessage) => void) => {
    return websocketService.onMessage(handler);
  };

  return { status, onMessage };
}
