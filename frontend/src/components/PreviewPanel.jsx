import React from 'react'
import './PreviewPanel.css'

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
