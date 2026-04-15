"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addCartItem, getProduct } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";
import { ProductVisual } from "@/components/product-visual";

export function ProductDetailView({ productId }: { productId: number }) {
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();
  const { data: product } = useQuery({
    queryKey: ["product", productId],
    queryFn: () => getProduct(productId)
  });

  const addToCartMutation = useMutation({
    mutationFn: () => addCartItem(member!.id, { productId, quantity: 1 }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
    }
  });

  if (!product) {
    return <div className="panel">Loading product details.</div>;
  }

  return (
    <div className="page-stack">
      <section className="detail-hero">
        <ProductVisual brand={product.brand} className="detail-hero__visual" imageUrl={product.imageUrl} />
        <div className="detail-hero__body">
          <p className="eyebrow">{product.brand}</p>
          <h1>{product.name}</h1>
          <strong>{formatCurrency(product.price)}</strong>
          <p className="detail-copy">{product.description}</p>
          <div className="detail-stats">
            <article>
              <span>Stock</span>
              <strong>{product.stockQuantity}</strong>
            </article>
            <article>
              <span>JWT checkout</span>
              <strong>{member ? "ready" : "login required"}</strong>
            </article>
          </div>
          <div className="hero-actions">
            {member ? (
              <button className="button button--dark" onClick={() => addToCartMutation.mutate()}>
                Add to bag
              </button>
            ) : (
              <Link className="button button--dark" href="/login">
                Login to buy
              </Link>
            )}
            <Link className="button button--ghostDark" href="/cart">
              Go to cart
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
