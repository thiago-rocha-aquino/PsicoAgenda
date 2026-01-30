"use client";

import { useState, useEffect, useCallback } from "react";
import { authApi, AuthResponse } from "@/lib/api";

const TOKEN_KEY = "psicoagenda_token";
const REFRESH_KEY = "psicoagenda_refresh";
const USER_KEY = "psicoagenda_user";

export function useAuth() {
  const [user, setUser] = useState<AuthResponse["user"] | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_KEY);
    const storedUser = localStorage.getItem(USER_KEY);

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password);
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    setToken(response.accessToken);
    setUser(response.user);
    return response;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const refreshToken = useCallback(async () => {
    const storedRefresh = localStorage.getItem(REFRESH_KEY);
    if (!storedRefresh) {
      logout();
      return;
    }

    try {
      const response = await authApi.refresh(storedRefresh);
      localStorage.setItem(TOKEN_KEY, response.accessToken);
      localStorage.setItem(REFRESH_KEY, response.refreshToken);
      setToken(response.accessToken);
    } catch {
      logout();
    }
  }, [logout]);

  return {
    user,
    token,
    loading,
    isAuthenticated: !!token,
    login,
    logout,
    refreshToken,
  };
}
