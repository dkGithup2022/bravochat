import type { NextConfig } from "next";

// 빌드 타임 env 검증
import "./src/env.mjs";

const nextConfig: NextConfig = {
  experimental: {
    optimizePackageImports: ["lucide-react"],
  },
};

export default nextConfig;
