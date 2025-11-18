import { useEffect, useState } from "react";
import { getAllWebSocketChannels } from "../services/api";

export function WsChannelMessageTree() {
  const [channels, setChannels] = useState<
    Array<{ name: string; address?: string; messages: string[] }>
  >([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const run = async () => {
      setLoading(true);
      try {
        const chRes = await getAllWebSocketChannels();

        const chList = (chRes?.data || []).map((c: any) => {
          const msgs = c?.channel?.messages
            ? Object.keys(c.channel.messages || {})
            : [];
          return {
            name: c.channelName,
            address: c.channel?.address,
            messages: msgs,
          };
        });
        setChannels(chList);
      } finally {
        setLoading(false);
      }
    };
    run();
  }, []);

  return (
    <div className="border rounded-md dark:border-[#2D333B]">
      <div className="px-3 py-2 border-b dark:border-[#2D333B] text-sm font-semibold">
        Channels / Messages
      </div>
      <div className="max-h-80 overflow-auto p-2 text-sm">
        {loading ? (
          <div className="text-gray-500">Loading...</div>
        ) : channels.length === 0 ? (
          <div className="text-gray-500">No channels</div>
        ) : (
          <ul className="space-y-2">
            {channels.map((ch) => (
              <li key={ch.name}>
                <div className="font-mono text-gray-800 dark:text-[#E6EDF3]">
                  {ch.address ? `${ch.name} (${ch.address})` : ch.name}
                </div>
                {ch.messages?.length ? (
                  <ul className="ml-4 list-disc">
                    {ch.messages.map((m) => (
                      <li
                        key={m}
                        className="font-mono text-gray-600 dark:text-[#8B949E]"
                      >
                        {m}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="ml-4 text-gray-500">No messages</div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}


