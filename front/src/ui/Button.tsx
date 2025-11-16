import type { ButtonHTMLAttributes, ReactNode } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "outline" | "ghost";
  children: ReactNode;
  className?: string;
  fullWidth?: boolean; // 항상 가로 꽉 채우기
  responsiveFull?: boolean; // 모바일만 가로 꽉, md 이상 auto
}

export function Button({
  variant = "primary",
  children,
  className = "",
  fullWidth = false,
  responsiveFull = false,
  ...props
}: ButtonProps) {
  const widthClasses = fullWidth
    ? "w-full"
    : responsiveFull
    ? "w-full md:w-auto"
    : "w-auto";

  const baseStyles =
    [
      "inline-flex items-center justify-center",
      widthClasses,
      "rounded-md px-3 py-2 text-sm font-medium",
      "transition-all",
      "disabled:opacity-50 disabled:cursor-not-allowed",
      // Focus style: remove default outline, use soft ring only on keyboard focus
      "focus:outline-none",
      "focus-visible:ring-2 focus-visible:ring-blue-500",
      "focus-visible:ring-offset-2 focus-visible:ring-offset-white",
      "dark:focus-visible:ring-offset-[#0D1117]",
      "focus-visible:border-transparent",
      // Pressed feedback
      "active:translate-y-[1px]",
    ].join(" ");
  
  const variantStyles = {
    primary: "bg-[#2563EB] hover:bg-[#1E40AF] text-white",
    outline:
      "border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] bg-white dark:bg-[#0D1117] hover:bg-gray-50 dark:hover:bg-[#161B22]",
    ghost: "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3] bg-transparent",
  };

  return (
    <button
      className={`${baseStyles} ${variantStyles[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

