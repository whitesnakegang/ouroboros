import { useState } from "react";
import { useTranslation } from "react-i18next";
import { downloadMarkdown } from "../utils/markdownExporter";
import { AlertModal } from "@/ui/AlertModal";
import DOMPurify from "dompurify";

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
  html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (_match, lang, code) => {
    const placeholder = `__CODE_BLOCK_${codeBlockIndex}__`;
    const language = lang || "text";
    codeBlocks.push({
      placeholder,
      content: `<pre class="bg-gray-100 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B] rounded-md p-4 overflow-x-auto my-4"><code class="language-${language}">${escapeHtml(
        code.trim()
      )}</code></pre>`,
    });
    codeBlockIndex++;
    return placeholder;
  });

  // 테이블 처리 (다른 마크다운 처리 전에 먼저 처리)
  // 테이블 셀 내부의 마크다운을 처리하기 위한 헬퍼 함수
  const processTableCell = (cell: string): string => {
    // 셀 내부의 마크다운 문법 처리 (원본 마크다운 상태에서)
    let processed = cell.trim();

    // 인라인 코드 처리
    processed = processed.replace(/`([^`\n]+)`/g, (_match, code) => {
      return `<code class="bg-gray-100 dark:bg-[#0D1117] px-1.5 py-0.5 rounded text-sm font-mono border border-gray-300 dark:border-[#2D333B]">${escapeHtml(
        code
      )}</code>`;
    });

    // 볼드 처리
    processed = processed.replace(/\*\*([^*]+)\*\*/g, (_match, text) => {
      return `<strong class="font-semibold text-gray-900 dark:text-[#E6EDF3]">${escapeHtml(
        text
      )}</strong>`;
    });

    // 나머지 텍스트는 escape (HTML 태그가 아닌 부분만)
    return processed
      .split(/(<[^>]+>)/g)
      .map((part) => {
        if (part.startsWith("<") && part.endsWith(">")) {
          return part; // HTML 태그는 그대로
        }
        return escapeHtml(part); // 텍스트만 escape
      })
      .join("");
  };

  // 테이블을 먼저 처리하고 placeholder로 치환
  const tablePlaceholders: Array<{ placeholder: string; content: string }> = [];
  let tableIndex = 0;

  const lines = html.split("\n");
  const processedLines: string[] = [];
  let inTable = false;
  let tableRows: string[] = [];
  let isFirstTableRow = true;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmedLine = line.trim();

    // 테이블 구분선 체크 (더 정확한 패턴)
    const isTableSeparator =
      /^\|[\s:]*[-:]+[\s:]*(\|[\s:]*[-:]+[\s:]*)*\|$/.test(trimmedLine);

    // 테이블 행 체크 (|로 시작하고 끝나야 함, 최소 2개의 |가 있어야 함)
    const isTableRow =
      trimmedLine.startsWith("|") &&
      trimmedLine.endsWith("|") &&
      trimmedLine.split("|").length >= 3;

    if (isTableRow && !isTableSeparator) {
      if (!inTable) {
        inTable = true;
        tableRows = [];
        isFirstTableRow = true;
      }

      // 테이블 셀 파싱: 첫 번째와 마지막 빈 요소 제거, 중간 빈 셀은 유지
      const splitCells = line.split("|");
      // 첫 번째와 마지막이 빈 문자열이면 제거하고 나머지는 유지
      const cells: string[] = [];
      // 첫 번째와 마지막 인덱스는 제외하고 중간 셀만 처리
      for (let j = 1; j < splitCells.length - 1; j++) {
        const trimmed = splitCells[j].trim();
        cells.push(trimmed);
      }

      // 셀이 비어있으면 "-"로 표시
      if (cells.length > 0) {
        const tag = isFirstTableRow ? "th" : "td";
        const cellClass = isFirstTableRow
          ? "px-4 py-2 font-semibold text-gray-900 dark:text-[#E6EDF3] bg-gray-100 dark:bg-[#0D1117] border border-gray-300 dark:border-[#2D333B]"
          : "px-4 py-2 text-gray-700 dark:text-[#C9D1D9] border border-gray-300 dark:border-[#2D333B]";

        tableRows.push(
          `<tr>${cells
            .map(
              (cell) =>
                `<${tag} class="${cellClass}">${processTableCell(
                  cell || "-"
                )}</${tag}>`
            )
            .join("")}</tr>`
        );
        isFirstTableRow = false;
      }
    } else if (isTableSeparator) {
      // 헤더 구분선은 무시하되, 테이블이 시작되었다면 계속 유지
      if (inTable) {
        // 구분선 다음부터는 헤더가 아님
        isFirstTableRow = false;
      }
      continue;
    } else {
      // 테이블이 아닌 줄이 나오면 테이블 종료
      if (inTable && tableRows.length > 0) {
        const placeholder = `__TABLE_${tableIndex}__`;
        const tableHtml = `<table class="w-full border-collapse border border-gray-300 dark:border-[#2D333B] my-4">${tableRows.join(
          ""
        )}</table>`;
        tablePlaceholders.push({ placeholder, content: tableHtml });
        processedLines.push(placeholder);
        tableIndex++;
        tableRows = [];
        inTable = false;
        isFirstTableRow = true;
      }
      processedLines.push(line);
    }
  }

  if (inTable && tableRows.length > 0) {
    const placeholder = `__TABLE_${tableIndex}__`;
    const tableHtml = `<table class="w-full border-collapse border border-gray-300 dark:border-[#2D333B] my-4">${tableRows.join(
      ""
    )}</table>`;
    tablePlaceholders.push({ placeholder, content: tableHtml });
    processedLines.push(placeholder);
  }

  html = processedLines.join("\n");

  // 이제 나머지 마크다운 처리 (테이블은 이미 placeholder로 치환됨)
  // 인라인 코드 처리 (`code`) - 코드 블록 내부가 아닌 경우만
  html = html.replace(
    /`([^`\n]+)`/g,
    '<code class="bg-gray-100 dark:bg-[#0D1117] px-1.5 py-0.5 rounded text-sm font-mono border border-gray-300 dark:border-[#2D333B]">$1</code>'
  );

  // 헤더 처리 (줄 시작에서만) - #### (4개), ##### (5개)도 처리
  html = html.replace(
    /^##### (.*)$/gim,
    '<h5 class="text-sm font-semibold text-gray-900 dark:text-[#E6EDF3] mt-3 mb-2">$1</h5>'
  );
  html = html.replace(
    /^#### (.*)$/gim,
    '<h4 class="text-base font-semibold text-gray-900 dark:text-[#E6EDF3] mt-4 mb-2">$1</h4>'
  );
  html = html.replace(
    /^### (.*)$/gim,
    '<h3 class="text-lg font-semibold text-gray-900 dark:text-[#E6EDF3] mt-6 mb-3">$1</h3>'
  );
  html = html.replace(
    /^## (.*)$/gim,
    '<h2 class="text-xl font-semibold text-gray-900 dark:text-[#E6EDF3] mt-8 mb-4">$1</h2>'
  );
  html = html.replace(
    /^# (.*)$/gim,
    '<h1 class="text-2xl font-bold text-gray-900 dark:text-[#E6EDF3] mt-8 mb-4">$1</h1>'
  );

  // 볼드 처리 (**text**)
  html = html.replace(
    /\*\*([^*]+)\*\*/g,
    '<strong class="font-semibold text-gray-900 dark:text-[#E6EDF3]">$1</strong>'
  );

  // 링크 처리 ([text](url))
  html = html.replace(
    /\[([^\]]+)\]\(([^)]+)\)/g,
    '<a href="$2" class="text-blue-600 dark:text-blue-400 hover:underline" target="_blank" rel="noopener noreferrer">$1</a>'
  );

  // 리스트 처리
  html = html.replace(
    /^- (.+)$/gim,
    '<li class="ml-4 list-disc text-gray-700 dark:text-[#C9D1D9] my-1">$1</li>'
  );

  // 연속된 li를 ul로 감싸기
  html = html.replace(
    /(<li[\s\S]*?<\/li>(?:\s*<li[\s\S]*?<\/li>)*)/g,
    (match) => {
      if (match.includes("<ul")) return match;
      return `<ul class="my-2 space-y-1">${match}</ul>`;
    }
  );

  // 줄바꿈 처리 (이미 HTML 태그가 있는 부분은 제외)
  html = html
    .split("\n")
    .map((line) => {
      const trimmed = line.trim();
      // 이미 HTML 태그로 시작하는 줄은 그대로
      if (trimmed.startsWith("<") || trimmed === "") {
        return line;
      }
      // placeholder는 그대로 유지
      if (
        trimmed.startsWith("__TABLE_") ||
        trimmed.startsWith("__CODE_BLOCK_")
      ) {
        return line;
      }
      // 일반 텍스트 줄은 문단으로 감싸기
      return `<p class="text-gray-700 dark:text-[#C9D1D9] mb-4">${line}</p>`;
    })
    .join("\n");

  // 테이블 placeholder 복원 (줄바꿈 처리 후)
  tablePlaceholders.forEach(({ placeholder, content }) => {
    html = html.replace(placeholder, content);
  });

  // 코드 블록 복원
  codeBlocks.forEach(({ placeholder, content }) => {
    html = html.replace(placeholder, content);
  });

  return html;
}

