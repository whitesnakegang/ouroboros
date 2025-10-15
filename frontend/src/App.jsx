import React, { useState, useEffect } from 'react'
import axios from 'axios'
import EndpointList from './components/EndpointList'
import EndpointEditor from './components/EndpointEditor'
import PreviewPanel from './components/PreviewPanel'
import { getStatusTemplate } from './utils/statusTemplates'
import './App.css'

/**
 * Root editor UI for the Demo API Generator that lets users view, create, edit, preview, and save API endpoints.
 *
 * Manages API definition state, selected endpoint, preview data, and loading/saving indicators; triggers network
 * requests to load and persist the API definition and to generate endpoint previews.
 *
 * @returns {JSX.Element} The rendered App component.
 */
function App() {
  const [apiDefinition, setApiDefinition] = useState({ endpoints: [] })
  const [selectedEndpoint, setSelectedEndpoint] = useState(null)
  const [previewData, setPreviewData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadApiDefinition()
  }, [])

  const loadApiDefinition = async () => {
    try {
      setLoading(true)
      const response = await axios.get('/demoapigen/api/definition')
      setApiDefinition(response.data || { endpoints: [] })
    } catch (error) {
      console.error('Failed to load API definition:', error)
      alert('Failed to load API definition')
    } finally {
      setLoading(false)
    }
  }

  const saveApiDefinition = async () => {
    try {
      setSaving(true)
      await axios.post('/demoapigen/api/definition', apiDefinition)
      alert('API definition saved successfully!')
    } catch (error) {
      console.error('Failed to save API definition:', error)
      alert('Failed to save API definition')
    } finally {
      setSaving(false)
    }
  }

  const generatePreview = async (endpoint) => {
    try {
      const response = await axios.post('/demoapigen/api/preview', endpoint)
      setPreviewData(response.data)
    } catch (error) {
      console.error('Failed to generate preview:', error)
      setPreviewData({ error: 'Failed to generate preview' })
    }
  }

  const addEndpoint = () => {
    const template200 = getStatusTemplate(200)
    const template401 = getStatusTemplate(401)

    const newEndpoint = {
      path: '/api/new-endpoint',
      method: 'GET',
      description: 'New endpoint',
      responses: [
        {
          statusCode: 200,
          response: template200.response
        },
        {
          statusCode: 401,
          response: template401.response
        }
      ]
    }
    setApiDefinition({
      ...apiDefinition,
      endpoints: [...(apiDefinition.endpoints || []), newEndpoint]
    })
    setSelectedEndpoint(newEndpoint)
  }

  const updateEndpoint = (index, updatedEndpoint) => {
    const newEndpoints = [...apiDefinition.endpoints]
    newEndpoints[index] = updatedEndpoint
    setApiDefinition({ ...apiDefinition, endpoints: newEndpoints })
    setSelectedEndpoint(updatedEndpoint)
  }

  const deleteEndpoint = (index) => {
    if (window.confirm('Are you sure you want to delete this endpoint?')) {
      const newEndpoints = apiDefinition.endpoints.filter((_, i) => i !== index)
      setApiDefinition({ ...apiDefinition, endpoints: newEndpoints })
      if (selectedEndpoint === apiDefinition.endpoints[index]) {
        setSelectedEndpoint(null)
        setPreviewData(null)
      }
    }
  }

  if (loading) {
    return <div className="loading">Loading...</div>
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>🚀 DemoApiGen Editor</h1>
        <div className="header-actions">
          <button onClick={loadApiDefinition} className="btn btn-secondary">
            Reload
          </button>
          <button onClick={saveApiDefinition} className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </header>

      <div className="app-content">
        <div className="sidebar">
          <div className="sidebar-header">
            <h2>Endpoints</h2>
            <button onClick={addEndpoint} className="btn btn-small btn-primary">
              + Add
            </button>
          </div>
          <EndpointList
            endpoints={apiDefinition.endpoints || []}
            selectedEndpoint={selectedEndpoint}
            onSelectEndpoint={setSelectedEndpoint}
            onDeleteEndpoint={deleteEndpoint}
          />
        </div>

        <div className="main-content">
          {selectedEndpoint ? (
            <EndpointEditor
              endpoint={selectedEndpoint}
              onUpdate={(updated) => {
                const index = apiDefinition.endpoints.indexOf(selectedEndpoint)
                updateEndpoint(index, updated)
              }}
              onPreview={generatePreview}
            />
          ) : (
            <div className="empty-state">
              <h2>Select an endpoint to edit</h2>
              <p>or create a new one</p>
            </div>
          )}
        </div>

        <div className="preview-panel">
          <PreviewPanel data={previewData} />
        </div>
      </div>
    </div>
  )
}

export default App