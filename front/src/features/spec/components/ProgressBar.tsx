interface ProgressStep {
  id: string;
  label: string;
  status: "completed" | "current" | "upcoming";
}

interface ProgressBarProps {
  steps: ProgressStep[];
}

export function ProgressBar({ steps }: ProgressBarProps) {
  return (
    <div className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
      <div className="flex items-center px-8 py-4 gap-2">
        {steps.map((step, index) => (
          <div key={step.id} className="flex items-center gap-2 flex-1">
            {/* Step */}
            <div className="flex items-center gap-2">
              <div
                className={`flex items-center justify-center w-8 h-8 rounded-full font-medium text-sm transition-colors ${
                  step.status === "completed"
                    ? "bg-green-500 text-white"
                    : step.status === "current"
                    ? "bg-blue-500 text-white"
                    : "bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400"
                }`}
              >
                {step.status === "completed" ? (
                  <svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                ) : (
                  index + 1
                )}
              </div>
              <span
                className={`text-sm font-medium ${
                  step.status === "current"
                    ? "text-blue-600 dark:text-blue-400"
                    : step.status === "completed"
                    ? "text-green-600 dark:text-green-400"
                    : "text-gray-500 dark:text-gray-400"
                }`}
              >
                {step.label}
              </span>
            </div>

            {/* Connector */}
            {index < steps.length - 1 && (
              <div
                className={`flex-1 h-1 rounded-full mx-2 transition-colors ${
                  step.status === "completed"
                    ? "bg-green-500"
                    : "bg-gray-200 dark:bg-gray-700"
                }`}
              />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
