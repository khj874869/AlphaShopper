"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addCartItem, getCoupons, getOrders, getProducts, searchProducts } from "@/lib/api";
import { buildAuthPath } from "@/lib/auth";
import { formatCurrency } from "@/lib/format";
import { DEMO_ACCOUNTS_ENABLED } from "@/lib/runtime";
import { useSessionStore } from "@/store/session-store";
import { ProductVisual } from "@/components/product-visual";
import type { ProductResponse } from "@/lib/types";

const spotlightWords = ["denim", "editorial outer", "spring bag", "office skirt"];

export function HomeView() {
  const member = useSessionStore((state) => state.member);
  const [keyword, setKeyword] = useState("denim");
  const queryClient = useQueryClient();

  const { data: products } = useQuery({
    queryKey: ["products"],
    queryFn: getProducts
  });
  const { data: coupons } = useQuery({
    queryKey: ["coupons"],
    queryFn: getCoupons
  });
  const { data: searchResult } = useQuery({
    queryKey: ["search", keyword],
    queryFn: () => searchProducts(keyword),
    enabled: keyword.trim().length > 0
  });
  const { data: orders } = useQuery({
    queryKey: ["orders", member?.id],
    queryFn: () => getOrders(member!.id),
    enabled: Boolean(member?.id)
  });

  const addToCartMutation = useMutation({
    mutationFn: (productId: number) => addCartItem(member!.id, { productId, quantity: 1 }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
    }
  });

  const featuredProducts = useMemo(() => (products ?? []).slice(0, 3), [products]);

  return (
    <div className="page-stack">
      <section className="hero-panel">
        <div className="hero-panel__content">
          <p className="eyebrow">AlphaShopper editorial board</p>
          <h1>
            Musinsa-like visual density,
            <br />
            Zigzag-like fast shopping flow.
          </h1>
          <p className="hero-copy">
            Search, cart, coupon, order and delivery status are connected in one web app flow.
          </p>
          <div className="hero-actions">
            <Link className="button button--light" href="/products">
              Explore products
            </Link>
            <Link className="button button--ghost" href={member ? "/cart" : buildAuthPath({ next: "/cart" })}>
              {member ? "Checkout now" : "Login to shop"}
            </Link>
          </div>
        </div>
        <div className="hero-panel__board">
          <div className="metric-card">
            <span>drop keywords</span>
            <strong>{spotlightWords.join(" / ")}</strong>
          </div>
          <div className="metric-grid">
            <article>
              <span>Live products</span>
              <strong>{products?.length ?? 0}</strong>
            </article>
            <article>
              <span>Coupons ready</span>
              <strong>{coupons?.length ?? 0}</strong>
            </article>
            <article>
              <span>Recent orders</span>
              <strong>{orders?.length ?? 0}</strong>
            </article>
          </div>
        </div>
      </section>

      {!member ? (
        <section className="panel auth-callout">
          <div>
            <p className="eyebrow">Secure shopping</p>
            <h2>Secure login is now required for cart and order APIs.</h2>
            {DEMO_ACCOUNTS_ENABLED ? (
              <p className="muted">
                Demo account: <strong>buyer1@zigzag.local / buyer1234</strong>
              </p>
            ) : (
              <p className="muted">Sign in with a shopper account to use cart and order features.</p>
            )}
          </div>
          <Link className="button button--dark" href={buildAuthPath({ next: "/cart" })}>
            Open login
          </Link>
        </section>
      ) : null}

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Search acceleration</p>
            <h2>Elastic-backed keyword search</h2>
          </div>
          <div className="chip-row">
            {spotlightWords.map((word) => (
              <button key={word} className="chip" onClick={() => setKeyword(word)}>
                {word}
              </button>
            ))}
          </div>
        </div>
        <label className="search-bar">
          <span>Keyword</span>
          <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        </label>
        <div className="product-grid">
          {searchResult?.content.map((product) => (
            <ProductTile
              key={product.id}
              product={product}
              canAdd={Boolean(member)}
              onAdd={() => addToCartMutation.mutate(product.id)}
            />
          ))}
        </div>
      </section>

      <section className="two-column">
        <div className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">Curated floor</p>
              <h2>Editorial picks with seeded images and prices</h2>
            </div>
          </div>
          <div className="product-grid">
            {featuredProducts.map((product) => (
              <ProductTile
                key={product.id}
                product={product}
                canAdd={Boolean(member)}
                onAdd={() => addToCartMutation.mutate(product.id)}
              />
            ))}
          </div>
        </div>

        <div className="panel panel--accent">
          <div className="panel-head">
            <div>
              <p className="eyebrow">Coupon lane</p>
              <h2>Ready-to-apply benefits</h2>
            </div>
          </div>
          <div className="coupon-stack">
            {coupons?.map((coupon) => (
              <article className="coupon-card" key={coupon.id}>
                <span>{coupon.code}</span>
                <strong>{coupon.name}</strong>
              </article>
            ))}
          </div>
          <Link className="button button--dark" href={member ? "/cart" : buildAuthPath({ next: "/cart" })}>
            {member ? "Use coupon at checkout" : "Login to use coupon"}
          </Link>
        </div>
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Order radar</p>
            <h2>Recent order status</h2>
          </div>
          <Link className="text-link" href={member ? "/orders" : buildAuthPath({ next: "/orders" })}>
            {member ? "View all orders" : "Login first"}
          </Link>
        </div>
        <div className="order-strip">
          {orders?.length ? (
            orders.slice(0, 4).map((order) => (
              <article className="order-card" key={order.orderId}>
                <span>#{order.orderId}</span>
                <strong>{order.status}</strong>
                <p>{order.deliveryStatus}</p>
                <em>{formatCurrency(order.payAmount)}</em>
              </article>
            ))
          ) : (
            <div className="empty-state">
              {member ? "No orders yet. Try a checkout flow from the cart." : "Login to see your order board."}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

function ProductTile({
  product,
  canAdd,
  onAdd
}: {
  product: Pick<ProductResponse, "id" | "name" | "brand" | "price" | "description" | "imageUrl">;
  canAdd: boolean;
  onAdd: () => void;
}) {
  return (
    <article className="product-card">
      <ProductVisual brand={product.brand} className="product-card__visual" imageUrl={product.imageUrl} />
      <div className="product-card__body">
        <div>
          <p>{product.brand}</p>
          <h3>{product.name}</h3>
          <span>{formatCurrency(product.price)}</span>
        </div>
        <p className="muted">{product.description}</p>
      </div>
      <div className="product-card__actions">
        <Link className="button button--ghostDark" href={`/products/${product.id}`}>
          Detail
        </Link>
        {canAdd ? (
          <button className="button button--dark" onClick={onAdd}>
            Add to bag
          </button>
        ) : (
          <Link className="button button--dark" href={buildAuthPath({ next: `/products/${product.id}` })}>
            Login to buy
          </Link>
        )}
      </div>
    </article>
  );
}
