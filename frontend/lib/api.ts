import type {
  AddCartItemRequest,
  ApiErrorResponse,
  AiChatRequest,
  AiChatResponse,
  AiInteractionLogFilters,
  AiInteractionLogResponse,
  AiInteractionReviewRequest,
  AiRecommendationRequest,
  AiRecommendationResponse,
  AiRecommendationSettingsResponse,
  AuthResponse,
  CartResponse,
  CheckoutFailureReportRequest,
  CheckoutRequest,
  ConfirmCheckoutRequest,
  CouponResponse,
  CsrfResponse,
  LoginRequest,
  MemberResponse,
  OrderResponse,
  OrderSummaryResponse,
  PrepareCheckoutRequest,
  PrepareCheckoutResponse,
  ProductDiscoveryClickLogFilters,
  ProductDiscoveryClickLogResponse,
  ProductDiscoveryClickRequest,
  ProductDiscoveryClickResponse,
  ProductDiscoveryFunnelSummaryResponse,
  ProductDiscoveryImpressionBatchRequest,
  ProductDiscoveryImpressionLogFilters,
  ProductDiscoveryImpressionLogResponse,
  ProductDiscoveryImpressionResponse,
  ProductResponse,
  ProductSearchPageResponse,
  RegisterRequest,
  RefundRequest,
  UpdateDeliveryRequest
} from "@/lib/types";
import { API_BASE_URL } from "@/lib/runtime";

type ApiFetchOptions = RequestInit & {
  allowUnauthorized?: boolean;
  skipCsrf?: boolean;
};

async function apiFetch<T>(path: string, init?: ApiFetchOptions): Promise<T> {
  const csrfHeaders = init?.skipCsrf || isSafeMethod(init?.method) ? {} : await getCsrfHeaders();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...csrfHeaders,
      ...(init?.headers ?? {})
    },
    cache: "no-store"
  });

  if (!response.ok) {
    if (init?.allowUnauthorized && response.status === 401) {
      return null as T;
    }

    const errorMessage = await extractErrorMessage(response);
    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function getAssetUrl(path: string | null | undefined) {
  if (!path) {
    return "/product-fallback.svg";
  }

  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  return `${API_BASE_URL}${path}`;
}

export function login(payload: LoginRequest) {
  return apiFetch<AuthResponse>("/api/auth/login", {
    method: "POST",
    skipCsrf: true,
    body: JSON.stringify(payload)
  });
}

export function register(payload: RegisterRequest) {
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    skipCsrf: true,
    body: JSON.stringify(payload)
  });
}

export function logout() {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    skipCsrf: true
  });
}

export function getSessionProfile() {
  return apiFetch<MemberResponse | null>("/api/auth/me", {
    allowUnauthorized: true
  });
}

export function getMembers() {
  return apiFetch<MemberResponse[]>("/api/members");
}

export function getProducts() {
  return apiFetch<ProductResponse[]>("/api/products");
}

export function getProduct(productId: number) {
  return apiFetch<ProductResponse>(`/api/products/${productId}`);
}

export function searchProducts(keyword: string) {
  const query = new URLSearchParams({
    keyword,
    page: "0",
    size: "12"
  });
  return apiFetch<ProductSearchPageResponse>(`/api/products/search?${query.toString()}`);
}

