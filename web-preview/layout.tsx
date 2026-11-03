import type { Metadata } from "next";
import { Vazirmatn } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const vazir = Vazirmatn({
  variable: "--font-vazir",
  subsets: ["arabic", "latin"],
});

export const metadata: Metadata = {
  title: "پیش‌نمایش پیشنهادها — همراه درمان اعتیاد",
  description:
    "پیش‌نمایش پیشنهادهای نسخهٔ بعد: جای آیکون «آزمون»، حالت‌های آزمون، اصلاح ترامادول و گزارش تطبیق ۴ سند PDF",
  icons: {
    icon: "/hamrah-icon-128.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fa" dir="rtl" suppressHydrationWarning>
      <body
        className={`${vazir.variable} antialiased bg-background text-foreground`}
        style={{ fontFamily: "var(--font-vazir), Tahoma, sans-serif" }}
      >
        {children}
        <Toaster />
      </body>
    </html>
  );
}
