import { useTestingStore } from "../store/testing.store";

export function TestRequestPanel() {
  const {
    request,
    setRequest,
    updateRequestHeader,
    addRequestHeader,
    removeRequestHeader,
  } = useTestingStore();

  const methods = ["GET", "POST", "PUT", "PATCH", "DELETE"];

  return (
    <div className="rounded-md border border-gray-200 dark:border-[#2D333B] bg-white dark:bg-[#161B22] p-4 shadow-sm">
      <div className="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mb-2 flex items-center gap-2">
        <svg
          className="h-4 w-4 text-gray-500 dark:text-[#8B949E]"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 4v16m8-8H4"
          />
        </svg>
        <span>Request</span>
      </div>

      {/* Method + URL */}
      <div className="space-y-4 mb-6">
        <div className="grid grid-cols-12 gap-3">
          <div className="col-span-3">
            <label className="block text-xs font-medium text-[#8B949E] mb-2">
              METHOD
            </label>
            <select
              value={request.method}
              onChange={(e) => setRequest({ method: e.target.value })}
              className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-medium"
            >
              {methods.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>
          <div className="col-span-9">
            <label className="block text-xs font-medium text-[#8B949E] mb-2">
              URL
            </label>
            <input
              type="text"
              value={request.url}
              onChange={(e) => setRequest({ url: e.target.value })}
              placeholder="/api/endpoint"
              className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm font-mono"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-[#8B949E] mb-2">
            Desc
          </label>
          <input
            type="text"
            value={request.description}
            onChange={(e) => setRequest({ description: e.target.value })}
            placeholder="API 설명"
              className="w-full px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
          />
        </div>
      </div>

      {/* Headers */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-3">
          <label className="text-xs font-medium text-[#8B949E]">
            Headers
          </label>
          <button
            onClick={addRequestHeader}
            className="text-xs px-2 py-1 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md transition-colors"
          >
            + Add
          </button>
        </div>
        <div className="space-y-2">
          {request.headers.map((header, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                value={header.key}
                onChange={(e) =>
                  updateRequestHeader(index, e.target.value, header.value)
                }
                placeholder="Key"
                className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
              <input
                type="text"
                value={header.value}
                onChange={(e) =>
                  updateRequestHeader(index, header.key, e.target.value)
                }
                placeholder="Value"
                className="flex-1 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] text-sm"
              />
              <button
                onClick={() => removeRequestHeader(index)}
                className="px-3 py-2 bg-transparent border border-[#2D333B] text-[#8B949E] hover:text-red-500 hover:border-red-500 rounded-md transition-colors"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Request Body */}
      <div>
        <label className="block text-xs font-medium text-[#8B949E] mb-2">
          Request Body
        </label>
        <textarea
          value={request.body}
          onChange={(e) => setRequest({ body: e.target.value })}
          placeholder='{ "key": "value" }'
          className="w-full h-40 px-3 py-2 rounded-md bg-[#0D1117] border border-[#2D333B] text-[#E6EDF3] placeholder:text-[#8B949E] focus:outline-none focus:ring-1 focus:ring-[#2563EB] focus:border-[#2563EB] font-mono text-sm"
        />
      </div>
    </div>
  );
}

