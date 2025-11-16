import { useState, useEffect } from "react";

interface FilenameOptionsProps {
  protocol: "REST" | "WebSocket";
  onChange: (filename: string) => void;
  baseName?: string;
  version?: string;
  ext: "yml" | "md";
}

export function FilenameOptions({
  protocol,
  onChange,
  baseName,
  version,
  ext,
}: FilenameOptionsProps) {
  const [project, setProject] = useState(
    baseName || (protocol === "WebSocket" ? "ourowebsocket" : "ourorest")
  );
  const [includeVersion, setIncludeVersion] = useState(!!version);
  const [ver, setVer] = useState(version || "");
  const [includeTimestamp, setIncludeTimestamp] = useState(true);

  const build = () => {
    const parts: string[] = [project];
    if (includeVersion && ver.trim()) parts.push(ver.trim());
    if (includeTimestamp) parts.push(String(Date.now()));
    return `${parts.join("_")}.${ext}`;
  };

  useEffect(() => {
    onChange(build());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project, includeVersion, ver, includeTimestamp, protocol, ext]);

  return (
    <div className="flex items-center gap-2 text-sm">
      <input
        value={project}
        onChange={(e) => setProject(e.target.value)}
        placeholder="프로젝트명"
        className="px-2 py-1 border rounded-md dark:bg-[#0D1117] dark:border-[#2D333B]"
      />
      <label className="flex items-center gap-1">
        <input
          type="checkbox"
          checked={includeVersion}
          onChange={(e) => setIncludeVersion(e.target.checked)}
        />
        <span>버전</span>
      </label>
      <input
        value={ver}
        onChange={(e) => setVer(e.target.value)}
        placeholder="예: v1.0.0"
        disabled={!includeVersion}
        className="px-2 py-1 border rounded-md dark:bg-[#0D1117] dark:border-[#2D333B]"
      />
      <label className="flex items-center gap-1">
        <input
          type="checkbox"
          checked={includeTimestamp}
          onChange={(e) => setIncludeTimestamp(e.target.checked)}
        />
        <span>타임스탬프</span>
      </label>
    </div>
  );
}


