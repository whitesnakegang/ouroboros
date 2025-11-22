import type { TextareaHTMLAttributes } from "react";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  className?: string;
}

export function Textarea({ className = "", ...props }: TextareaProps) {
  return (
    <textarea
      className={`w-full rounded-md bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] px-3 py-2 text-sm text-gray-900 dark:text-[#E6EDF3] placeholder:text-gray-400 dark:placeholder:text-[#8B949E] focus:outline-none focus:ring-0 focus-visible:outline-none resize-none ${className}`}
      {...props}
    />
  );
}

