"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { MemberResponse } from "@/lib/types";

type SessionState = {
  member: MemberResponse | null;
  signIn: (member: MemberResponse) => void;
  signOut: () => void;
};

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      member: null,
      signIn: (member) => set({ member }),
      signOut: () => set({ member: null })
    }),
    {
      name: "alphashopper-web-session",
      partialize: (state) => ({ member: state.member })
    }
  )
);
