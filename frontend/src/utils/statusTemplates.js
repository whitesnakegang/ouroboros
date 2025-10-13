export const STATUS_CODE_TEMPLATES = {
  200: {
    label: '200 - OK',
    description: 'Standard success response',
    response: {
      type: 'object',
      fields: []
    }
  },
  201: {
    label: '201 - Created',
    description: 'Resource created successfully',
    response: {
      type: 'object',
      fields: [
        { name: 'id', type: 'string' },
        { name: 'createdAt', type: 'string' }
      ]
    }
  },
  400: {
    label: '400 - Bad Request',
    description: 'Invalid request parameters',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string', defaultValue: 'Bad Request' },
        { name: 'message', type: 'string', defaultValue: 'Invalid request parameters' }
      ]
    }
  },
  401: {
    label: '401 - Unauthorized',
    description: 'Authentication required',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string', defaultValue: 'Unauthorized' },
        { name: 'message', type: 'string', defaultValue: 'Authentication required' }
      ]
    }
  },
  403: {
    label: '403 - Forbidden',
    description: 'Insufficient permissions',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string', defaultValue: 'Forbidden' },
        { name: 'message', type: 'string', defaultValue: 'Insufficient permissions' }
      ]
    }
  },
  404: {
    label: '404 - Not Found',
    description: 'Resource not found',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string' },
        { name: 'message', type: 'string' },
        { name: 'resource', type: 'string' }
      ]
    }
  },
  500: {
    label: '500 - Internal Server Error',
    description: 'Server error occurred',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string' },
        { name: 'message', type: 'string' },
        { name: 'timestamp', type: 'string' }
      ]
    }
  },
  502: {
    label: '502 - Bad Gateway',
    description: 'Invalid response from upstream',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string' },
        { name: 'message', type: 'string' }
      ]
    }
  },
  503: {
    label: '503 - Service Unavailable',
    description: 'Service temporarily unavailable',
    response: {
      type: 'object',
      fields: [
        { name: 'error', type: 'string' },
        { name: 'message', type: 'string' },
        { name: 'retryAfter', type: 'number' }
      ]
    }
  }
}

export const getStatusTemplate = (statusCode) => {
  return STATUS_CODE_TEMPLATES[statusCode] || {
    label: `${statusCode} - Custom`,
    description: 'Custom status code',
    response: {
      type: 'object',
      fields: []
    }
  }
}

// Codes we don't want to expose in the UI (handled globally)
export const HIDDEN_STATUS_CODES = [404, 500, 502, 503]

export const getAvailableStatusCodes = () => {
  return Object.keys(STATUS_CODE_TEMPLATES)
    .map(code => ({
      code: parseInt(code),
      ...STATUS_CODE_TEMPLATES[code]
    }))
    .filter(({ code }) => !HIDDEN_STATUS_CODES.includes(code))
}
