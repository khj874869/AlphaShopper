import type {
  AddCartItemRequest,
  ApiErrorResponse,
  AuthResponse,
  CartResponse,
  CheckoutFailureReportRequest,
  CheckoutRequest,
  ConfirmCheckoutRequest,
  CouponResponse,
  LoginRequest,
  MemberResponse,
  OrderResponse,
  OrderSummaryResponse,
  PrepareCheckoutRequest,
  PrepareCheckoutResponse,
  ProductResponse,
  ProductSearchPageResponse,
  RegisterRequest,
  RefundRequest,
  UpdateDeliveryRequest
} from "@/lib/types";
import { API_BASE_URL } from "@/lib/runtime";

type ApiFetchOptions = RequestInit & {
  allowUnauthorized?: boolean;
};

async function apiFetch<T>(path: string, init?: ApiFetchOptions): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
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
    body: JSON.stringify(payload)
  });
}

export function register(payload: RegisterRequest) {
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function logout() {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST"
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
