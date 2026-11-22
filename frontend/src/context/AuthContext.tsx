import React, { createContext, useContext, useEffect, useState } from 'react';
import api from '../api/client';
import type { User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName?: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [, setToken] = useState<string | null>(localStorage.getItem('cp_access_token'));
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchMe = async () => {
      const storedToken = localStorage.getItem('cp_access_token');
      if (storedToken) {
        try {
          const res = await api.get('/auth/me');
          setUser(res.data.data);
        } catch {
          localStorage.removeItem('cp_access_token');
          localStorage.removeItem('cp_refresh_token');
          setUser(null);
          setToken(null);
        }
      }
      setLoading(false);
    };

    fetchMe();
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.post('/auth/login', { email, password });
    const { accessToken, refreshToken, user: authUser } = res.data.data;
    localStorage.setItem('cp_access_token', accessToken);
    localStorage.setItem('cp_refresh_token', refreshToken);
    setToken(accessToken);
    setUser(authUser);
  };

  const register = async (email: string, password: string, firstName: string, lastName?: string) => {
    const res = await api.post('/auth/register', { email, password, firstName, lastName });
    const { accessToken, refreshToken, user: authUser } = res.data.data;
    localStorage.setItem('cp_access_token', accessToken);
    localStorage.setItem('cp_refresh_token', refreshToken);
    setToken(accessToken);
    setUser(authUser);
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Ignored
    } finally {
      localStorage.removeItem('cp_access_token');
      localStorage.removeItem('cp_refresh_token');
      setUser(null);
      setToken(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, token: localStorage.getItem('cp_access_token'), loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
