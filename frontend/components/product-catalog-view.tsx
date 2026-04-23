"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addCartItem,
  getProducts,
  recordProductDiscoveryClick,
  recordProductDiscoveryImpressions,
  searchProducts
} from "@/lib/api";
import { buildAuthPath } from "@/lib/auth";
import { formatCurrency } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";
import { ProductVisual } from "@/components/product-visual";
import type { ProductSearchResponse } from "@/lib/types";

const tags = ["denim", "bag", "knit", "loafer", "jacket"];

export function ProductCatalogView() {
  const [keyword, setKeyword] = useState("");
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();
  const recordedImpressionsRef = useRef(new Set<string>());

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
  const trimmedKeyword = keyword.trim();

  useEffect(() => {
    if (!trimmedKeyword || !searchResult?.content.length) {
      return;
    }

    const productIds = searchResult.content.map((product) => product.id).join(",");
    const impressionKey = `catalog-search:${trimmedKeyword}:${productIds}`;
    if (recordedImpressionsRef.current.has(impressionKey)) {
      return;
    }
    recordedImpressionsRef.current.add(impressionKey);

    void recordProductDiscoveryImpressions({
      impressions: searchResult.content.map((product, index) => ({
        memberId: member?.id,
        surface: "SEARCH",
        query: trimmedKeyword,
        productId: product.id,
        productName: product.name,
        recommendationSource: "ELASTICSEARCH",
        searchScore: product.searchScore,
        rankPosition: index + 1,
        highlights: product.highlights,
        impressionKey
      }))
    }).catch(() => undefined);
  }, [member?.id, searchResult, trimmedKeyword]);

  const recordSearchClick = (product: ProductSearchResponse, index: number) => {
    void recordProductDiscoveryClick({
      memberId: member?.id,
      surface: "SEARCH",
      query: trimmedKeyword,
      productId: product.id,
      productName: product.name,
      recommendationSource: "ELASTICSEARCH",
      searchScore: product.searchScore,
      rankPosition: index + 1,
      highlights: product.highlights
    }).catch(() => undefined);
  };

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
        {items.map((product, index) => (
          <article className="catalog-card" key={product.id}>
            <ProductVisual brand={product.brand} className="catalog-card__poster" imageUrl={product.imageUrl} />
            <div className="catalog-card__meta">
              <p>{product.brand}</p>
              <h2>{product.name}</h2>
              <strong>{formatCurrency(product.price)}</strong>
              <small>Stock {product.stockQuantity}</small>
              {isSearchResult(product) && product.searchScore ? (
                <small>Score {product.searchScore.toFixed(2)}</small>
              ) : null}
              <p className="muted">{product.description}</p>
              {isSearchResult(product) && product.highlights.length ? (
                <div className="search-highlights">
                  {product.highlights.slice(0, 2).map((highlight) => (
                    <p key={highlight}>{renderHighlight(highlight)}</p>
                  ))}
                </div>
              ) : null}
            </div>
            <div className="catalog-card__actions">
              <Link
                className="button button--ghostDark"
                href={`/products/${product.id}`}
                onClick={() => {
                  if (trimmedKeyword && isSearchResult(product)) {
                    recordSearchClick(product, index);
                  }
                }}
              >
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

function isSearchResult(product: unknown): product is ProductSearchResponse {
  return typeof product === "object" && product !== null && "highlights" in product;
}

function renderHighlight(value: string) {
  return value.split(/(\[\[.*?\]\])/g).map((part, index) => {
    if (part.startsWith("[[") && part.endsWith("]]")) {
      return <mark key={`${part}-${index}`}>{part.slice(2, -2)}</mark>;
    }
    return <span key={`${part}-${index}`}>{part}</span>;
  });
}
