import { useEffect, useState } from "react";
import dataFakerMethods from "../../../assets/data/datafaker-methods.json";

interface MockExpressionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (expression: string) => void;
  initialValue?: string;
}

interface Provider {
  name: string;
  category: string;
  icon: string;
  methods: Method[];
}

interface Method {
  name: string;
  expression: string;
  description: string;
  example: string;
  hasParams?: boolean;
}

export function MockExpressionModal({
  isOpen,
  onClose,
  onSelect,
  initialValue = "",
}: MockExpressionModalProps) {
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(
    null
  );
  const [selectedMethod, setSelectedMethod] = useState<Method | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [options, setOptions] = useState({
    length: "",
    min: "",
    max: "",
  });

  const providers = dataFakerMethods.providers as Provider[];

  // 초기값 파싱 (예: {{$name.fullName}} → expression 값으로 찾기)
  useEffect(() => {
    if (initialValue && isOpen) {
      // {{$...}} 형식에서 expression 추출 (파라미터 제거)
      const match = initialValue.match(/\{\{\$(.+?)\}\}/);
      if (match) {
        const expressionWithParams = match[1];
        // 파라미터가 있는 경우 제거 (예: "lorem.fixedString(10)" → "lorem.fixedString")
        const expression = expressionWithParams.split("(")[0].trim();

        // 모든 provider의 methods에서 expression과 일치하는 것 찾기
        for (const provider of providers) {
          const method = provider.methods.find(
            (m) => m.expression === expression
          );

          if (method) {
            setSelectedProvider(provider);
            setSelectedMethod(method);

            // 파라미터 추출 (있는 경우)
            const paramMatch = expressionWithParams.match(/\((.+)\)/);
            if (paramMatch && method.hasParams) {
              const params = paramMatch[1].split(",").map((p) => p.trim());
              setOptions({
                length: params[0] || "",
                min: params[1] || "",
                max: params[2] || "",
              });
            }
            break;
          }
        }
      }
    }
  }, [initialValue, isOpen, providers]);

  // 검색어 필터링
  const filteredProviders = searchQuery
    ? providers.filter(
        (p) =>
          p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          p.methods.some((m) =>
            m.description.toLowerCase().includes(searchQuery.toLowerCase())
          )
      )
    : providers;

  // 최종 표현식 생성 - JSON의 expression 값을 직접 사용
  const generateExpression = () => {
    if (!selectedMethod || !selectedProvider) return "";

    // JSON의 expression 값을 직접 사용 (예: "name.fullName", "lorem.fixedString")
    let expr = selectedMethod.expression;

    // 파라미터가 있는 메소드의 경우
    if (selectedMethod.hasParams) {
      const params = [];
      if (options.length) params.push(options.length);
      if (options.min) params.push(options.min);
      if (options.max) params.push(options.max);

      if (params.length > 0) {
        expr += `(${params.join(", ")})`;
      }
    }

    // expressionFormat 적용 ({{$%s}})
    const format = dataFakerMethods.expressionFormat || "{{$%s}}";
    return format.replace("%s", expr);
  };

  const handleConfirm = () => {
    const expression = generateExpression();
    if (expression) {
      onSelect(expression);
      handleClose();
    }
  };

  const handleClose = () => {
    setSelectedProvider(null);
    setSelectedMethod(null);
    setSearchQuery("");
    setOptions({ length: "", min: "", max: "" });
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="bg-white dark:bg-[#0D1117] rounded-2xl shadow-2xl w-full max-w-4xl max-h-[80vh] overflow-hidden border border-gray-200 dark:border-[#30363D]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-[#30363D]">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3]">
              Select DataFaker Mock Expression
            </h2>
          </div>
          <button
            onClick={handleClose}
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-[#30363D] transition-colors"
          >
            <svg
              className="w-6 h-6 text-gray-500"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
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

        {/* Content */}
        <div className="flex h-[calc(80vh-200px)]">
          {/* Left Panel - Provider List */}
          <div className="w-1/3 border-r border-gray-200 dark:border-[#30363D] overflow-y-auto">
            <div className="p-4">
              <input
                type="text"
                placeholder="Search providers..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 dark:border-[#30363D] rounded-lg bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3] focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="space-y-1 px-2">
              {filteredProviders.map((provider) => (
                <button
                  key={provider.name}
                  onClick={() => {
                    setSelectedProvider(provider);
                    setSelectedMethod(null);
                  }}
                  className={`w-full text-left px-4 py-3 rounded-lg transition-all focus:outline-none focus:ring-0 ${
                    selectedProvider?.name === provider.name
                      ? "bg-gray-100 dark:bg-gray-800 border-2 border-gray-300 dark:border-gray-600"
                      : "hover:bg-gray-100 dark:hover:bg-[#161B22] border-2 border-transparent"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">{provider.icon}</span>
                    <div className="flex-1">
                      <div className="font-semibold text-gray-900 dark:text-[#E6EDF3]">
                        {provider.name}
                      </div>
                      <div className="text-xs text-gray-500 dark:text-[#8B949E]">
                        {provider.methods.length} methods
                      </div>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Middle Panel - Method List */}
          <div className="w-1/3 border-r border-gray-200 dark:border-[#30363D] overflow-y-auto">
            {selectedProvider ? (
              <div className="p-4 space-y-1">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-[#8B949E] mb-3 px-2">
                  {selectedProvider.icon} {selectedProvider.name} Methods
                </h3>
                {selectedProvider.methods.map((method) => (
                  <button
                    key={method.name}
                    onClick={() => setSelectedMethod(method)}
                    className={`w-full text-left px-4 py-3 rounded-lg transition-all focus:outline-none focus:ring-0 ${
                      selectedMethod?.name === method.name
                        ? "bg-gray-100 dark:bg-gray-800 border-2 border-gray-300 dark:border-gray-600"
                        : "hover:bg-gray-100 dark:hover:bg-[#161B22] border-2 border-transparent"
                    }`}
                  >
                    <div className="font-mono text-sm font-semibold text-gray-900 dark:text-[#E6EDF3]">
                      {method.name}()
                      {method.hasParams && (
                        <span className="ml-2 text-xs text-orange-600 dark:text-orange-400">
                          params
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-gray-500 dark:text-[#8B949E] mt-1">
                      {method.description}
                    </div>
                    <div className="text-xs text-gray-400 dark:text-[#6E7681] mt-1 font-mono">
                      Example: {method.example}
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-400 dark:text-[#8B949E]">
                <div className="text-center">
                  <svg
                    className="w-12 h-12 mx-auto mb-2 text-gray-300 dark:text-[#6B7280]"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                  <p>Select a Provider</p>
                </div>
              </div>
            )}
          </div>

          {/* Right Panel - Preview & Options */}
          <div className="w-1/3 overflow-y-auto bg-gray-50 dark:bg-[#161B22]">
            {selectedMethod ? (
              <div className="p-4 space-y-4">
                <div>
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-[#8B949E] mb-2">
                    Selected Method
                  </h3>
                  <div className="bg-white dark:bg-[#0D1117] p-4 rounded-lg border border-gray-200 dark:border-[#30363D]">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-xl">{selectedProvider?.icon}</span>
                      <span className="font-semibold text-gray-900 dark:text-[#E6EDF3]">
                        {selectedProvider?.name}
                      </span>
                    </div>
                    <div className="font-mono text-sm text-blue-600 dark:text-blue-400">
                      {selectedMethod.name}()
                    </div>
                    <div className="text-xs text-gray-500 dark:text-[#8B949E] mt-2">
                      {selectedMethod.description}
                    </div>
                  </div>
                </div>

                {/* Options (파라미터가 있는 경우만) */}
                {selectedMethod.hasParams && (
                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 dark:text-[#8B949E] mb-2">
                      Options (optional)
                    </h3>
                    <div className="space-y-2">
                      <div>
                        <label className="block text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                          Length
                        </label>
                        <input
                          type="number"
                          value={options.length}
                          onChange={(e) =>
                            setOptions({ ...options, length: e.target.value })
                          }
                          placeholder="10"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3]"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                          Min
                        </label>
                        <input
                          type="number"
                          value={options.min}
                          onChange={(e) =>
                            setOptions({ ...options, min: e.target.value })
                          }
                          placeholder="1"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3]"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-600 dark:text-[#8B949E] mb-1">
                          Max
                        </label>
                        <input
                          type="number"
                          value={options.max}
                          onChange={(e) =>
                            setOptions({ ...options, max: e.target.value })
                          }
                          placeholder="100"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-[#30363D] rounded-md bg-white dark:bg-[#0D1117] text-gray-900 dark:text-[#E6EDF3]"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Preview */}
                <div>
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-[#8B949E] mb-2">
                    Preview
                  </h3>
                  <div className="bg-gray-900 dark:bg-black p-4 rounded-lg border border-gray-700">
                    <div className="font-mono text-sm text-green-400">
                      {generateExpression() || "{{$provider.method}}"}
                    </div>
                  </div>
                  <p className="text-xs text-gray-500 dark:text-[#8B949E] mt-2">
                    This expression creates mock data using DataFaker in the
                    backend.
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-400 dark:text-[#8B949E]">
                <div className="text-center">
                  <svg
                    className="w-12 h-12 mx-auto mb-2 text-gray-300 dark:text-[#6B7280]"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                  <p>Select a method</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between p-6 border-t border-gray-200 dark:border-[#30363D] bg-gray-50 dark:bg-[#161B22]">
          <div className="text-sm text-gray-500 dark:text-[#8B949E]">
            {selectedMethod && selectedProvider && (
              <span className="font-mono text-xs">{generateExpression()}</span>
            )}
          </div>
          <div className="flex gap-3">
            <button
              onClick={handleClose}
              className="px-6 py-2 border border-gray-300 dark:border-[#30363D] rounded-lg hover:bg-gray-100 dark:hover:bg-[#30363D] transition-colors text-gray-700 dark:text-[#E6EDF3]"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirm}
              disabled={!selectedMethod || !selectedProvider}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 dark:disabled:bg-gray-700 disabled:cursor-not-allowed transition-colors"
            >
              Select
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
