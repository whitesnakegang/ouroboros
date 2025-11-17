interface ProtocolTabsProps {
  selectedProtocol: "REST" | "WebSocket" | null;
  onProtocolChange: (protocol: "REST" | "WebSocket" | null) => void;
  onNewForm?: () => void; // 새 작성 폼을 위한 콜백
  compact?: boolean; // 사이드바용 작은 사이즈
}

export function ProtocolTabs({
  selectedProtocol,
  onProtocolChange,
  onNewForm,
  compact = false,
}: ProtocolTabsProps) {
  const protocols: Array<"REST" | "WebSocket"> = [
    "REST",
    "WebSocket",
  ];

  const handleProtocolChange = (protocol: "REST" | "WebSocket") => {
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
      <div className="mb-3">
        <div className="flex gap-2 p-1 bg-gray-100 dark:bg-[#161B22] rounded-lg">
          {protocols.map((protocol) => (
            <button
              key={protocol}
              onClick={() => handleProtocolChange(protocol)}
              className={`flex-1 py-2 px-3 text-xs font-medium rounded-md transition-colors focus:outline-none focus-visible:outline-none ${
                selectedProtocol === protocol
                  ? "bg-white dark:bg-[#0D1117] text-[#2563EB] shadow-sm"
                  : "text-gray-600 dark:text-[#8B949E] hover:text-gray-900 dark:hover:text-[#E6EDF3]"
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
            className={`px-4 py-3 text-sm font-medium transition-colors border-b-2 focus:outline-none focus-visible:outline-none ${
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
