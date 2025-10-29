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
    <div className="mb-6 border-b border-gray-200 dark:border-gray-700">
      <div className="flex gap-8">
        {protocols.map((protocol) => (
          <button
            key={protocol}
            onClick={() => handleProtocolChange(protocol)}
            className={`px-4 py-3 text-sm font-medium transition-colors ${
              selectedProtocol === protocol
                ? "text-gray-900 dark:text-white border-b-2 border-gray-900 dark:border-white"
                : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
            }`}
          >
            {protocol}
          </button>
        ))}
      </div>
    </div>
  );
}
