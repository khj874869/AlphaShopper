"use client";

import Image from "next/image";
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
      <Image alt={brand} fill sizes="(max-width: 768px) 100vw, 33vw" src={getAssetUrl(imageUrl)} unoptimized />
      <span>{brand}</span>
    </div>
  );
}
