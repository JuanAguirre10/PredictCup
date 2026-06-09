import type { Metadata } from "next";
import { Manrope, Bricolage_Grotesque } from "next/font/google";
import { Toaster } from "sonner";
import Navbar from "@/components/Navbar";
import "./globals.css";

const sans = Manrope({ variable: "--font-sans", subsets: ["latin"] });
const heading = Bricolage_Grotesque({
  variable: "--font-heading",
  subsets: ["latin"],
  weight: ["500", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "PredictCup — Predice, Compite y Gana",
  description:
    "Plataforma de predicciones del Mundial 2026: predice marcadores, compite en salas y sube en el ranking en vivo.",
  icons: { icon: "/brand/logo-mark.png" },
};

// Aplica el tema guardado antes del primer pintado (evita parpadeo). Claro por defecto.
const temaScript = `try{if(localStorage.getItem('tema')==='dark')document.documentElement.classList.add('dark')}catch(e){}`;

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="es"
      suppressHydrationWarning
      className={`${sans.variable} ${heading.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col">
        <script dangerouslySetInnerHTML={{ __html: temaScript }} />
        <Navbar />
        <main className="flex-1">{children}</main>
        <Toaster theme="system" position="top-right" richColors />
      </body>
    </html>
  );
}
