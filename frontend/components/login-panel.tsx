"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { login, register } from "@/lib/api";
import { useSessionStore } from "@/store/session-store";

const demoAccounts = [
  { label: "Buyer A", email: "buyer1@zigzag.local", password: "buyer1234" },
  { label: "Buyer B", email: "buyer2@zigzag.local", password: "buyer1234" },
  { label: "Admin", email: "admin@alphashopper.local", password: "admin1234" }
];

export function LoginPanel() {
  const router = useRouter();
  const signIn = useSessionStore((state) => state.signIn);

  const [email, setEmail] = useState(demoAccounts[0].email);
  const [password, setPassword] = useState(demoAccounts[0].password);
  const [registerName, setRegisterName] = useState("New Shopper");
  const [registerEmail, setRegisterEmail] = useState("new@alphashopper.local");
  const [registerPassword, setRegisterPassword] = useState("newpass1234");

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      signIn(response.accessToken, response.member);
      router.push("/");
      router.refresh();
    }
  });

  const registerMutation = useMutation({
    mutationFn: register,
    onSuccess: (response) => {
      signIn(response.accessToken, response.member);
      router.push("/");
      router.refresh();
    }
  });

  return (
    <div className="auth-layout">
      <section className="panel auth-panel auth-panel--dark">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Secure access</p>
            <h1>Login with JWT and use protected shopping APIs.</h1>
          </div>
        </div>

        <div className="chip-row">
          {demoAccounts.map((account) => (
            <button
              key={account.email}
              className="chip"
              onClick={() => {
                setEmail(account.email);
                setPassword(account.password);
              }}
            >
              {account.label}
            </button>
          ))}
        </div>

        <label className="field">
          <span>Email</span>
          <input value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>

        <button
          className="button button--light button--block"
          disabled={loginMutation.isPending}
          onClick={() => loginMutation.mutate({ email, password })}
        >
          Login
        </button>

        {loginMutation.error ? <p className="form-error">{getErrorMessage(loginMutation.error)}</p> : null}
      </section>

      <section className="panel auth-panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Quick signup</p>
            <h2>Create a shopper account and sign in immediately.</h2>
          </div>
        </div>

        <label className="field">
          <span>Name</span>
          <input value={registerName} onChange={(event) => setRegisterName(event.target.value)} />
        </label>
        <label className="field">
          <span>Email</span>
          <input value={registerEmail} onChange={(event) => setRegisterEmail(event.target.value)} />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={registerPassword} onChange={(event) => setRegisterPassword(event.target.value)} />
        </label>

        <button
          className="button button--dark button--block"
          disabled={registerMutation.isPending}
          onClick={() =>
            registerMutation.mutate({
              name: registerName,
              email: registerEmail,
              password: registerPassword
            })
          }
        >
          Register
        </button>

        <p className="muted">
          Demo accounts:
          <br />
          buyer1@zigzag.local / buyer1234
          <br />
          admin@alphashopper.local / admin1234
        </p>

        {registerMutation.error ? <p className="form-error">{getErrorMessage(registerMutation.error)}</p> : null}
      </section>
    </div>
  );
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }

  return "Request failed.";
}
