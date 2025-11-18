interface AlertModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  message: string;
  variant?: "success" | "error" | "warning" | "info";
  confirmText?: string;
}

export function AlertModal({
  isOpen,
  onClose,
  title,
  message,
  variant = "info",
  confirmText = "OK",
}: AlertModalProps) {
  if (!isOpen) return null;

  const getVariantStyles = () => {
    switch (variant) {
      case "success":
        return {
          accentColor: "text-emerald-600 dark:text-emerald-400",
          borderColor: "border-emerald-500/20",
        };
      case "error":
        return {
          accentColor: "text-red-600 dark:text-red-400",
          borderColor: "border-red-500/20",
        };
      case "warning":
        return {
          accentColor: "text-amber-600 dark:text-amber-400",
          borderColor: "border-amber-500/20",
        };
      default:
        return {
          accentColor: "text-blue-600 dark:text-blue-400",
          borderColor: "border-blue-500/20",
        };
    }
  };

  const variantStyles = getVariantStyles();

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity duration-200"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none p-4">
        <div className="bg-white dark:bg-[#161B22] rounded-lg shadow-xl max-w-md w-full overflow-hidden flex flex-col pointer-events-auto border border-gray-200/50 dark:border-[#30363D] transform transition-all duration-200">
          {/* Header */}
          <div className="px-6 pt-6 pb-4">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <h2 className={`text-lg font-semibold ${variantStyles.accentColor} leading-tight`}>
                  {title}
                </h2>
              </div>
              <button
                onClick={onClose}
                className="flex-shrink-0 p-1.5 -mt-1 -mr-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#21262D] rounded-md transition-colors"
                aria-label="Close"
              >
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
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          </div>

          {/* Content */}
          <div className="px-6 pb-6">
            <p className="text-sm text-gray-600 dark:text-[#8B949E] leading-relaxed whitespace-pre-wrap">
              {message}
            </p>
          </div>

          {/* Footer */}
          <div className="px-6 pb-6 flex items-center justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:ring-offset-2 dark:focus:ring-offset-[#0D1117] active:scale-[0.98]"
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
