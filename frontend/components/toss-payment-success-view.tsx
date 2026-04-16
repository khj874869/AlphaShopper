"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { confirmCheckout } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";

export function TossPaymentSuccessView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();

  const paymentKey = searchParams.get("paymentKey") ?? "";
  const providerOrderId = searchParams.get("orderId") ?? "";
  const amount = Number(searchParams.get("amount") ?? "0");

  const confirmMutation = useMutation({
    mutationFn: () =>
      confirmCheckout({
        memberId: member!.id,
        providerOrderId,
        paymentKey,
        amount
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
      await queryClient.invalidateQueries({ queryKey: ["orders", member?.id] });
    }
  });

  useEffect(() => {
    if (!member) {
      router.replace("/login?next=/cart");
      return;
    }

    if (!paymentKey || !providerOrderId || !Number.isFinite(amount) || amount <= 0) {
      return;
    }

    if (!confirmMutation.isPending && !confirmMutation.data && !confirmMutation.error) {
      confirmMutation.mutate();
    }
  }, [amount, confirmMutation, member, paymentKey, providerOrderId, router]);

  if (!member) {
    return null;
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <p className="eyebrow">Payment confirmation</p>
          <h1>Toss Payments approval result</h1>
        </div>
      </div>

      {confirmMutation.isPending ? <p className="muted">Confirming payment with the server...</p> : null}

      {confirmMutation.error ? (
        <div className="result-card result-card--danger">
          <strong>Confirmation failed</strong>
          <p>{confirmMutation.error instanceof Error ? confirmMutation.error.message : "Request failed."}</p>
          <span>Provider order ID: {providerOrderId}</span>
        </div>
      ) : null}

      {confirmMutation.data ? (
        <div className="result-card result-card--success">
          <strong>{confirmMutation.data.status}</strong>
          <p>
            Order #{confirmMutation.data.orderId} - {formatCurrency(confirmMutation.data.payAmount)}
          </p>
          <span>Payment key: {confirmMutation.data.payment.transactionKey}</span>
        </div>
      ) : null}

      <div className="auth-actions">
        <Link className="button button--dark" href="/orders">
          View orders
        </Link>
        <Link className="button button--ghostDark" href="/products">
          Continue shopping
        </Link>
      </div>
    </section>
  );
}
