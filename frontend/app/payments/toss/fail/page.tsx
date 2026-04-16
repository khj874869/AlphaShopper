import { Suspense } from "react";
import { TossPaymentFailView } from "@/components/toss-payment-fail-view";

export default function TossPaymentFailPage() {
  return (
    <Suspense fallback={<div className="panel">Loading payment result...</div>}>
      <TossPaymentFailView />
    </Suspense>
  );
}
