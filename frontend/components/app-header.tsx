"use client";

import Link from "next/link";
import { useSessionStore } from "@/store/session-store";

const navItems = [
  { href: "/", label: "Home" },
  { href: "/products", label: "Explore" },
  { href: "/cart", label: "Bag" },
  { href: "/orders", label: "Orders" }
];

export function AppHeader() {
  const member = useSessionStore((state) => state.member);
  const signOut = useSessionStore((state) => state.signOut);

  return (
    <header className="app-header">
      <div className="brand-lockup">
        <Link className="brand-mark" href="/">
          <span className="brand-mark__badge">AS</span>
          <div>
            <strong>AlphaShopper</strong>
            <p>Musinsa editorial meets Zigzag speed</p>
          </div>
        </Link>
        <nav className="main-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <Link key={item.href} href={item.href}>
              {item.label}
            </Link>
          ))}
        </nav>
      </div>
      <div className="auth-bar">
        {member ? (
          <>
            <div className="auth-badge">
              <span>{member.role}</span>
              <strong>{member.name}</strong>
            </div>
            <button className="button button--ghostDark" onClick={signOut}>
              Logout
            </button>
          </>
        ) : (
          <Link className="button button--dark" href="/login">
            Login
          </Link>
        )}
      </div>
    </header>
  );
}
