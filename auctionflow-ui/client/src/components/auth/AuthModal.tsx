import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { api } from '@/services/api';
import { useAuthStore } from '@/stores/auth';
import { useUIStore } from '@/stores/ui';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useToast } from '@/hooks/use-toast';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

const registerSchema = loginSchema.extend({
  displayName: z.string().min(2, 'Name must be at least 2 characters'),
  role: z.enum(['BUYER', 'SELLER']),
});

type LoginForm = z.infer<typeof loginSchema>;
type RegisterForm = z.infer<typeof registerSchema>;

export function AuthModal() {
  const { authModalOpen, setAuthModalOpen, authMode, setAuthMode } = useUIStore();
  const { setAuth } = useAuthStore();
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState<'login' | 'register'>(authMode);

  const loginForm = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const registerForm = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '', displayName: '', role: 'BUYER' },
  });

  const loginMutation = useMutation({
    mutationFn: api.login,
    onSuccess: (data) => {
      setAuth(data.user, data.token);
      setAuthModalOpen(false);
      toast({
        title: 'Welcome back!',
        description: 'You have successfully logged in.',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Login failed',
        description: error.message || 'Invalid email or password',
        variant: 'destructive',
      });
    },
  });

  const registerMutation = useMutation({
    mutationFn: api.register,
    onSuccess: () => {
      toast({
        title: 'Account created!',
        description: 'Please login with your credentials.',
      });
      setActiveTab('login');
    },
    onError: (error: any) => {
      toast({
        title: 'Registration failed',
        description: error.message || 'Failed to create account',
        variant: 'destructive',
      });
    },
  });

  const handleLogin = (data: LoginForm) => {
    loginMutation.mutate(data);
  };

  const handleRegister = (data: RegisterForm) => {
    registerMutation.mutate(data);
  };

  return (
    <Dialog open={authModalOpen} onOpenChange={setAuthModalOpen}>
      <DialogContent className="sm:max-w-md" data-testid="modal-auth">
        <DialogHeader>
          <DialogTitle>Welcome to AuctionFlow</DialogTitle>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'login' | 'register')}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="login" data-testid="tab-login">Sign In</TabsTrigger>
            <TabsTrigger value="register" data-testid="tab-register">Register</TabsTrigger>
          </TabsList>

          <TabsContent value="login">
            <form onSubmit={loginForm.handleSubmit(handleLogin)} className="space-y-4" data-testid="form-login">
              <div>
                <Label htmlFor="login-email">Email</Label>
                <Input
                  id="login-email"
                  type="email"
                  placeholder="your@email.com"
                  {...loginForm.register('email')}
                  data-testid="input-email"
                />
                {loginForm.formState.errors.email && (
                  <p className="text-sm text-destructive mt-1">{loginForm.formState.errors.email.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="login-password">Password</Label>
                <Input
                  id="login-password"
                  type="password"
                  placeholder="••••••••"
                  {...loginForm.register('password')}
                  data-testid="input-password"
                />
                {loginForm.formState.errors.password && (
                  <p className="text-sm text-destructive mt-1">{loginForm.formState.errors.password.message}</p>
                )}
              </div>

              <Button
                type="submit"
                className="w-full"
                disabled={loginMutation.isPending}
                data-testid="button-submit-login"
              >
                {loginMutation.isPending ? 'Signing in...' : 'Sign In'}
              </Button>
            </form>
          </TabsContent>

          <TabsContent value="register">
            <form onSubmit={registerForm.handleSubmit(handleRegister)} className="space-y-4" data-testid="form-register">
              <div>
                <Label htmlFor="register-name">Full Name</Label>
                <Input
                  id="register-name"
                  type="text"
                  placeholder="John Doe"
                  {...registerForm.register('displayName')}
                  data-testid="input-displayname"
                />
                {registerForm.formState.errors.displayName && (
                  <p className="text-sm text-destructive mt-1">{registerForm.formState.errors.displayName.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="register-email">Email</Label>
                <Input
                  id="register-email"
                  type="email"
                  placeholder="your@email.com"
                  {...registerForm.register('email')}
                  data-testid="input-register-email"
                />
                {registerForm.formState.errors.email && (
                  <p className="text-sm text-destructive mt-1">{registerForm.formState.errors.email.message}</p>
                )}
              </div>

              <div>
                <Label htmlFor="register-password">Password</Label>
                <Input
                  id="register-password"
                  type="password"
                  placeholder="••••••••"
                  {...registerForm.register('password')}
                  data-testid="input-register-password"
                />
                {registerForm.formState.errors.password && (
                  <p className="text-sm text-destructive mt-1">{registerForm.formState.errors.password.message}</p>
                )}
              </div>

              <Button
                type="submit"
                className="w-full"
                disabled={registerMutation.isPending}
                data-testid="button-submit-register"
              >
                {registerMutation.isPending ? 'Creating account...' : 'Create Account'}
              </Button>
            </form>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
