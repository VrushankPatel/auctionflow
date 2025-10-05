import { useAuctionTimer } from '@/hooks/use-auction-timer';
import { Clock } from 'lucide-react';
import { cn } from '@/lib/utils';

interface CountdownTimerProps {
  endTime: string;
  className?: string;
  showIcon?: boolean;
}

export function CountdownTimer({ endTime, className, showIcon = true }: CountdownTimerProps) {
  const timer = useAuctionTimer(endTime);

  return (
    <div
      className={cn(
        'flex items-center gap-2',
        timer.isUrgent && 'timer-urgent',
        className
      )}
      data-testid="countdown-timer"
    >
      {showIcon && <Clock className="w-4 h-4" />}
      <span className="font-semibold" data-testid="text-time-remaining">
        {timer.formatted}
      </span>
    </div>
  );
}
