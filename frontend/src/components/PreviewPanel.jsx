import React from 'react'
import './PreviewPanel.css'

/**
 * Render a preview panel that shows pretty-printed JSON for the given data or a placeholder when no data is provided.
 * @param {Object|Array|any} data - The value to display; when present it is formatted with two-space indentation using `JSON.stringify`.
 * @returns {JSX.Element} The preview panel React element.
 */
function PreviewPanel({ data }) {
  return (
    <div className="preview-panel-content">
      <div className="preview-header">
        <h3>Preview</h3>
      </div>
      <div className="preview-body">
        {data ? (
          <pre className="preview-json">
            {JSON.stringify(data, null, 2)}
          </pre>
        ) : (
          <div className="preview-empty">
            Click "Preview" to see dummy data
          </div>
        )}
      </div>
    </div>
  )
}

export default PreviewPanel