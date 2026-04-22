import { LoginPanel } from "@/components/login-panel";

export default async function LoginPage({
  searchParams
}: {
  searchParams?: Promise<{
    next?: string;
    mode?: string;
  }>;
}) {
  const params = await searchParams;

  return (
    <LoginPanel
      key={`${params?.mode ?? "login"}:${params?.next ?? ""}`}
      initialNext={params?.next}
      initialMode={params?.mode === "register" ? "register" : "login"}
    />
  );
}
