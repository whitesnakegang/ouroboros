import { useState, useEffect, useRef } from "react";
import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/theme-monokai";
import "ace-builds/src-noconflict/theme-github";
import "ace-builds/src-noconflict/ext-language_tools";

interface JsonEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  height?: string;
  readOnly?: boolean;
  className?: string;
}

export function JsonEditor({
  value,
  onChange,
  placeholder = '{\n  "key": "value"\n}',
  height = "300px",
  readOnly = false,
  className = "",
}: JsonEditorProps) {
  const editorRef = useRef<AceEditor>(null);
  const [isDarkMode, setIsDarkMode] = useState(false);

  // 다크 모드 감지 및 변경 감지
  useEffect(() => {
    const checkDarkMode = () => {
      setIsDarkMode(
        document.documentElement.classList.contains("dark") ||
          window.matchMedia("(prefers-color-scheme: dark)").matches
      );
    };

    checkDarkMode();

    // MutationObserver로 dark 클래스 변경 감지
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class"],
    });

    // 미디어 쿼리 변경 감지
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    mediaQuery.addEventListener("change", checkDarkMode);

    return () => {
      observer.disconnect();
      mediaQuery.removeEventListener("change", checkDarkMode);
    };
  }, []);

  return (
    <div className={`relative ${className}`}>
      <AceEditor
        ref={editorRef}
        mode="json"
        theme={isDarkMode ? "monokai" : "github"}
        value={value}
        onChange={onChange}
        name="json-editor"
        editorProps={{ $blockScrolling: true }}
        setOptions={{
          enableBasicAutocompletion: true,
          enableLiveAutocompletion: true,
          enableSnippets: true,
          showLineNumbers: true,
          tabSize: 2,
          useSoftTabs: true,
          wrap: true,
          showPrintMargin: false,
          readOnly: readOnly,
          placeholder: placeholder,
        }}
        width="100%"
        height={height}
        fontSize={14}
        style={{
          borderRadius: "0.375rem",
          border: "1px solid",
          borderColor: isDarkMode ? "#2D333B" : "#D1D5DB",
        }}
      />
    </div>
  );
}

