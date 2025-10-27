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
    <div className="border-b border-gray-200 dark:border-gray-700">
      {/* 그룹 헤더 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-4 py-3 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
      >
        <span className="font-semibold text-gray-700 dark:text-gray-200">
          {groupName}
        </span>
        <svg
          className={`w-4 h-4 text-gray-500 dark:text-gray-400 transform transition-transform ${
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
      {isOpen && <div className="bg-gray-50 dark:bg-gray-800">{children}</div>}
    </div>
  );
}
