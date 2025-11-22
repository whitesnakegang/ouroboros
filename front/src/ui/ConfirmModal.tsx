import { useTranslation } from "react-i18next";

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: "danger" | "warning" | "info";
  isLoading?: boolean;
}

export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText,
  cancelText,
  variant = "info",
  isLoading = false,
}: ConfirmModalProps) {
  const { t } = useTranslation();
  if (!isOpen) return null;

  const handleConfirm = () => {
    if (!isLoading) {
      onConfirm();
    }
  };

  const getVariantStyles = () => {
    switch (variant) {
      case "danger":
        return {
          accentColor: "text-red-600 dark:text-red-400",
          confirmButton:
            "bg-red-600 hover:bg-red-700 dark:bg-red-600 dark:hover:bg-red-700",
        };
      case "warning":
        return {
          accentColor: "text-amber-600 dark:text-amber-400",
          confirmButton:
            "bg-amber-600 hover:bg-amber-700 dark:bg-amber-600 dark:hover:bg-amber-700",
        };
      default:
        return {
          accentColor: "text-blue-600 dark:text-blue-400",
          confirmButton:
            "bg-[#2563EB] hover:bg-[#1D4ED8] dark:bg-[#2563EB] dark:hover:bg-[#1D4ED8]",
        };
    }
  };

  const variantStyles = getVariantStyles();

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity duration-200"
        onClick={!isLoading ? onClose : undefined}
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
              {!isLoading && (
                <button
                  onClick={onClose}
                  className="flex-shrink-0 p-1.5 -mt-1 -mr-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#21262D] rounded-md transition-colors"
                  aria-label={t("common.close")}
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
              )}
            </div>
          </div>

          {/* Content */}
          <div className="px-6 pb-6">
            <p className="text-sm text-gray-600 dark:text-[#8B949E] leading-relaxed whitespace-pre-wrap">
              {message}
            </p>
          </div>

          {/* Footer */}
          <div className="px-6 pb-6 flex items-center justify-end gap-3">
            <button
              onClick={onClose}
              disabled={isLoading}
              className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-[#C9D1D9] bg-white dark:bg-[#0D1117] border border-gray-300 dark:border-[#30363D] rounded-lg hover:bg-gray-50 dark:hover:bg-[#161B22] transition-colors focus:outline-none focus:ring-0 focus-visible:outline-none disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]"
            >
              {cancelText || t("common.cancel")}
            </button>
            <button
              onClick={handleConfirm}
              disabled={isLoading}
              className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors focus:outline-none focus:ring-0 focus-visible:outline-none disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98] ${variantStyles.confirmButton}`}
            >
              {isLoading ? (
                <span className="flex items-center gap-2">
                  <svg
                    className="animate-spin h-4 w-4 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  {t("common.processing")}
                </span>
              ) : (
                confirmText || t("common.confirm")
              )}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
