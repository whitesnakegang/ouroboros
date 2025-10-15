import React, { useState, useEffect } from 'react'
import FieldEditor from './FieldEditor'
import { getAvailableStatusCodes, getStatusTemplate } from '../utils/statusTemplates'
import './EndpointEditor.css'

/**
 * Renders a form-based editor for configuring an API endpoint and propagates edits to the parent.
 *
 * The component maintains a local editable copy of the provided `endpoint` prop, updates that local
 * state as the user edits fields, and calls `onUpdate` with the updated endpoint on every change.
 * It also supports previewing the current local endpoint via `onPreview`.
 *
 * @param {Object} props
 * @param {Object} props.endpoint - Initial endpoint configuration to edit (method, path, description, requiresAuth, authType, authHeader, responses, request, etc.).
 * @param {(updated: Object) => void} props.onUpdate - Callback invoked whenever the endpoint configuration changes; receives the updated endpoint object.
 * @param {(snapshot: Object) => void} props.onPreview - Callback invoked when the user requests a preview; receives the current local endpoint snapshot.
 * @returns {JSX.Element} The EndpointEditor React element.
 */
function EndpointEditor({ endpoint, onUpdate, onPreview }) {
  const [localEndpoint, setLocalEndpoint] = useState(endpoint)

  useEffect(() => {
    setLocalEndpoint(endpoint)
  }, [endpoint])

  const handleChange = (field, value) => {
    const updated = { ...localEndpoint, [field]: value }
    setLocalEndpoint(updated)
    onUpdate(updated)
  }

  const handleResponsesChange = (responses) => {
    const updated = { ...localEndpoint, responses }
    setLocalEndpoint(updated)
    onUpdate(updated)
  }

  const addStatusResponse = () => {
    const newResponses = localEndpoint.responses || []
    const template = getStatusTemplate(200)
    const newStatusResponse = {
      statusCode: 200,
      response: template.response
    }
    handleResponsesChange([...newResponses, newStatusResponse])
  }

  const updateStatusResponse = (index, updates) => {
    const newResponses = [...(localEndpoint.responses || [])]
    newResponses[index] = { ...newResponses[index], ...updates }
    handleResponsesChange(newResponses)
  }

  const deleteStatusResponse = (index) => {
    const newResponses = (localEndpoint.responses || []).filter((_, i) => i !== index)
    handleResponsesChange(newResponses)
  }

  return (
    <div className="endpoint-editor">
      <div className="editor-header">
        <h2>Endpoint Configuration</h2>
        <button
          className="btn btn-primary"
          onClick={() => onPreview(localEndpoint)}
        >
          Preview
        </button>
      </div>

      <div className="editor-content">
        <div className="form-group">
          <label>Method</label>
          <select
            value={localEndpoint.method || 'GET'}
            onChange={(e) => handleChange('method', e.target.value)}
          >
            <option>GET</option>
            <option>POST</option>
            <option>PUT</option>
            <option>DELETE</option>
            <option>PATCH</option>
          </select>
        </div>

        <div className="form-group">
          <label>Path</label>
          <input
            type="text"
            value={localEndpoint.path || ''}
            onChange={(e) => handleChange('path', e.target.value)}
            placeholder="/api/example"
          />
        </div>

        <div className="form-group">
          <label>Description</label>
          <input
            type="text"
            value={localEndpoint.description || ''}
            onChange={(e) => handleChange('description', e.target.value)}
            placeholder="Endpoint description"
          />
        </div>

        <div className="form-group">
          <label>
            <input
              type="checkbox"
              checked={localEndpoint.requiresAuth || false}
              onChange={(e) => handleChange('requiresAuth', e.target.checked)}
              style={{ marginRight: '0.5rem' }}
            />
            Requires Authentication
          </label>
        </div>

        {localEndpoint.requiresAuth && (
          <>
            <div className="form-group">
              <label>Authentication Type</label>
              <select
                value={localEndpoint.authType || 'bearer'}
                onChange={(e) => handleChange('authType', e.target.value)}
              >
                <option value="bearer">Bearer Token</option>
                <option value="basic">Basic Auth</option>
                <option value="apikey">API Key</option>
                <option value="custom">Custom Header</option>
              </select>
            </div>

            {localEndpoint.authType === 'custom' && (
              <div className="form-group">
                <label>Custom Header Name</label>
                <input
                  type="text"
                  value={localEndpoint.authHeader || ''}
                  onChange={(e) => handleChange('authHeader', e.target.value)}
                  placeholder="X-Auth-Token"
                />
              </div>
            )}
          </>
        )}

        <div className="form-group">
          <label>Responses</label>
          <div className="responses-section">
            {(localEndpoint.responses || []).map((statusResp, index) => (
              <div key={index} className="status-response-item">
                <div className="status-response-header">
                  <div className="status-code-selector">
                    <label>Status Code</label>
                    <select
                      value={statusResp.statusCode || 200}
                      onChange={(e) => {
                        const value = e.target.value
                        if (value === 'custom') {
                          updateStatusResponse(index, { statusCode: '' })
                        } else {
                          const code = parseInt(value)
                          const template = getStatusTemplate(code)
                          updateStatusResponse(index, { statusCode: code, response: template.response })
                        }
                      }}
                    >
                      <option value="200">200 - OK</option>
                      <option value="201">201 - Created</option>
                      <option value="400">400 - Bad Request</option>
                      <option value="401">401 - Unauthorized</option>
                      <option value="403">403 - Forbidden</option>
                      
                      <option value="custom">기타 (직접 입력)</option>
                    </select>
                    {![200, 201, 400, 401, 403].includes(statusResp.statusCode) && (
                      <input
                        type="number"
                        value={statusResp.statusCode || ''}
                        onChange={(e) => updateStatusResponse(index, { statusCode: parseInt(e.target.value) })}
                        placeholder="Status code"
                        min="100"
                        max="599"
                        className="custom-status-input"
                      />
                    )}
                  </div>
                  <button
                    onClick={() => deleteStatusResponse(index)}
                    className="btn btn-small btn-danger"
                  >
                    삭제
                  </button>
                </div>

                <div className="response-type-selector">
                  <label>Response Type</label>
                  <select
                    value={statusResp.response?.type || 'object'}
                    onChange={(e) => {
                      const newResponse = { ...statusResp.response, type: e.target.value }
                      if (e.target.value === 'array' && !newResponse.arrayItemType) {
                        newResponse.arrayItemType = { type: 'object', fields: [] }
                      }
                      updateStatusResponse(index, { response: newResponse })
                    }}
                  >
                    <option>object</option>
                    <option>array</option>
                    <option>string</option>
                    <option>number</option>
                    <option>boolean</option>
                  </select>
                </div>

                {statusResp.response?.type === 'object' && (
                  <div className="response-fields">
                    <label>Response Fields</label>
                    <FieldEditor
                      fields={statusResp.response.fields || []}
                      onChange={(fields) => {
                        updateStatusResponse(index, {
                          response: { ...statusResp.response, fields }
                        })
                      }}
                    />
                  </div>
                )}

                {statusResp.response?.type === 'array' && (
                  <div className="response-fields">
                    <label>Array Item Type</label>
                    <select
                      value={statusResp.response.arrayItemType?.type || 'object'}
                      onChange={(e) => {
                        const newArrayItemType = { type: e.target.value, fields: [] }
                        updateStatusResponse(index, {
                          response: {
                            ...statusResp.response,
                            arrayItemType: newArrayItemType
                          }
                        })
                      }}
                    >
                      <option>object</option>
                      <option>string</option>
                      <option>number</option>
                      <option>boolean</option>
                    </select>

                    {statusResp.response.arrayItemType?.type === 'object' && (
                      <FieldEditor
                        fields={statusResp.response.arrayItemType.fields || []}
                        onChange={(fields) => {
                          updateStatusResponse(index, {
                            response: {
                              ...statusResp.response,
                              arrayItemType: { ...statusResp.response.arrayItemType, fields }
                            }
                          })
                        }}
                      />
                    )}
                  </div>
                )}
              </div>
            ))}
            <button onClick={addStatusResponse} className="btn btn-small btn-secondary">
              + Add Status Response
            </button>
          </div>
        </div>

        <div className="form-group">
          <label>Request Type</label>
          <select
            value={localEndpoint.request?.type || 'none'}
            onChange={(e) => {
              const newRequest = { type: e.target.value, contentType: 'json', fields: [] }
              handleChange('request', newRequest)
            }}
          >
            <option value="none">None</option>
            <option value="query">Query Parameters</option>
            <option value="body">Request Body</option>
          </select>
        </div>

        {localEndpoint.request?.type === 'body' && (
          <div className="form-group">
            <label>Content Type</label>
            <select
              value={localEndpoint.request?.contentType || 'json'}
              onChange={(e) => {
                handleChange('request', { ...localEndpoint.request, contentType: e.target.value })
              }}
            >
              <option value="json">application/json</option>
              <option value="formData">multipart/form-data</option>
            </select>
          </div>
        )}

        {(localEndpoint.request?.type === 'body' || localEndpoint.request?.type === 'query') && (
          <div className="form-group">
            <label>Request Fields</label>
            <FieldEditor
              fields={localEndpoint.request?.fields || []}
              onChange={(fields) => {
                handleChange('request', { ...localEndpoint.request, fields })
              }}
              isRequestField={true}
            />
          </div>
        )}

      </div>
    </div>
  )
}

export default EndpointEditor