function escapeHtml(text: string): string {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

export function MarkdownPreviewModal({
  isOpen,
  onClose,
  content,
  filename = "API_DOCUMENTATION.md",
}: MarkdownPreviewModalProps) {
  const { t } = useTranslation();
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
        title: t("markdownPreview.copyCompleted"),
        message: t("markdownPreview.markdownCopiedToClipboard"),
        variant: "success",
      });
    } catch {
      setAlertModal({
        isOpen: true,
        title: t("markdownPreview.copyFailed"),
        message: t("markdownPreview.failedToCopyToClipboard"),
        variant: "error",
      });
    }
  };

  const handleDownload = () => {
    downloadMarkdown(content, filename);
    onClose();
  };

  const renderedHtml =
    viewMode === "preview"
      ? DOMPurify.sanitize(markdownToHtml(content), {
          ALLOWED_TAGS: [
            "p",
            "br",
            "strong",
            "em",
            "u",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "ul",
            "ol",
            "li",
            "a",
            "code",
            "pre",
            "table",
            "thead",
            "tbody",
            "tr",
            "th",
            "td",
            "hr",
          ],
          ALLOWED_ATTR: ["class", "href", "target", "rel"],
        })
      : null;

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
              {t("markdownPreview.markdownPreview")}
            </h2>
            <div className="flex items-center gap-2">
              {/* View Mode Toggle - 눈 아이콘만 */}
              <button
                onClick={() =>
                  setViewMode(viewMode === "preview" ? "source" : "preview")
                }
                className={`p-2 rounded-md transition-all focus:outline-none focus-visible:outline-none ${
                  viewMode === "preview"
                    ? "bg-[#2563EB] text-white"
                    : "bg-white dark:bg-[#0D1117] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#161B22] border border-gray-300 dark:border-[#2D333B]"
                }`}
                title={viewMode === "preview" ? t("markdownPreview.showSource") : t("markdownPreview.showPreview")}
              >
                <EyeIcon className="w-5 h-5" />
              </button>
              <button
                onClick={handleCopy}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
              >
                {t("markdownPreview.copy")}
              </button>
              <button
                onClick={onClose}
                className="px-3 py-2 text-sm rounded-md border border-gray-300 dark:border-[#2D333B] text-gray-700 dark:text-[#E6EDF3] hover:bg-gray-50 dark:hover:bg-[#0D1117] transition-all active:translate-y-[1px] focus:outline-none focus-visible:outline-none"
              >
                {t("common.close")}
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
              {t("markdownPreview.download")}
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
