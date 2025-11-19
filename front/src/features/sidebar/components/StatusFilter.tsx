import { useState } from "react";

interface StatusFilterProps {
  activeFilter: "mock" | "completed" | "all";
  onFilterChange: (filter: "mock" | "completed" | "all") => void;
}

export function StatusFilter({
  activeFilter,
  onFilterChange,
}: StatusFilterProps) {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className="bg-gray-50 dark:bg-[#0D1117] border-b border-gray-200 dark:border-[#2D333B] -mx-4 px-4 pt-2">
      <div className="flex gap-0.5 -mb-px">
        <button
          onClick={() => onFilterChange("all")}
          className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
            activeFilter === "all"
              ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#0D1117] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#0D1117] relative z-10"
              : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
          }`}
        >
          All
        </button>
        <div className="relative group">
          <button
            onClick={() => onFilterChange("mock")}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
              activeFilter === "mock"
                ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#0D1117] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#0D1117] relative z-10"
                : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
            }`}
          >
            Mock
          </button>
          {/* 툴팁 */}
          <div
            className={`absolute left-0 bottom-full mb-2 ${
              isFocused ? "block" : "hidden group-hover:block"
            } z-50 pointer-events-none`}
          >
            <div className="bg-white dark:bg-[#161B22] border border-gray-300 dark:border-[#2D333B] rounded-md px-3 py-2 shadow-lg min-w-[200px]">
              <div className="text-xs font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2">
                Badge Status
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-[#b6bdca] flex-shrink-0"></div>
                  <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                    Gray: Not Implemented
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-[#EAB308] flex-shrink-0"></div>
                  <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                    Yellow: In Progress
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-[#F97316] flex-shrink-0"></div>
                  <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                    Orange: Modifying
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-[#25eb64] flex-shrink-0"></div>
                  <span className="text-xs text-gray-600 dark:text-[#8B949E]">
                    Green: Completed
                  </span>
                </div>
              </div>
            </div>
            {/* 화살표 */}
            <div className="absolute left-4 top-full w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-300 dark:border-t-[#2D333B]"></div>
          </div>
        </div>
        <button
          onClick={() => onFilterChange("completed")}
          className={`px-4 py-2 text-sm font-medium transition-all rounded-t-md rounded-b-none border border-b-0 focus:outline-none focus-visible:outline-none ${
            activeFilter === "completed"
              ? "text-gray-900 dark:text-[#E6EDF3] bg-white dark:bg-[#0D1117] border-gray-200 dark:border-[#2D333B] border-b-white dark:border-b-[#0D1117] relative z-10"
              : "text-gray-500 dark:text-[#8B949E] bg-transparent border-transparent hover:text-gray-700 dark:hover:text-[#C9D1D9] hover:bg-gray-100 dark:hover:bg-[#21262D]"
          }`}
        >
          Completed
        </button>
      </div>
    </div>
  );
}
