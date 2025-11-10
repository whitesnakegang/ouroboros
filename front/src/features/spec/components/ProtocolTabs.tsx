interface ProtocolTabsProps {
  selectedProtocol: "REST" | "GraphQL" | "WebSocket" | null;
  onProtocolChange: (protocol: "REST" | "GraphQL" | "WebSocket" | null) => void;
  onNewForm?: () => void; // 새 작성 폼을 위한 콜백
  compact?: boolean; // 사이드바용 작은 사이즈
}

export function ProtocolTabs({
  selectedProtocol,
  onProtocolChange,
  onNewForm,
  compact = false,
}: ProtocolTabsProps) {
  const protocols: Array<"REST" | "GraphQL" | "WebSocket"> = [
    "REST",
    "WebSocket",
    "GraphQL",
  ];

  const handleProtocolChange = (protocol: "REST" | "GraphQL" | "WebSocket") => {
    // 토글 로직: 같은 프로토콜을 클릭하면 선택 해제 (null)
    if (selectedProtocol === protocol) {
      onProtocolChange(null);
    } else {
      onProtocolChange(protocol);
      if (onNewForm) {
        onNewForm();
      }
    }
  };

  if (compact) {
    // 사이드바용 작은 사이즈
    return (
      <div className="mt-3">
        <div className="flex gap-1 border-b border-gray-200 dark:border-[#2D333B]">
          {protocols.map((protocol) => (
            <button
              key={protocol}
              onClick={() => handleProtocolChange(protocol)}
              className={`px-3 py-1.5 text-xs font-medium transition-colors border-b-2 ${
                selectedProtocol === protocol
                  ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                  : "text-gray-600 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
              }`}
            >
              {protocol}
            </button>
          ))}
        </div>
      </div>
    );
  }

  // 기존 사이즈 (ApiEditorLayout용)
  return (
    <div className="mb-6 border-b border-gray-200 dark:border-[#2D333B]">
      <div className="flex gap-8">
        {protocols.map((protocol) => (
          <button
            key={protocol}
            onClick={() => handleProtocolChange(protocol)}
            className={`px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
              selectedProtocol === protocol
                ? "text-gray-900 dark:text-[#E6EDF3] border-[#2563EB]"
                : "text-gray-500 dark:text-[#8B949E] border-transparent hover:text-gray-900 dark:hover:text-[#E6EDF3]"
            }`}
          >
            {protocol}
          </button>
        ))}
      </div>
    </div>
  );
}
