interface ProtocolTabsProps {
  selectedProtocol: "REST" | "GraphQL" | "WebSocket";
  onProtocolChange: (protocol: "REST" | "GraphQL" | "WebSocket") => void;
  onNewForm?: () => void; // 새 작성 폼을 위한 콜백
}

export function ProtocolTabs({
  selectedProtocol,
  onProtocolChange,
  onNewForm,
}: ProtocolTabsProps) {
  const protocols: Array<"REST" | "GraphQL" | "WebSocket"> = [
    "REST",
    "GraphQL",
    "WebSocket",
  ];

  const handleProtocolChange = (protocol: string) => {
    onProtocolChange(protocol as any);
    if (onNewForm) {
      onNewForm();
    }
  };

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
