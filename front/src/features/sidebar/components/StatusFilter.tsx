interface StatusFilterProps {
  activeFilter: "mock" | "completed";
  onFilterChange: (filter: "mock" | "completed") => void;
}

export function StatusFilter({
  activeFilter,
  onFilterChange,
}: StatusFilterProps) {
  return (
    <div className="flex gap-2 border-b dark:border-gray-700">
      <button
        onClick={() => onFilterChange("mock")}
        className={`px-4 py-2 text-sm font-medium transition-colors ${
          activeFilter === "mock"
            ? "text-black dark:text-white border-b-2 border-black dark:border-white"
            : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        }`}
      >
        Mock
      </button>
      <button
        onClick={() => onFilterChange("completed")}
        className={`px-4 py-2 text-sm font-medium transition-colors ${
          activeFilter === "completed"
            ? "text-black dark:text-white border-b-2 border-black dark:border-white"
            : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        }`}
      >
        Completed
      </button>
    </div>
  );
}
