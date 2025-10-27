import { useSpecStore } from "../store/spec.store";

export function SpecToolbar() {
  const { isEditing, setIsEditing } = useSpecStore();

  return (
    <div className="flex justify-end gap-3 mb-6">
      <button
        onClick={() => setIsEditing(!isEditing)}
        className={`px-4 py-2 border rounded-lg transition-colors font-medium ${
          isEditing
            ? "border-blue-500 bg-blue-500 text-white hover:bg-blue-600 dark:bg-blue-600 dark:hover:bg-blue-700"
            : "border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
        }`}
      >
        {isEditing ? "완료" : "Edit"}
      </button>
      <button className="px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
        Import YAML
      </button>
      <button className="px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
        Export Markdown
      </button>
      <button className="px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors font-medium">
        Generate api.yaml
      </button>
    </div>
  );
}
