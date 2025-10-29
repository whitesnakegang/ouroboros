import { useEffect, useMemo, useRef, useState } from "react";
import providersJson from "../../../assets/data/datafaker_providers.json";

interface FakerProviderSelectProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

export function FakerProviderSelect({
  value,
  onChange,
  placeholder = "Select faker provider...",
  disabled = false,
}: FakerProviderSelectProps) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const dropdownRef = useRef<HTMLDivElement | null>(null);
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(0);
  const [position, setPosition] = useState({ top: 0, left: 0, width: 0 });

  const names = useMemo(() => {
    const arr = Array.isArray((providersJson as any).providers)
      ? (providersJson as any).providers
      : [];
    return arr.map((p: any) => String(p.name));
  }, []);

  const isValidValue = (val: string) => {
    return names.includes(val);
  };

  const filtered = useMemo(() => {
    const q = (query || value || "").trim().toLowerCase();
    if (!q) return names.slice(0, 50);
    return names.filter((n) => n.toLowerCase().includes(q)).slice(0, 50);
  }, [names, query, value]);

  useEffect(() => {
    if (!open) setHighlightIndex(0);
  }, [open]);

  useEffect(() => {
    // 드롭다운 위치 계산
    if (open && inputRef.current) {
      const rect = inputRef.current.getBoundingClientRect();
      setPosition({
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX,
        width: rect.width,
      });
    }
  }, [open]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!inputRef.current || !dropdownRef.current) return;
      const target = e.target as Node;
      if (
        !inputRef.current.contains(target) &&
        !dropdownRef.current.contains(target)
      ) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, []);

  const applySelection = (name: string) => {
    onChange(name);
    setQuery("");
    setOpen(false);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setQuery(newValue);
    if (!open) setOpen(true);

    // 유효한 값만 허용
    if (newValue && names.includes(newValue)) {
      onChange(newValue);
    }
  };

  return (
    <>
      <input
        ref={inputRef}
        type="text"
        value={open ? query : value}
        onChange={handleInputChange}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setOpen(true);
            setHighlightIndex((i) =>
              Math.min(i + 1, Math.max(filtered.length - 1, 0))
            );
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setOpen(true);
            setHighlightIndex((i) => Math.max(i - 1, 0));
          } else if (e.key === "Enter") {
            e.preventDefault();
            const pick = filtered[highlightIndex];
            if (pick) applySelection(pick);
          } else if (e.key === "Escape") {
            setOpen(false);
            setQuery("");
          }
        }}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
        autoComplete="off"
      />
      {open && filtered.length > 0 && (
        <div
          ref={dropdownRef}
          className="fixed z-50 mt-1 max-h-64 overflow-auto bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded shadow-lg"
          style={{
            top: `${position.top}px`,
            left: `${position.left}px`,
            width: `${position.width}px`,
          }}
        >
          {filtered.map((name, idx) => (
            <button
              key={name}
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => applySelection(name)}
              onMouseEnter={() => setHighlightIndex(idx)}
              className={`w-full text-left px-3 py-2 text-sm ${
                idx === highlightIndex
                  ? "bg-blue-50 dark:bg-blue-900 text-blue-700 dark:text-blue-200"
                  : "text-gray-800 dark:text-gray-100"
              }`}
            >
              {name}
            </button>
          ))}
          {filtered.length === 50 && (
            <div className="px-3 py-2 text-xs text-gray-500 dark:text-gray-400">
              상위 50개만 표시됩니다. 더 입력해 좁혀보세요.
            </div>
          )}
        </div>
      )}
    </>
  );
}
