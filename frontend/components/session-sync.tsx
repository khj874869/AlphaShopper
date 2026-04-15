"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getMyProfile } from "@/lib/api";
import { useSessionStore } from "@/store/session-store";

export function SessionSync() {
  const accessToken = useSessionStore((state) => state.accessToken);
  const member = useSessionStore((state) => state.member);
  const signIn = useSessionStore((state) => state.signIn);
  const signOut = useSessionStore((state) => state.signOut);

  const { data, error } = useQuery({
    queryKey: ["auth", "me", accessToken],
    queryFn: getMyProfile,
    enabled: Boolean(accessToken),
    retry: false,
    staleTime: 60_000
  });

  useEffect(() => {
    if (
      accessToken &&
      data &&
      (!member ||
        member.id !== data.id ||
        member.email !== data.email ||
        member.name !== data.name ||
        member.role !== data.role)
    ) {
      signIn(accessToken, data);
    }
  }, [accessToken, data, member, signIn]);

  useEffect(() => {
    if (accessToken && error) {
      signOut();
    }
  }, [accessToken, error, signOut]);

  return null;
}
