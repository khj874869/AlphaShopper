"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { login, register } from "@/lib/api";
import { buildAuthPath, formatNextLabel, sanitizeNextPath } from "@/lib/auth";
import { DEMO_ACCOUNTS_ENABLED } from "@/lib/runtime";
import { useSessionStore } from "@/store/session-store";

const demoAccounts = [
  {
    label: "Buyer A",
    email: "buyer1@zigzag.local",
    password: "buyer1234",
    note: "Standard shopper flow"
  },
  {
    label: "Buyer B",
    email: "buyer2@zigzag.local",
    password: "buyer1234",
    note: "Fresh account baseline"
  },
  {
    label: "Admin",
    email: "admin@alphashopper.local",
    password: "admin1234",
    note: "Ops and catalog control"
  }
] as const;

type AuthMode = "login" | "register";

export function LoginPanel({
  initialNext,
  initialMode
}: {
  initialNext?: string;
  initialMode?: AuthMode;
}) {
  const router = useRouter();
  const member = useSessionStore((state) => state.member);
  const signIn = useSessionStore((state) => state.signIn);
  const nextPath = sanitizeNextPath(initialNext);
  const targetLabel = formatNextLabel(nextPath);
  const resolvedMode = initialMode === "register" ? "register" : "login";
  const initialDemoAccount = DEMO_ACCOUNTS_ENABLED ? demoAccounts[0] : null;

  const [mode, setMode] = useState<AuthMode>(resolvedMode);
  const [email, setEmail] = useState<string>(initialDemoAccount?.email ?? "");
  const [password, setPassword] = useState<string>(initialDemoAccount?.password ?? "");
  const [registerName, setRegisterName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    if (member) {
      router.replace(nextPath);
    }
  }, [member, nextPath, router]);

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      signIn(response.member);
      router.push(nextPath);
      router.refresh();
    }
  });

  const registerMutation = useMutation({
    mutationFn: register,
    onSuccess: (response) => {
      signIn(response.member);
      router.push(nextPath);
      router.refresh();
    }
  });

  const registerHint = useMemo(() => {
    if (!registerPassword) {
      return "Use at least 8 characters with letters and numbers.";
    }

    if (registerPassword.length < 8) {
      return "Password must be at least 8 characters.";
    }

    if (confirmPassword && registerPassword !== confirmPassword) {
      return "Passwords do not match yet.";
    }

    return "Account will be created as a shopper and signed in immediately.";
  }, [confirmPassword, registerPassword]);

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setLocalError(null);
    loginMutation.reset();
    registerMutation.reset();
  };

  const handleLoginSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLocalError(null);
    loginMutation.reset();

    if (!email.trim() || !password.trim()) {
      setLocalError("Enter both email and password.");
      return;
    }

    loginMutation.mutate({
      email: email.trim(),
      password
    });
  };

  const handleRegisterSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLocalError(null);
    registerMutation.reset();

    const validationError = validateRegisterForm({
      name: registerName,
      email: registerEmail,
      password: registerPassword,
      confirmPassword
    });

    if (validationError) {
      setLocalError(validationError);
      return;
    }

    registerMutation.mutate({
      name: registerName.trim(),
      email: registerEmail.trim(),
      password: registerPassword
    });
  };

  return (
    <div className="auth-shell">
      <section className="auth-showcase">
        <div className="auth-showcase__copy">
          <p className="eyebrow">AlphaShopper account</p>
          <h1>Login, signup, cart and checkout now move as one clean flow.</h1>
          <p>
            Session auth now uses a secure server cookie, while signup still validates common mistakes before submit and
            shows the errors in plain language.
          </p>
        </div>

        {DEMO_ACCOUNTS_ENABLED ? (
          <div className="auth-demo-grid">
            {demoAccounts.map((account) => (
              <button
                key={account.email}
                className="auth-demo-card"
                onClick={() => {
                  switchMode("login");
                  setEmail(account.email);
                  setPassword(account.password);
                }}
                type="button"
              >
                <span>{account.label}</span>
                <strong>{account.email}</strong>
                <small>{account.note}</small>
              </button>
            ))}
          </div>
        ) : null}

        {DEMO_ACCOUNTS_ENABLED ? (
          <div className="auth-note">
            <strong>Demo passwords</strong>
            <p>
              Buyer accounts use <code>buyer1234</code>. Admin uses <code>admin1234</code>.
            </p>
          </div>
        ) : null}

        {nextPath !== "/" ? (
          <div className="auth-note auth-note--intent">
            <strong>After sign-in</strong>
            <p>You will return directly to the {targetLabel}.</p>
          </div>
        ) : null}
      </section>

      <section className="panel auth-form-panel">
        <div className="auth-switch" role="tablist" aria-label="Authentication mode">
          <button
            className={mode === "login" ? "auth-switch__item auth-switch__item--active" : "auth-switch__item"}
            onClick={() => switchMode("login")}
            type="button"
          >
            Login
          </button>
          <button
            className={mode === "register" ? "auth-switch__item auth-switch__item--active" : "auth-switch__item"}
            onClick={() => switchMode("register")}
            type="button"
          >
            Register
          </button>
        </div>

        {mode === "login" ? (
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <div className="panel-head">
              <div>
                <p className="eyebrow">Secure access</p>
                <h2>Sign in and continue shopping</h2>
              </div>
            </div>

            <label className="field">
              <span>Email</span>
              <input
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Password</span>
              <input
                autoComplete="current-password"
                placeholder="Enter your password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>

            <button className="button button--dark button--block" disabled={loginMutation.isPending} type="submit">
              {loginMutation.isPending ? "Signing in..." : "Login"}
            </button>

            <p className="form-helper">
              {DEMO_ACCOUNTS_ENABLED
                ? "Use the demo cards on the left for instant account fill-in."
                : "Use your shopper account email and password."}
            </p>
            {nextPath !== "/" ? (
              <p className="form-helper">
                After login you will move to the {targetLabel}. Need a new account instead?{" "}
                <button className="inline-switch" onClick={() => switchMode("register")} type="button">
                  Open signup
                </button>
              </p>
            ) : null}
            {localError && mode === "login" ? <p className="form-error">{localError}</p> : null}
            {loginMutation.error ? <p className="form-error">{getErrorMessage(loginMutation.error)}</p> : null}
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleRegisterSubmit}>
            <div className="panel-head">
              <div>
                <p className="eyebrow">Quick signup</p>
                <h2>Create a shopper account</h2>
              </div>
            </div>

            <label className="field">
              <span>Name</span>
              <input
                autoComplete="name"
                placeholder="Your shopper name"
                value={registerName}
                onChange={(event) => setRegisterName(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Email</span>
              <input
                autoComplete="email"
                placeholder="you@alphashopper.local"
                value={registerEmail}
                onChange={(event) => setRegisterEmail(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Password</span>
              <input
                autoComplete="new-password"
                placeholder="At least 8 characters"
                type="password"
                value={registerPassword}
                onChange={(event) => setRegisterPassword(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Confirm password</span>
              <input
                autoComplete="new-password"
                placeholder="Enter the same password again"
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </label>

            <p className="form-helper">{registerHint}</p>

            <button className="button button--dark button--block" disabled={registerMutation.isPending} type="submit">
              {registerMutation.isPending ? "Creating account..." : "Create account"}
            </button>

            <div className="auth-note auth-note--compact">
              <strong>After signup</strong>
              <p>You are signed in immediately and routed back to the storefront as a `USER` member.</p>
            </div>

            {nextPath !== "/" ? (
              <p className="form-helper">
                After account creation you will continue to the {targetLabel}. Already have an account?{" "}
                <button className="inline-switch" onClick={() => switchMode("login")} type="button">
                  Login instead
                </button>
              </p>
            ) : null}

            {localError && mode === "register" ? <p className="form-error">{localError}</p> : null}
            {registerMutation.error ? <p className="form-error">{getErrorMessage(registerMutation.error)}</p> : null}
          </form>
        )}

        <div className="auth-footer">
          <span>Need the catalog first?</span>
          <Link href="/products">Browse products</Link>
        </div>

        {!member && nextPath === "/" ? (
          <div className="auth-footer auth-footer--soft">
            <span>Just exploring?</span>
            <Link href={buildAuthPath({ mode: "register" })}>Open signup</Link>
          </div>
        ) : null}
      </section>
    </div>
  );
}

function validateRegisterForm(input: {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}) {
  if (!input.name.trim()) {
    return "Enter your name.";
  }

  if (!input.email.trim()) {
    return "Enter your email.";
  }

  if (!input.email.includes("@")) {
    return "Use a valid email format.";
  }

  if (input.password.length < 8) {
    return "Password must be at least 8 characters.";
  }

  if (input.password !== input.confirmPassword) {
    return "Password confirmation does not match.";
  }

  return null;
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }

  return "Request failed.";
}
