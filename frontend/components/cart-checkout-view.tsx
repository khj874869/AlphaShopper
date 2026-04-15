"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { checkout, clearCart, getCart, getCoupons, removeCartItem } from "@/lib/api";
import { buildAuthPath } from "@/lib/auth";
import { formatCurrency } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";

const paymentMethods = ["CARD", "KAKAO_PAY", "NAVER_PAY", "BANK_TRANSFER"] as const;

export function CartCheckoutView() {
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();
  const [couponCode, setCouponCode] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<(typeof paymentMethods)[number]>("CARD");
  const [paymentReference, setPaymentReference] = useState("CARD-OK-001");
  const [shippingAddress, setShippingAddress] = useState("Seoul Seongsu-ro 00");

  const { data: cart } = useQuery({
    queryKey: ["cart", member?.id],
    queryFn: () => getCart(member!.id),
    enabled: Boolean(member?.id)
  });
  const { data: coupons } = useQuery({
    queryKey: ["coupons"],
    queryFn: getCoupons,
    enabled: Boolean(member?.id)
  });

  const checkoutMutation = useMutation({
    mutationFn: () =>
      checkout({
        memberId: member!.id,
        paymentMethod,
        paymentReference,
        shippingAddress,
        couponCode: couponCode || null
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
      void queryClient.invalidateQueries({ queryKey: ["orders", member?.id] });
    }
  });

  const removeItemMutation = useMutation({
    mutationFn: (productId: number) => removeCartItem(member!.id, productId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
    }
  });

  const clearCartMutation = useMutation({
    mutationFn: () => clearCart(member!.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
    }
  });

  const hasItems = Boolean(cart?.items.length);
  const checkoutResult = checkoutMutation.data;

  const estimatedPayAmount = useMemo(() => {
    if (!cart) {
      return 0;
    }
    return Number(cart.totalAmount);
  }, [cart]);

  if (!member) {
    return (
      <section className="panel auth-callout">
        <div>
          <p className="eyebrow">Protected cart</p>
          <h1>Login is required before using the cart or checkout flow.</h1>
          <p className="muted">Demo account: buyer1@zigzag.local / buyer1234</p>
        </div>
        <div className="auth-actions">
          <Link className="button button--dark" href={buildAuthPath({ next: "/cart" })}>
            Open login
          </Link>
          <Link className="button button--ghostDark" href={buildAuthPath({ next: "/cart", mode: "register" })}>
            Create account
          </Link>
        </div>
      </section>
    );
  }

  return (
    <div className="page-stack">
      <section className="checkout-layout">
        <div className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">Bag overview</p>
              <h1>Fast checkout rail</h1>
            </div>
            {hasItems ? (
              <button className="text-link" onClick={() => clearCartMutation.mutate()}>
                Clear all
              </button>
            ) : null}
          </div>

          <div className="cart-list">
            {cart?.items.length ? (
              cart.items.map((item) => (
                <article className="cart-line" key={item.productId}>
                  <div>
                    <strong>{item.productName}</strong>
                    <p>
                      {item.quantity} x {formatCurrency(item.unitPrice)}
                    </p>
                  </div>
                  <div className="cart-line__actions">
                    <strong>{formatCurrency(item.lineTotal)}</strong>
                    <button onClick={() => removeItemMutation.mutate(item.productId)}>Remove</button>
                  </div>
                </article>
              ))
            ) : (
              <div className="empty-state">Your cart is empty. Add products from the catalog first.</div>
            )}
          </div>
        </div>

        <div className="panel panel--sticky">
          <div className="panel-head">
            <div>
              <p className="eyebrow">Checkout</p>
              <h2>Coupon and payment selection</h2>
            </div>
          </div>
          <label className="field">
            <span>Shipping address</span>
            <input value={shippingAddress} onChange={(event) => setShippingAddress(event.target.value)} />
          </label>
          <label className="field">
            <span>Payment method</span>
            <select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value as (typeof paymentMethods)[number])}>
              {paymentMethods.map((method) => (
                <option key={method} value={method}>
                  {method}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Payment reference</span>
            <input value={paymentReference} onChange={(event) => setPaymentReference(event.target.value)} />
          </label>
          <label className="field">
            <span>Coupon</span>
            <select value={couponCode} onChange={(event) => setCouponCode(event.target.value)}>
              <option value="">No coupon</option>
              {coupons?.map((coupon) => (
                <option key={coupon.id} value={coupon.code}>
                  {coupon.code} - {coupon.name}
                </option>
              ))}
            </select>
          </label>

          <div className="summary-box">
            <div>
              <span>Cart total</span>
              <strong>{formatCurrency(estimatedPayAmount)}</strong>
            </div>
            <p className="muted">
              Put <code>FAIL</code> or <code>DECLINE</code> in the payment reference to force a failed payment case.
            </p>
          </div>

          <button className="button button--dark button--block" disabled={!hasItems || checkoutMutation.isPending} onClick={() => checkoutMutation.mutate()}>
            Run checkout
          </button>

          {checkoutResult ? (
            <div className={`result-card ${checkoutResult.status === "PAID" ? "result-card--success" : "result-card--danger"}`}>
              <strong>{checkoutResult.status}</strong>
              <p>
                Order #{checkoutResult.orderId} - {formatCurrency(checkoutResult.payAmount)}
              </p>
              <span>{checkoutResult.payment.status}</span>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
