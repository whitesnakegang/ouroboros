import React from 'react'
import './EndpointList.css'

/**
 * Render a list of API endpoints with method badges, paths, descriptions, selection highlighting, and per-item delete controls.
 *
 * @param {{method?: string, path: string, description?: string}[]} endpoints - Array of endpoint objects to display; each may include `method`, `path`, and `description`.
 * @param {{method?: string, path: string, description?: string}|null} selectedEndpoint - The currently selected endpoint object, or null if none.
 * @param {(endpoint: {method?: string, path: string, description?: string}) => void} onSelectEndpoint - Callback invoked with an endpoint when the user selects (clicks) an item.
 * @param {(index: number) => void} onDeleteEndpoint - Callback invoked with the index of an endpoint when its delete button is clicked.
 * @returns {JSX.Element} The React element representing the endpoints list (or an empty-state message when there are no endpoints).
 */
function EndpointList({ endpoints, selectedEndpoint, onSelectEndpoint, onDeleteEndpoint }) {
  const getMethodColor = (method) => {
    const colors = {
      GET: '#61affe',
      POST: '#49cc90',
      PUT: '#fca130',
      DELETE: '#f93e3e',
      PATCH: '#50e3c2'
    }
    return colors[method?.toUpperCase()] || '#999'
  }

  return (
    <div className="endpoint-list">
      {endpoints.length === 0 ? (
        <div className="endpoint-list-empty">
          No endpoints yet. Click "Add" to create one.
        </div>
      ) : (
        endpoints.map((endpoint, index) => (
          <div
            key={index}
            className={`endpoint-item ${selectedEndpoint === endpoint ? 'selected' : ''}`}
            onClick={() => onSelectEndpoint(endpoint)}
          >
            <div className="endpoint-item-header">
              <span
                className="endpoint-method"
                style={{ background: getMethodColor(endpoint.method) }}
              >
                {endpoint.method}
              </span>
              <span className="endpoint-path">{endpoint.path}</span>
            </div>
            <div className="endpoint-item-description">
              {endpoint.description || 'No description'}
            </div>
            <button
              className="endpoint-delete"
              onClick={(e) => {
                e.stopPropagation()
                onDeleteEndpoint(index)
              }}
            >
              ×
            </button>
          </div>
        ))
      )}
    </div>
  )
}

export default EndpointList