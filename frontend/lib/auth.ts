export function buildAuthPath(options?: { next?: string; mode?: "login" | "register" }) {
  const params = new URLSearchParams();

  if (options?.next) {
    params.set("next", options.next);
  }

  if (options?.mode && options.mode !== "login") {
    params.set("mode", options.mode);
  }

  const query = params.toString();
  return query ? `/login?${query}` : "/login";
}

export function sanitizeNextPath(nextPath: string | null | undefined) {
  if (!nextPath || !nextPath.startsWith("/")) {
    return "/";
  }

  if (nextPath.startsWith("//")) {
    return "/";
  }

  return nextPath;
}

export function formatNextLabel(nextPath: string) {
  if (nextPath.startsWith("/cart")) {
    return "cart";
  }

  if (nextPath.startsWith("/orders")) {
    return "orders";
  }

  if (nextPath.startsWith("/products")) {
    return "product page";
  }

  return "requested page";
}
