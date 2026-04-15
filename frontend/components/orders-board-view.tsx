"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getOrders, refundOrder, updateDelivery } from "@/lib/api";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";

export function OrdersBoardView() {
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();
  const { data: orders } = useQuery({
    queryKey: ["orders", member?.id],
    queryFn: () => getOrders(member!.id),
    enabled: Boolean(member?.id)
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["orders", member?.id] });
  };

  const refundMutation = useMutation({
    mutationFn: (orderId: number) => refundOrder(orderId, { reason: "Web app refund demo" }),
    onSuccess: refresh
  });

  const shipMutation = useMutation({
    mutationFn: (orderId: number) =>
      updateDelivery(orderId, {
        deliveryStatus: "SHIPPED",
        trackingNumber: `ALPHA-${orderId}`
      }),
    onSuccess: refresh
  });

  const deliverMutation = useMutation({
    mutationFn: (orderId: number) =>
      updateDelivery(orderId, {
        deliveryStatus: "DELIVERED",
        trackingNumber: null
      }),
    onSuccess: refresh
  });

  if (!member) {
    return (
      <section className="panel auth-callout">
        <div>
          <p className="eyebrow">Protected orders</p>
          <h1>Login is required to view or manage your orders.</h1>
        </div>
        <Link className="button button--dark" href="/login">
          Open login
        </Link>
      </section>
    );
  }

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Order control board</p>
            <h1>Track payment and delivery state in one place</h1>
          </div>
        </div>

        <div className="orders-grid">
          {orders?.length ? (
            orders.map((order) => (
              <article className="timeline-card" key={order.orderId}>
                <div className="timeline-card__top">
                  <div>
                    <span>ORDER #{order.orderId}</span>
                    <strong>{order.status}</strong>
                  </div>
                  <em>{formatCurrency(order.payAmount)}</em>
                </div>
                <p>{order.deliveryStatus}</p>
                <small>{formatDateTime(order.orderedAt)}</small>
                <div className="timeline-card__actions">
                  <button onClick={() => refundMutation.mutate(order.orderId)}>Refund</button>
                  {member.role === "ADMIN" ? (
                    <>
                      <button onClick={() => shipMutation.mutate(order.orderId)}>Ship</button>
                      <button onClick={() => deliverMutation.mutate(order.orderId)}>Deliver</button>
                    </>
                  ) : null}
                </div>
              </article>
            ))
          ) : (
            <div className="empty-state">No order history yet. Run a checkout from the cart first.</div>
          )}
        </div>
      </section>
    </div>
  );
}
