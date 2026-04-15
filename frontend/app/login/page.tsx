import { LoginPanel } from "@/components/login-panel";

export default function LoginPage({
  searchParams
}: {
  searchParams?: {
    next?: string;
    mode?: string;
  };
}) {
  return (
    <LoginPanel
      initialNext={searchParams?.next}
      initialMode={searchParams?.mode === "register" ? "register" : "login"}
    />
  );
}
