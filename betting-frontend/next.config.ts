import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Salida autocontenida para una imagen Docker minima (server.js + node_modules justos).
  output: "standalone",
  images: {
    // Avatares de Google (OAuth). En Next 16 `domains` esta deprecado: se usa remotePatterns.
    remotePatterns: [
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com",
      },
    ],
  },
};

export default nextConfig;