export function askShoppingAi(payload: AiChatRequest) {
  return apiFetch<AiChatResponse>("/api/ai/chat", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getAiRecommendations(payload: AiRecommendationRequest) {
  return apiFetch<AiRecommendationResponse>("/api/ai/recommendations", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function recordProductDiscoveryClick(payload: ProductDiscoveryClickRequest) {
  return apiFetch<ProductDiscoveryClickResponse>("/api/analytics/product-clicks", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function recordProductDiscoveryImpressions(payload: ProductDiscoveryImpressionBatchRequest) {
  return apiFetch<ProductDiscoveryImpressionResponse>("/api/analytics/product-impressions", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getAiInteractionLogs(limit = 50, filters: AiInteractionLogFilters = {}) {
  const query = new URLSearchParams({ limit: String(limit) });
  if (filters.recommendationSource) {
    query.set("recommendationSource", filters.recommendationSource);
  }
  if (filters.recommendationBucket) {
    query.set("recommendationBucket", filters.recommendationBucket);
  }
  if (filters.llmUsed !== undefined) {
    query.set("llmUsed", String(filters.llmUsed));
  }
  if (filters.reviewStatus) {
    query.set("reviewStatus", filters.reviewStatus);
  }
  return apiFetch<AiInteractionLogResponse[]>(`/api/admin/ai/interactions?${query.toString()}`);
}

export function reviewAiInteraction(interactionId: number, payload: AiInteractionReviewRequest) {
  return apiFetch<AiInteractionLogResponse>(`/api/admin/ai/interactions/${interactionId}/review`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function getAiRecommendationSettings() {
  return apiFetch<AiRecommendationSettingsResponse>("/api/admin/ai/recommendation-settings");
}

export function getProductDiscoveryClickLogs(limit = 100, filters: ProductDiscoveryClickLogFilters = {}) {
  const query = new URLSearchParams({ limit: String(limit) });
  if (filters.surface) {
    query.set("surface", filters.surface);
  }
  if (filters.recommendationSource) {
    query.set("recommendationSource", filters.recommendationSource);
  }
  if (filters.recommendationBucket) {
    query.set("recommendationBucket", filters.recommendationBucket);
  }
  if (filters.from) {
    query.set("from", filters.from);
  }
  if (filters.to) {
    query.set("to", filters.to);
  }
  return apiFetch<ProductDiscoveryClickLogResponse[]>(`/api/admin/ai/product-clicks?${query.toString()}`);
}

export function getProductDiscoveryFunnelSummary(filters: ProductDiscoveryClickLogFilters = {}) {
  const query = new URLSearchParams();
  if (filters.surface) {
    query.set("surface", filters.surface);
  }
  if (filters.recommendationSource) {
    query.set("recommendationSource", filters.recommendationSource);
  }
  if (filters.recommendationBucket) {
    query.set("recommendationBucket", filters.recommendationBucket);
  }
  if (filters.from) {
    query.set("from", filters.from);
  }
  if (filters.to) {
    query.set("to", filters.to);
  }
  return apiFetch<ProductDiscoveryFunnelSummaryResponse>(`/api/admin/ai/discovery-funnel?${query.toString()}`);
}

export function getProductDiscoveryImpressionLogs(limit = 100, filters: ProductDiscoveryImpressionLogFilters = {}) {
  const query = new URLSearchParams({ limit: String(limit) });
  if (filters.surface) {
    query.set("surface", filters.surface);
  }
  if (filters.recommendationSource) {
    query.set("recommendationSource", filters.recommendationSource);
  }
  if (filters.recommendationBucket) {
    query.set("recommendationBucket", filters.recommendationBucket);
  }
  if (filters.from) {
    query.set("from", filters.from);
  }
  if (filters.to) {
    query.set("to", filters.to);
  }
  return apiFetch<ProductDiscoveryImpressionLogResponse[]>(`/api/admin/ai/product-impressions?${query.toString()}`);
}

export function getCoupons() {
  return apiFetch<CouponResponse[]>("/api/coupons");
}

export function getCart(memberId: number) {
  return apiFetch<CartResponse>(`/api/members/${memberId}/cart`);
}

export function addCartItem(memberId: number, payload: AddCartItemRequest) {
  return apiFetch<CartResponse>(`/api/members/${memberId}/cart/items`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function removeCartItem(memberId: number, productId: number) {
  return apiFetch<CartResponse>(`/api/members/${memberId}/cart/items/${productId}`, {
    method: "DELETE"
  });
}

export function clearCart(memberId: number) {
  return apiFetch<CartResponse>(`/api/members/${memberId}/cart/items`, {
    method: "DELETE"
  });
}

export function checkout(payload: CheckoutRequest) {
  return apiFetch<OrderResponse>("/api/orders/checkout", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function prepareCheckout(payload: PrepareCheckoutRequest) {
  return apiFetch<PrepareCheckoutResponse>("/api/orders/checkout/prepare", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function confirmCheckout(payload: ConfirmCheckoutRequest) {
  return apiFetch<OrderResponse>("/api/orders/checkout/confirm", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function reportCheckoutFailure(payload: CheckoutFailureReportRequest) {
  return apiFetch<void>("/api/orders/checkout/fail", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getOrders(memberId: number) {
  return apiFetch<OrderSummaryResponse[]>(`/api/members/${memberId}/orders`);
}

export function refundOrder(orderId: number, payload: RefundRequest) {
  return apiFetch<OrderResponse>(`/api/orders/${orderId}/refund`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateDelivery(orderId: number, payload: UpdateDeliveryRequest) {
  return apiFetch<OrderResponse>(`/api/orders/${orderId}/delivery`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

async function extractErrorMessage(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    const payload = (await response.json().catch(() => null)) as ApiErrorResponse | null;
    if (payload?.message) {
      return payload.message;
    }

    if (payload?.error) {
      return payload.error;
    }
  }

  const errorText = await response.text().catch(() => "");
  return errorText || `Request failed: ${response.status}`;
}

async function getCsrfHeaders() {
  const csrf = await apiFetch<CsrfResponse>("/api/auth/csrf", {
    method: "GET",
    skipCsrf: true
  });
  return { [csrf.headerName]: csrf.token };
}

function isSafeMethod(method: string | undefined) {
  const normalized = method?.toUpperCase() ?? "GET";
  return normalized === "GET" || normalized === "HEAD" || normalized === "OPTIONS";
}
