"use client";

import { getAssetUrl } from "@/lib/api";

export function ProductVisual({
  imageUrl,
  brand,
  className
}: {
  imageUrl: string | null | undefined;
  brand: string;
  className: string;
}) {
  return (
    <div className={className}>
      <img alt={brand} src={getAssetUrl(imageUrl)} />
      <span>{brand}</span>
    </div>
  );
}
