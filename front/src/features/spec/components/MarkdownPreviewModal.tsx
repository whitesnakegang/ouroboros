import { useState } from "react";
import { downloadMarkdown } from "../utils/markdownExporter";
import { AlertModal } from "@/ui/AlertModal";

interface MarkdownPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  content: string;
  filename?: string;
}

type ViewMode = "preview" | "source";

// 눈 아이콘 컴포넌트
function EyeIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
      />
    </svg>
  );
}

// 간단한 markdown을 HTML로 변환하는 함수
function markdownToHtml(markdown: string): string {
  let html = markdown;
  const codeBlocks: Array<{ placeholder: string; content: string }> = [];
  let codeBlockIndex = 0;

  // 코드 블록을 먼저 임시로 치환 (다른 파싱에 영향받지 않도록)
  html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
    const placeholder = `__CODE_BLOCK_${codeBlockIndex}__`;
    const language = lang || "text";
    codeBlocks.push({
      placeholder,
      content: `<pre class="bg-gray-100 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4 overflow-x-auto my-4"><code class="language-${language}">${escapeHtml(code.trim())}</code></pre>`,
    });
    codeBlockIndex++;
    return placeholder;
  });

  // 인라인 코드 처리 (`code`) - 코드 블록 내부가 아닌 경우만
  html = html.replace(/`([^`\n]+)`/g, '<code class="bg-gray-100 dark:bg-[#0D1117] px-1.5 py-0.5 rounded text-sm font-mono border border-gray-300 dark:border-[#2D333B]">$1</code>');

  // 헤더 처리 (줄 시작에서만) - #### (4개), ##### (5개)도 처리
  html = html.replace(/^##### (.*)$/gim, '<h5 class="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mt-3 mb-2">$1</h5>');
  html = html.replace(/^#### (.*)$/gim, '<h4 class="text-base font-semibold text-gray-900 dark:text-[#E6EDF3] mt-4 mb-2">$1</h4>');
  html = html.replace(/^### (.*)$/gim, '<h3 class="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3] mt-6 mb-3">$1</h3>');
  html = html.replace(/^## (.*)$/gim, '<h2 class="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] mt-8 mb-4">$1</h2>');
  html = html.replace(/^# (.*)$/gim, '<h1 class="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3] mt-8 mb-4">$1</h1>');

  // 볼드 처리 (**text**)
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong class="font-semibold text-gray-900 dark:text-[#E6EDF3]">$1</strong>');

  // 링크 처리 ([text](url))
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="text-blue-600 dark:text-blue-400 hover:underline" target="_blank" rel="noopener noreferrer">$1</a>');

  // 테이블 처리 (더 안정적으로)
  const lines = html.split('\n');
  const processedLines: string[] = [];
  let inTable = false;
  let tableRows: string[] = [];
  let isFirstTableRow = true;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const isTableRow = /^\|.+\|$/.test(line.trim());
    const isTableSeparator = /^\|[\s\S]*:?-+:?[\s\S]*\|$/.test(line.trim());

    if (isTableRow && !isTableSeparator) {
      if (!inTable) {
        inTable = true;
        tableRows = [];
        isFirstTableRow = true;
      }
      
      const cells = line.split('|').map(cell => cell.trim()).filter(cell => cell);
      const tag = isFirstTableRow ? 'th' : 'td';
      const cellClass = isFirstTableRow
        ? 'px-4 py-2 font-semibold text-gray-900 dark:text-[#E6EDF3] bg-gray-100 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B]'
        : 'px-4 py-2 text-gray-700 dark:text-[#C9D1D9] border border-gray-300 dark:border-[#2D333B]';
      
      tableRows.push(`<tr>${cells.map(cell => `<${tag} class="${cellClass}">${escapeHtml(cell)}</${tag}>`).join('')}</tr>`);
      isFirstTableRow = false;
    } else if (isTableSeparator) {
      // 헤더 구분선은 무시
      continue;
    } else {
      if (inTable && tableRows.length > 0) {
        processedLines.push(`<table class="w-full border-collapse border border-gray-300 dark:border-[#2D333B] my-4">${tableRows.join('')}</table>`);
        tableRows = [];
        inTable = false;
        isFirstTableRow = true;
      }
      processedLines.push(line);
    }
  }

  if (inTable && tableRows.length > 0) {
    processedLines.push(`<table class="w-full border-collapse border border-gray-300 dark:border-[#2D333B] my-4">${tableRows.join('')}</table>`);
  }

  html = processedLines.join('\n');

  // 리스트 처리
  html = html.replace(/^\- (.+)$/gim, '<li class="ml-4 list-disc text-gray-700 dark:text-[#C9D1D9] my-1">$1</li>');
  
  // 연속된 li를 ul로 감싸기
  html = html.replace(/(<li[\s\S]*?<\/li>(?:\s*<li[\s\S]*?<\/li>)*)/g, (match) => {
    if (match.includes('<ul')) return match;
    return `<ul class="my-2 space-y-1">${match}</ul>`;
  });

  // 코드 블록 복원
  codeBlocks.forEach(({ placeholder, content }) => {
    html = html.replace(placeholder, content);
  });

  // 줄바꿈 처리 (이미 HTML 태그가 있는 부분은 제외)
  html = html.split('\n').map(line => {
    const trimmed = line.trim();
    // 이미 HTML 태그로 시작하는 줄은 그대로
    if (trimmed.startsWith('<') || trimmed === '') {
      return line;
    }
    // 일반 텍스트 줄은 문단으로 감싸기
    return `<p class="text-gray-700 dark:text-[#C9D1D9] mb-4">${line}</p>`;
  }).join('\n');

  return html;
}

function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

export function MarkdownPreviewModal({
  isOpen,
  onClose,
  content,
  filename = "API_DOCUMENTATION.md",
}: MarkdownPreviewModalProps) {
  const [viewMode, setViewMode] = useState<ViewMode>("source");
  const [alertModal, setAlertModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    variant?: "success" | "error" | "warning" | "info";
  }>({
    isOpen: false,
    title: "",
    message: "",
  });

  if (!isOpen) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      setAlertModal({
        isOpen: true,
        title: "Copy Completed",
        message: "Markdown has been copied to the clipboard.",
        variant: "success",
      });
    } catch {
      setAlertModal({
        isOpen: true,
        title: "Copy Failed",
        message: "Failed to copy to clipboard.",
        variant: "error",
      });
    }
  };

  const handleDownload = () => {
    downloadMarkdown(content, filename);
    onClose();
  };

  const renderedHtml = viewMode === "preview" ? markdownToHtml(content) : null;

  return (
    <>
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40"
        onClick={onClose}
      />
      <div className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none">
        <div className="bg-white dark:bg-[#161B22] rounded-md shadow-2xl max-w-5xl w-full mx-4 max-h-[85vh] overflow-hidden flex flex-col pointer-events-auto border border-gray-200 dark:border-[#2D333B]">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-[#2D333B] flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3]">
              Markdown Preview
            </h2>
            <div className="flex items-center gap-2">
              {/* View Mode Toggle - 눈 아이콘만 */}
              <button
                onClick={() => setViewMode(viewMode === "preview" ? "source" : "preview")}
                className={`p-2 rounded-md transition-all focus:outline-none focus-visible:outline-none ${
                  viewMode === "preview"
                    ? "bg-[#2563EB] text-white"
                    : "bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] border border-gray-300 dark:border-[#2D333B]"
                }`}
                title={viewMode === "preview" ? "Show Source" : "Show Preview"}
              >
                <EyeIcon className="w-5 h-5" />
              </button>
              <button
                onClick={handleCopy}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
              >
                Copy
              </button>
              <button
                onClick={onClose}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
              >
                Close
              </button>
            </div>
          </div>
          <div className="flex-1 overflow-auto p-0">
            {viewMode === "preview" ? (
              <div
                className="m-0 p-6 text-sm leading-6 text-gray-800 dark:text-[#E6EDF3] bg-white dark:bg-[#161B22] prose prose-sm dark:prose-invert max-w-none"
                dangerouslySetInnerHTML={{ __html: renderedHtml || "" }}
              />
            ) : (
              <pre className="m-0 p-4 text-sm leading-6 whitespace-pre-wrap text-gray-800 dark:text-[#E6EDF3] bg-gray-50 dark:bg-[#0D1117] font-mono">
                {content}
              </pre>
            )}
          </div>
          <div className="px-6 py-4 border-t border-gray-200 dark:border-[#2D333B] flex items-center justify-end gap-2">
            <button
              onClick={handleDownload}
              className="px-4 py-2 bg-[#2563EB] hover:bg-[#1E40AF] text-white rounded-md text-sm font-medium transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
            >
              Download
            </button>
          </div>
        </div>
      </div>

      {/* Alert Modal */}
      <AlertModal
        isOpen={alertModal.isOpen}
        onClose={() => setAlertModal((prev) => ({ ...prev, isOpen: false }))}
        title={alertModal.title}
        message={alertModal.message}
        variant={alertModal.variant}
      />
    </>
  );
}
