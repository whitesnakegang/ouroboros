import { downloadMarkdown } from "../utils/markdownExporter";

interface MarkdownPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  content: string;
  filename?: string;
}

export function MarkdownPreviewModal({
  isOpen,
  onClose,
  content,
  filename = "API_DOCUMENTATION.md",
}: MarkdownPreviewModalProps) {
  if (!isOpen) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      alert("Markdown이 클립보드에 복사되었습니다.");
    } catch {
      alert("클립보드 복사에 실패했습니다.");
    }
  };

  const handleDownload = () => {
    downloadMarkdown(content, filename);
    onClose();
  };

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none">
        <div className="bg-white dark:bg-[#161B22] rounded-md shadow-2xl max-w-5xl w-full mx-4 max-h-[85vh] overflow-hidden flex flex-col pointer-events-auto border border-gray-200 dark:border-[#2D333B]">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">Markdown 미리보기</h2>
            <div className="flex items-center gap-2">
              <button
                onClick={handleCopy}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
              >
                복사
              </button>
              <button
                onClick={onClose}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117]"
              >
                닫기
              </button>
            </div>
          </div>
          <div className="flex-1 overflow-auto p-0">
            <pre className="m-0 p-4 text-sm leading-6 whitespace-pre-wrap text-gray-800 dark:text-[#E6EDF3] bg-gray-50 dark:bg-[#0D1117]">
{content}
            </pre>
          </div>
          <div className="px-6 py-4 border-t border-gray-200 dark:border-[#2D333B] flex items-center justify-end gap-2">
            <button
              onClick={handleDownload}
              className="px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium"
            >
              다운로드
            </button>
          </div>
        </div>
      </div>
    </>
  );
}


