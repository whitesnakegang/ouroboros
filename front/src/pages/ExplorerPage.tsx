import { useState } from "react";
import { ProtocolTabs } from "@/features/spec/components/ProtocolTabs";
import { SpecToolbar } from "@/features/spec/components/SpecToolbar";
import { SpecForm } from "@/features/spec/components/SpecForm";
import { useSpecStore } from "@/features/spec/store/spec.store";

export function ExplorerPage() {
  const { protocol, setProtocol } = useSpecStore();
  const [activeTab, setActiveTab] = useState<"spec" | "test">("spec");

  return (
    <div className="h-full flex flex-col">
      {/* 상단 탭 */}
      <div className="border-b border-gray-200 dark:border-gray-700 px-6">
        <div className="flex gap-8">
          <button
            onClick={() => setActiveTab("spec")}
            className={`px-4 py-3 text-sm font-medium transition-colors ${
              activeTab === "spec"
                ? "text-gray-900 dark:text-white border-b-2 border-gray-900 dark:border-white"
                : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
            }`}
          >
            명세서
          </button>
          <button
            onClick={() => setActiveTab("test")}
            className={`px-4 py-3 text-sm font-medium transition-colors ${
              activeTab === "test"
                ? "text-gray-900 dark:text-white border-b-2 border-gray-900 dark:border-white"
                : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
            }`}
          >
            테스트
          </button>
        </div>
      </div>

      {/* 컨텐츠 */}
      <div className="flex-1 overflow-auto bg-gray-50 dark:bg-gray-900">
        {activeTab === "spec" ? (
          <div className="p-6">
            <ProtocolTabs
              selectedProtocol={protocol}
              onProtocolChange={setProtocol}
            />
            <SpecToolbar />
            <SpecForm protocol={protocol} />
          </div>
        ) : (
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            테스트 기능은 준비 중입니다.
          </div>
        )}
      </div>
    </div>
  );
}
