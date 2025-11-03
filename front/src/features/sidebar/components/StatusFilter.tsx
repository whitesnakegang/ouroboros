import { useState } from "react";

interface StatusFilterProps {
  activeFilter: "mock" | "completed";
  onFilterChange: (filter: "mock" | "completed") => void;
}

export function StatusFilter({
  activeFilter,
  onFilterChange,
}: StatusFilterProps) {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className="flex gap-1 border-b border-[#2D333B]">
      <div className="relative group">
        <button
          onClick={() => onFilterChange("mock")}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
            activeFilter === "mock"
              ? "text-[#E6EDF3] border-[#2563EB]"
              : "text-[#8B949E] border-transparent hover:text-[#E6EDF3]"
          }`}
        >
          Mock
        </button>
        {/* 툴팁 */}
        <div className={`absolute left-0 bottom-full mb-2 ${isFocused ? "block" : "hidden group-hover:block"} z-50 pointer-events-none`}>
          <div className="bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md px-3 py-2 shadow-lg min-w-[200px]">
            <div className="text-xs font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
              뱃지 색상별 상태
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 rounded-full bg-[#8B949E] flex-shrink-0"></div>
                <span className="text-xs text-gray-600 dark:text-[#8B949E]">회색: 미구현</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 rounded-full bg-blue-500 flex-shrink-0"></div>
                <span className="text-xs text-gray-600 dark:text-[#8B949E]">파란색: 구현중</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 rounded-full bg-orange-500 flex-shrink-0"></div>
                <span className="text-xs text-gray-600 dark:text-[#8B949E]">주황색: 수정중</span>
              </div>
            </div>
          </div>
          {/* 화살표 */}
          <div className="absolute left-4 top-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-300 dark:border-t-[#2D333B]"></div>
        </div>
      </div>
      <button
        onClick={() => onFilterChange("completed")}
        className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
          activeFilter === "completed"
            ? "text-[#E6EDF3] border-[#2563EB]"
            : "text-[#8B949E] border-transparent hover:text-[#E6EDF3]"
        }`}
      >
        Completed
      </button>
    </div>
  );
}
