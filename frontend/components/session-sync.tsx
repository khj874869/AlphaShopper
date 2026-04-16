"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getSessionProfile } from "@/lib/api";
import { useSessionStore } from "@/store/session-store";

export function SessionSync() {
  const member = useSessionStore((state) => state.member);
  const signIn = useSessionStore((state) => state.signIn);
  const signOut = useSessionStore((state) => state.signOut);

  const { data } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getSessionProfile,
    retry: false,
    staleTime: 60_000
  });

  useEffect(() => {
    if (data && (!member || member.id !== data.id || member.email !== data.email || member.name !== data.name || member.role !== data.role)) {
      signIn(data);
      return;
    }

    if (data === null && member) {
      signOut();
    }
  }, [data, member, signIn, signOut]);

  return null;
}
