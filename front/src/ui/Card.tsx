import { ReactNode } from "react";

interface CardProps {
  children: ReactNode;
  className?: string;
  title?: string;
  description?: string;
  icon?: ReactNode;
}

export function Card({ children, className = "", title, description, icon }: CardProps) {
  return (
    <div className={`rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm ${className}`}>
      {(title || description || icon) && (
        <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
          {icon && <div className="h-4 w-4 text-gray-500 dark:text-[#8B949E]">{icon}</div>}
          {title && <span>{title}</span>}
        </div>
      )}
      {description && (
        <p className="text-xs text-gray-600 dark:text-[#8B949E] mb-3">{description}</p>
      )}
      {children}
    </div>
  );
}

