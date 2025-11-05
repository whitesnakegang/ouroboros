import { useState } from "react";
import type { ReactNode } from "react";

interface EndpointGroupProps {
  groupName: string;
  defaultOpen?: boolean;
  children: ReactNode;
}

export function EndpointGroup({
  groupName,
  defaultOpen = true,
  children,
}: EndpointGroupProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-b border-gray-200 dark:border-[#2D333B]">
      {/* 그룹 헤더 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-4 py-2 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors"
      >
        <span className="text-xs font-semibold text-gray-600 dark:text-[#8B949E] uppercase tracking-wide">
          {groupName}
        </span>
        <svg
          className={`w-3.5 h-3.5 text-gray-500 dark:text-[#8B949E] transform transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M19 9l-7 7-7-7"
          />
        </svg>
      </button>

      {/* 그룹 내용 */}
      {isOpen && <div>{children}</div>}
    </div>
  );
}
