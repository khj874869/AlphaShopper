"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { useMutation } from "@tanstack/react-query";
import { reportCheckoutFailure } from "@/lib/api";
import { useSessionStore } from "@/store/session-store";

export function TossPaymentFailView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const member = useSessionStore((state) => state.member);

  const errorCode = searchParams.get("code");
  const errorMessage = searchParams.get("message");
  const providerOrderId = searchParams.get("orderId");

  const failMutation = useMutation({
    mutationFn: () =>
      reportCheckoutFailure({
        memberId: member!.id,
        providerOrderId: providerOrderId!,
        errorCode,
        errorMessage
      })
  });

  useEffect(() => {
    if (!member) {
      router.replace("/login?next=/cart");
      return;
    }

    if (!providerOrderId) {
      return;
    }

    if (!failMutation.isPending && !failMutation.isSuccess && !failMutation.error) {
      failMutation.mutate();
    }
  }, [failMutation, member, providerOrderId, router]);

  if (!member) {
    return null;
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <p className="eyebrow">Payment failure</p>
          <h1>Toss Payments checkout was not completed</h1>
        </div>
      </div>

      <div className="result-card result-card--danger">
        <strong>{errorCode ?? "PAYMENT_FAILED"}</strong>
        <p>{errorMessage ?? "The payment could not be completed."}</p>
        <span>{providerOrderId ? `Provider order ID: ${providerOrderId}` : "No provider order ID was returned."}</span>
      </div>

      <div className="auth-actions">
        <Link className="button button--dark" href="/cart">
          Back to cart
        </Link>
        <Link className="button button--ghostDark" href="/products">
          Continue shopping
        </Link>
      </div>
    </section>
  );
}
