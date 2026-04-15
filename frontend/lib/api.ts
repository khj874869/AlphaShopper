import type {
  AddCartItemRequest,
  AuthResponse,
  CartResponse,
  CheckoutRequest,
  CouponResponse,
  LoginRequest,
  MemberResponse,
  OrderResponse,
  OrderSummaryResponse,
  ProductResponse,
  ProductSearchPageResponse,
  RefundRequest,
  UpdateDeliveryRequest
} from "@/lib/types";
import { useSessionStore } from "@/store/session-store";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const accessToken = useSessionStore.getState().accessToken;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(init?.headers ?? {})
    },
    cache: "no-store"
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Request failed: ${response.status}`);
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

export function register(payload: { name: string; email: string; password: string }) {
  return apiFetch<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getMyProfile() {
  return apiFetch<MemberResponse>("/api/auth/me");
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
