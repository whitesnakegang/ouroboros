import React, { useState } from 'react'
import { getDummyTypeOptions, getDummyTypeByKey } from '../utils/dummyTypes'
import './FieldEditor.css'

/**
 * Render an editable list of fields with per-field type, dummy data selection, examples, and options.
 *
 * @param {Object[]} fields - Array of field definitions; each object may include `name`, `type`, `required`, `defaultValue`, and `fakerType`.
 * @param {(newFields: Object[]) => void} onChange - Called with the updated fields array when any field is added, updated, or deleted.
 * @param {boolean} [isRequestField=false] - When true, show "Required" checkbox and a default value input for each field; otherwise show a fixed default input only for string fields.
 * @returns {JSX.Element} The FieldEditor component UI.
 */
function FieldEditor({ fields, onChange, isRequestField = false }) {
  const [selectedDummy, setSelectedDummy] = useState({})
  const dummyOptions = getDummyTypeOptions()

  const addField = () => {
    onChange([...fields, { name: '', type: 'string' }])
  }

  const updateField = (index, updates) => {
    const newFields = [...fields]
    newFields[index] = { ...newFields[index], ...updates }
    onChange(newFields)
  }

  const deleteField = (index) => {
    onChange(fields.filter((_, i) => i !== index))
  }

  const handleDummyChange = (index, dummyKey) => {
    setSelectedDummy({ ...selectedDummy, [index]: dummyKey })

    if (dummyKey === 'none') {
      updateField(index, { type: 'string', fakerType: undefined })
    } else {
      const dummyType = getDummyTypeByKey(dummyKey)
      if (dummyType) {
        updateField(index, {
          type: 'faker',
          fakerType: dummyType.fakerType
        })
      }
    }
  }

  const getDummyExample = (index) => {
    const dummyKey = selectedDummy[index]
    if (!dummyKey || dummyKey === 'none') return null
    const dummyType = getDummyTypeByKey(dummyKey)
    return dummyType ? dummyType.example : null
  }

  return (
    <div className="field-editor">
      {fields.map((field, index) => (
        <div key={index} className="field-item">
          <div className="field-row">
            <input
              type="text"
              value={field.name || ''}
              onChange={(e) => updateField(index, { name: e.target.value })}
              placeholder="Field name (e.g., email)"
              className="field-name-input"
            />
            <select
              value={field.type || 'string'}
              onChange={(e) => {
                updateField(index, { type: e.target.value })
                if (e.target.value !== 'string' && e.target.value !== 'faker') {
                  setSelectedDummy({ ...selectedDummy, [index]: 'none' })
                }
              }}
              className="field-type-select"
            >
              <option value="string">string</option>
              <option value="number">number</option>
              <option value="boolean">boolean</option>
              <option value="object">object</option>
              <option value="array">array</option>
              <option value="file">file</option>
            </select>

            {(field.type === 'string' || field.type === 'faker') && (
              <select
                value={selectedDummy[index] || 'none'}
                onChange={(e) => handleDummyChange(index, e.target.value)}
                className="field-dummy-select"
              >
                <option value="none">Auto-detect from name</option>
                <optgroup label="Personal">
                  <option value="FULL_NAME">Full Name</option>
                  <option value="FIRST_NAME">First Name</option>
                  <option value="LAST_NAME">Last Name</option>
                  <option value="USERNAME">Username</option>
                </optgroup>
                <optgroup label="Contact">
                  <option value="EMAIL">Email Address</option>
                  <option value="PHONE">Phone Number</option>
                  <option value="MOBILE">Mobile Number</option>
                </optgroup>
                <optgroup label="Address">
                  <option value="FULL_ADDRESS">Full Address</option>
                  <option value="STREET">Street Address</option>
                  <option value="CITY">City</option>
                  <option value="STATE">State</option>
                  <option value="COUNTRY">Country</option>
                  <option value="ZIP_CODE">ZIP Code</option>
                </optgroup>
                <optgroup label="Company">
                  <option value="COMPANY_NAME">Company Name</option>
                  <option value="INDUSTRY">Industry</option>
                  <option value="JOB_TITLE">Job Title</option>
                </optgroup>
                <optgroup label="Internet">
                  <option value="URL">URL</option>
                  <option value="DOMAIN">Domain Name</option>
                  <option value="IP_ADDRESS">IP Address</option>
                  <option value="UUID">UUID</option>
                </optgroup>
                <optgroup label="Commerce">
                  <option value="PRODUCT_NAME">Product Name</option>
                  <option value="PRICE">Price</option>
                  <option value="DEPARTMENT">Department</option>
                </optgroup>
                <optgroup label="Content">
                  <option value="TITLE">Title</option>
                  <option value="SENTENCE">Sentence</option>
                  <option value="PARAGRAPH">Paragraph</option>
                  <option value="WORD">Word</option>
                </optgroup>
                <optgroup label="Other">
                  <option value="DATE">Date</option>
                  <option value="COLOR">Color</option>
                  <option value="FILE_NAME">File Name</option>
                </optgroup>
              </select>
            )}

            <button
              onClick={() => deleteField(index)}
              className="field-delete-btn"
            >
              ×
            </button>
          </div>

          {getDummyExample(index) && (
            <div className="field-example">
              Example: <code>{getDummyExample(index)}</code>
            </div>
          )}

          {isRequestField ? (
            <div className="field-options">
              <label className="field-checkbox">
                <input
                  type="checkbox"
                  checked={field.required || false}
                  onChange={(e) => updateField(index, { required: e.target.checked })}
                />
                Required
              </label>

              <input
                type="text"
                value={field.defaultValue || ''}
                onChange={(e) => updateField(index, { defaultValue: e.target.value })}
                placeholder="Default value (optional)"
                className="field-default-input"
              />
            </div>
          ) : (
            field.type === 'string' && (
              <div className="field-options">
                <input
                  type="text"
                  value={field.defaultValue || ''}
                  onChange={(e) => updateField(index, { defaultValue: e.target.value })}
                  placeholder="Fixed value (leave empty for random)"
                  className="field-default-input"
                  style={{ flex: 1 }}
                />
              </div>
            )
          )}
        </div>
      ))}
      <button onClick={addField} className="btn btn-small btn-secondary">
        + Add Field
      </button>
    </div>
  )
}

export default FieldEditor