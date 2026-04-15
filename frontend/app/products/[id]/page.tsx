import { ProductDetailView } from "@/components/product-detail-view";

export default function ProductDetailPage({
  params
}: {
  params: { id: string };
}) {
  return <ProductDetailView productId={Number(params.id)} />;
}
