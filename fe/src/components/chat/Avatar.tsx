import Image from "next/image";

/** 봇 메시지 좌측 원형 마스코트 아바타 (40px, Bravo Message 정본). */
export function Avatar() {
  return (
    <div className="flex h-10 w-10 flex-none items-center justify-center overflow-hidden rounded-full bg-[#0d1117]">
      <Image
        src="/assets/mascot-clear.png"
        alt="브라보"
        width={38}
        height={38}
        className="h-[38px] w-[38px] object-contain"
      />
    </div>
  );
}
