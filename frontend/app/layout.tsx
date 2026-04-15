import type { Metadata } from "next";
import "./globals.css";
import { QueryProvider } from "@/components/query-provider";
import { AppHeader } from "@/components/app-header";
import { SessionSync } from "@/components/session-sync";

export const metadata: Metadata = {
  title: "AlphaShopper Web",
  description: "Musinsa x Zigzag inspired shopping web app for AlphaShopper"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <QueryProvider>
          <SessionSync />
          <div className="site-shell">
            <AppHeader />
            <main className="page-shell">{children}</main>
          </div>
        </QueryProvider>
      </body>
    </html>
  );
}
