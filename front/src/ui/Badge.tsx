import type { ReactNode } from "react";

interface BadgeProps {
  children: ReactNode;
  variant?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "default";
  className?: string;
}

const methodColors = {
  GET: "text-[#10B981]",
  POST: "text-[#2563EB]",
  PUT: "text-[#F59E0B]",
  PATCH: "text-[#F59E0B]",
  DELETE: "text-red-500",
  default: "text-[#8B949E]",
};

export function Badge({ children, variant = "default", className = "" }: BadgeProps) {
  const colorClass = methodColors[variant] || methodColors.default;
  
  return (
    <span
      className={`inline-flex items-center rounded-[4px] border border-gray-300 dark:border-[#2D333B] bg-white dark:bg-[#0D1117] px-2 py-[2px] text-[10px] font-mono font-semibold ${colorClass} ${className}`}
    >
      {children}
    </span>
  );
}

