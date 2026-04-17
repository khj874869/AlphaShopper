export type MemberResponse = {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
};

export type AuthResponse = {
  member: MemberResponse;
};

export type CsrfResponse = {
  headerName: string;
  token: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
};

export type ApiErrorResponse = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
};

export type ProductResponse = {
  id: number;
  name: string;
  brand: string;
  price: number;
  stockQuantity: number;
  description: string;
  imageUrl: string | null;
  active?: boolean;
};

export type ProductSearchResponse = {
  id: number;
  name: string;
  brand: string;
  price: number;
  stockQuantity: number;
  description: string;
  imageUrl: string | null;
};

export type ProductSearchPageResponse = {
  keyword: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  content: ProductSearchResponse[];
};

export type CouponResponse = {
  id: number;
  code: string;
  name: string;
};

export type AddCartItemRequest = {
  productId: number;
  quantity: number;
};

export type CartItemResponse = {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type CartResponse = {
  memberId: number;
  items: CartItemResponse[];
  totalAmount: number;
};

export type PaymentMethod = "CARD" | "KAKAO_PAY" | "NAVER_PAY" | "BANK_TRANSFER";

export type CheckoutRequest = {
  memberId: number;
  paymentMethod: PaymentMethod;
  paymentReference: string;
  shippingAddress: string;
  couponCode: string | null;
};

export type PrepareCheckoutRequest = {
  memberId: number;
  paymentMethod: PaymentMethod;
  shippingAddress: string;
  couponCode: string | null;
};

export type PrepareCheckoutResponse = {
  provider: string;
  providerOrderId: string;
  checkoutUrl: string;
  amount: number;
};

export type ConfirmCheckoutRequest = {
  memberId: number;
  providerOrderId: string;
  paymentKey: string;
  amount: number;
};

export type CheckoutFailureReportRequest = {
  memberId: number;
  providerOrderId: string;
  errorCode: string | null;
  errorMessage: string | null;
};

export type PaymentResponse = {
  method: PaymentMethod;
  status: "READY" | "APPROVED" | "FAILED" | "REFUNDED";
  amount: number;
  transactionKey: string | null;
  failedReason: string | null;
  requestedAt: string;
  approvedAt: string | null;
  refundedAt: string | null;
};

export type OrderResponse = {
  orderId: number;
  memberId: number;
  status: "CREATED" | "PAID" | "PAYMENT_FAILED" | "CANCELLED";
  deliveryStatus: "READY" | "PREPARING" | "SHIPPED" | "DELIVERED";
  orderedAt: string;
  shippingAddress: string;
  totalAmount: number;
  discountAmount: number;
  payAmount: number;
  couponCode: string | null;
  couponName: string | null;
  trackingNumber: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  payment: PaymentResponse;
};

export type OrderSummaryResponse = {
  orderId: number;
  status: "CREATED" | "PAID" | "PAYMENT_FAILED" | "CANCELLED";
  deliveryStatus: "READY" | "PREPARING" | "SHIPPED" | "DELIVERED";
  totalAmount: number;
  discountAmount: number;
  payAmount: number;
  orderedAt: string;
};

export type RefundRequest = {
  reason: string;
};

export type UpdateDeliveryRequest = {
  deliveryStatus: "READY" | "PREPARING" | "SHIPPED" | "DELIVERED";
  trackingNumber: string | null;
};
