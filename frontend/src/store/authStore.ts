import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User, AuthResponse } from '../types';

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  needsOnboarding: boolean;
  setAuth: (response: AuthResponse) => void;
  updateUser: (user: Partial<User>) => void;
  completeOnboarding: () => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      needsOnboarding: false,
      
      setAuth: (response: AuthResponse) => {
        set({
          token: response.token,
          user: {
            id: response.userId,
            email: response.email,
            name: response.name,
            onboardingComplete: response.onboardingComplete,
          },
          isAuthenticated: true,
          needsOnboarding: response.firstLogin || !response.onboardingComplete,
        });
      },
      
      updateUser: (userData: Partial<User>) => {
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : null,
        }));
      },
      
      completeOnboarding: () => {
        set((state) => ({
          needsOnboarding: false,
          user: state.user ? { ...state.user, onboardingComplete: true } : null,
        }));
      },
      
      logout: () => {
        set({
          token: null,
          user: null,
          isAuthenticated: false,
          needsOnboarding: false,
        });
      },
    }),
    {
      name: 'shelve-auth',
    }
  )
);
