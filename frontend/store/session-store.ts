"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { MemberResponse } from "@/lib/types";

type SessionState = {
  accessToken: string | null;
  member: MemberResponse | null;
  signIn: (accessToken: string, member: MemberResponse) => void;
  signOut: () => void;
};

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      accessToken: null,
      member: null,
      signIn: (accessToken, member) => set({ accessToken, member }),
      signOut: () => set({ accessToken: null, member: null })
    }),
    {
      name: "alphashopper-web-session"
    }
  )
);
