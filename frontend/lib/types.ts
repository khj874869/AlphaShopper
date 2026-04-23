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
  searchScore: number | null;
  highlights: string[];
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

export type AiRecommendationSource = "ELASTICSEARCH" | "DATABASE";

export type AiRecommendationBucket = "DEFAULT" | "CONTROL" | "CTR_RANKING";

export type AiProductRecommendationResponse = {
  product: ProductResponse;
  reason: string;
  searchScore: number | null;
  highlights: string[];
};

export type AiChatRequest = {
  message: string;
  memberId?: number;
  maxRecommendations?: number;
};

export type AiChatResponse = {
  reply: string;
  llmUsed: boolean;
  recommendationSource: AiRecommendationSource;
  recommendationBucket: AiRecommendationBucket;
  recommendations: AiProductRecommendationResponse[];
};

export type AiRecommendationRequest = {
  prompt: string;
  memberId?: number;
  maxResults?: number;
};

export type AiRecommendationResponse = {
  query: string;
  recommendationSource: AiRecommendationSource;
  recommendationBucket: AiRecommendationBucket;
  recommendations: AiProductRecommendationResponse[];
};

export type AiInteractionLogResponse = {
  id: number;
  memberId: number | null;
  interactionType: "CHAT" | "RECOMMENDATION";
  prompt: string;
  reply: string | null;
  llmUsed: boolean;
  recommendationCount: number;
  recommendationSource: "ELASTICSEARCH" | "DATABASE";
  recommendationBucket: AiRecommendationBucket | null;
  recommendedProductIds: string | null;
  requestedAt: string;
  qualityScore: number | null;
  qualityNote: string | null;
  reviewedAt: string | null;
  reviewedByMemberId: number | null;
};

export type AiInteractionReviewStatus = "REVIEWED" | "UNREVIEWED" | "LOW_SCORE";

export type AiInteractionLogFilters = {
  recommendationSource?: AiRecommendationSource;
  recommendationBucket?: AiRecommendationBucket;
  llmUsed?: boolean;
  reviewStatus?: AiInteractionReviewStatus;
};

export type AiInteractionReviewRequest = {
  qualityScore: number;
  qualityNote: string | null;
};

export type AiRecommendationSettingsResponse = {
  clickSignalEnabled: boolean;
  clickSignalWindowDays: number;
  clickSignalBoostPerClick: number;
  clickSignalMaxClickBoost: number;
  ctrSignalEnabled: boolean;
  ctrSignalMinImpressions: number;
  ctrSignalHighThreshold: number;
  ctrSignalHighBoost: number;
  ctrSignalMidThreshold: number;
  ctrSignalMidBoost: number;
  ctrSignalLowThreshold: number;
  ctrSignalLowPenalty: number;
  ctrSignalLowAction: "PENALIZE" | "EXCLUDE";
  experimentEnabled: boolean;
  ctrTreatmentPercent: number;
};

export type ProductDiscoverySurface = "SEARCH" | "AI_RECOMMENDATION";

export type ProductDiscoveryClickRequest = {
  memberId?: number;
  surface: ProductDiscoverySurface;
  query: string;
  productId: number;
  productName: string;
  recommendationSource?: AiRecommendationSource;
  recommendationBucket?: AiRecommendationBucket;
  searchScore?: number | null;
  rankPosition?: number;
  highlights?: string[];
};

export type ProductDiscoveryClickResponse = {
  id: number;
  clickedAt: string;
};

export type ProductDiscoveryClickLogResponse = {
  id: number;
  memberId: number | null;
  surface: ProductDiscoverySurface;
  query: string;
  productId: number;
  productName: string;
  recommendationSource: AiRecommendationSource | null;
  recommendationBucket: AiRecommendationBucket | null;
  searchScore: number | null;
  rankPosition: number | null;
  highlights: string | null;
  clickedAt: string;
};

export type ProductDiscoveryClickLogFilters = {
  surface?: ProductDiscoverySurface;
  recommendationSource?: AiRecommendationSource;
  recommendationBucket?: AiRecommendationBucket;
  from?: string;
  to?: string;
};

export type ProductDiscoveryFunnelSegmentResponse = {
  key: string;
  impressions: number;
  clicks: number;
  ctr: number;
};

export type ProductDiscoveryProductFunnelResponse = {
  productId: number;
  productName: string;
  impressions: number;
  clicks: number;
  ctr: number;
  averageRank: number | null;
};

export type ProductDiscoveryFunnelTrendResponse = {
  date: string;
  impressions: number;
  clicks: number;
  ctr: number;
};

export type ProductDiscoveryFunnelSummaryResponse = {
  from: string | null;
  to: string | null;
  impressions: number;
  clicks: number;
  ctr: number;
  surfaces: ProductDiscoveryFunnelSegmentResponse[];
  sources: ProductDiscoveryFunnelSegmentResponse[];
  buckets: ProductDiscoveryFunnelSegmentResponse[];
  topProducts: ProductDiscoveryProductFunnelResponse[];
  dailyTrend: ProductDiscoveryFunnelTrendResponse[];
};

export type ProductDiscoveryImpressionRequest = {
  memberId?: number;
  surface: ProductDiscoverySurface;
  query: string;
  productId: number;
  productName: string;
  recommendationSource?: AiRecommendationSource;
  recommendationBucket?: AiRecommendationBucket;
  searchScore?: number | null;
  rankPosition: number;
  highlights?: string[];
  impressionKey?: string;
};

export type ProductDiscoveryImpressionBatchRequest = {
  impressions: ProductDiscoveryImpressionRequest[];
};

export type ProductDiscoveryImpressionResponse = {
  recordedCount: number;
  recordedAt: string;
};

export type ProductDiscoveryImpressionLogResponse = {
  id: number;
  memberId: number | null;
  surface: ProductDiscoverySurface;
  query: string;
  productId: number;
  productName: string;
  recommendationSource: AiRecommendationSource | null;
  recommendationBucket: AiRecommendationBucket | null;
  searchScore: number | null;
  rankPosition: number;
  highlights: string | null;
  impressionKey: string | null;
  shownAt: string;
};

export type ProductDiscoveryImpressionLogFilters = {
  surface?: ProductDiscoverySurface;
  recommendationSource?: AiRecommendationSource;
  recommendationBucket?: AiRecommendationBucket;
  from?: string;
  to?: string;
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
