import { Suspense } from "react";
import { TossPaymentSuccessView } from "@/components/toss-payment-success-view";

export default function TossPaymentSuccessPage() {
  return (
    <Suspense fallback={<div className="panel">Loading payment result...</div>}>
      <TossPaymentSuccessView />
    </Suspense>
  );
}
