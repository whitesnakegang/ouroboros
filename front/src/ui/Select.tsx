import type { SelectHTMLAttributes } from "react";

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  className?: string;
}

export function Select({ className = "", children, ...props }: SelectProps) {
  return (
    <select
      className={`w-full rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] px-3 py-2 text-sm text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-0 focus-visible:outline-none ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}

