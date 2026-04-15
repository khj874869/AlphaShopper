"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addCartItem, getProducts, searchProducts } from "@/lib/api";
import { buildAuthPath } from "@/lib/auth";
import { formatCurrency } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";
import { ProductVisual } from "@/components/product-visual";

const tags = ["denim", "bag", "knit", "loafer", "jacket"];

export function ProductCatalogView() {
  const [keyword, setKeyword] = useState("");
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();

  const { data: products } = useQuery({
    queryKey: ["products"],
    queryFn: getProducts
  });
  const { data: searchResult } = useQuery({
    queryKey: ["search", keyword],
    queryFn: () => searchProducts(keyword),
    enabled: keyword.trim().length > 0
  });

  const addToCartMutation = useMutation({
    mutationFn: (productId: number) => addCartItem(member!.id, { productId, quantity: 1 }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["cart", member?.id] });
    }
  });

  const items = keyword.trim().length > 0 ? searchResult?.content ?? [] : products ?? [];

  return (
    <div className="page-stack">
      <section className="catalog-hero">
        <div>
          <p className="eyebrow">Explore products</p>
          <h1>Dense fashion catalog with seeded product artwork and prices.</h1>
        </div>
        <label className="search-bar search-bar--wide">
          <span>Search</span>
          <input
            placeholder="Search by brand, product or mood"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        </label>
        <div className="chip-row">
          {tags.map((tag) => (
            <button key={tag} className="chip" onClick={() => setKeyword(tag)}>
              #{tag}
            </button>
          ))}
        </div>
      </section>

      {!member ? (
        <section className="panel auth-callout">
          <div>
            <p className="eyebrow">Shopping session</p>
            <h2>Product browsing is public. Cart actions require login.</h2>
          </div>
          <Link className="button button--dark" href={buildAuthPath({ next: "/products" })}>
            Login
          </Link>
        </section>
      ) : null}

      <section className="catalog-grid">
        {items.map((product) => (
          <article className="catalog-card" key={product.id}>
            <ProductVisual brand={product.brand} className="catalog-card__poster" imageUrl={product.imageUrl} />
            <div className="catalog-card__meta">
              <p>{product.brand}</p>
              <h2>{product.name}</h2>
              <strong>{formatCurrency(product.price)}</strong>
              <small>Stock {product.stockQuantity}</small>
              <p className="muted">{product.description}</p>
            </div>
            <div className="catalog-card__actions">
              <Link className="button button--ghostDark" href={`/products/${product.id}`}>
                View
              </Link>
              {member ? (
                <button className="button button--dark" onClick={() => addToCartMutation.mutate(product.id)}>
                  Add to bag
                </button>
              ) : (
                <Link className="button button--dark" href={buildAuthPath({ next: `/products/${product.id}` })}>
                  Login to buy
                </Link>
              )}
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
