import Image from "next/image";
import { cn } from "@/lib/utils";

/** 상단 큰 마스코트 (bob 애니메이션). height 로 크기 조절 (기본 150px). */
export function Mascot({
  className,
  height = 150,
}: {
  className?: string;
  height?: number;
}) {
  const width = Math.round(height * 0.75);
  return (
    <Image
      src="/assets/mascot-clear.png"
      alt="브라보"
      width={width}
      height={height}
      priority
      style={{ height, width: "auto" }}
      className={cn("animate-bob", className)}
    />
  );
}
