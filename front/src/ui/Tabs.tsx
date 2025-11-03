import { ReactNode } from "react";

interface TabsProps {
  tabs: Array<{ id: string; label: string }>;
  activeTab: string;
  onTabChange: (tabId: string) => void;
  className?: string;
}

export function Tabs({ tabs, activeTab, onTabChange, className = "" }: TabsProps) {
  return (
    <div className={`border-b border-gray-200 dark:border-[#2D333B] ${className}`}>
      <div className="flex gap-4">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
              activeTab === tab.id
                ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}

