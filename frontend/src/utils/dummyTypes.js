export const DUMMY_TYPES = {
  // Personal Information
  FULL_NAME: { label: 'Full Name', example: 'John Doe', fakerType: 'name.fullName' },
  FIRST_NAME: { label: 'First Name', example: 'John', fakerType: 'name.firstName' },
  LAST_NAME: { label: 'Last Name', example: 'Doe', fakerType: 'name.lastName' },
  USERNAME: { label: 'Username', example: 'john_doe123', fakerType: 'internet.username' },

  // Contact
  EMAIL: { label: 'Email Address', example: 'john.doe@example.com', fakerType: 'internet.emailAddress' },
  PHONE: { label: 'Phone Number', example: '010-1234-5678', fakerType: 'phoneNumber.cellPhone' },
  MOBILE: { label: 'Mobile Number', example: '010-9876-5432', fakerType: 'phoneNumber.cellPhone' },

  // Address
  FULL_ADDRESS: { label: 'Full Address', example: '123 Main St, New York, NY 10001', fakerType: 'address.fullAddress' },
  STREET: { label: 'Street Address', example: '123 Main Street', fakerType: 'address.streetAddress' },
  CITY: { label: 'City', example: 'New York', fakerType: 'address.city' },
  STATE: { label: 'State', example: 'California', fakerType: 'address.state' },
  COUNTRY: { label: 'Country', example: 'United States', fakerType: 'address.country' },
  ZIP_CODE: { label: 'ZIP Code', example: '10001', fakerType: 'address.zipCode' },

  // Company
  COMPANY_NAME: { label: 'Company Name', example: 'Acme Corporation', fakerType: 'company.name' },
  INDUSTRY: { label: 'Industry', example: 'Technology', fakerType: 'company.industry' },
  JOB_TITLE: { label: 'Job Title', example: 'Software Engineer', fakerType: 'name.jobTitle' },

  // Internet
  URL: { label: 'URL', example: 'https://example.com', fakerType: 'internet.url' },
  DOMAIN: { label: 'Domain Name', example: 'example.com', fakerType: 'internet.domainName' },
  IP_ADDRESS: { label: 'IP Address', example: '192.168.1.1', fakerType: 'internet.ipV4Address' },
  UUID: { label: 'UUID', example: '550e8400-e29b-41d4-a716-446655440000', fakerType: 'internet.uuid' },

  // Commerce
  PRODUCT_NAME: { label: 'Product Name', example: 'Awesome Gadget', fakerType: 'commerce.productName' },
  PRICE: { label: 'Price', example: '$99.99', fakerType: 'commerce.price' },
  DEPARTMENT: { label: 'Department', example: 'Electronics', fakerType: 'commerce.department' },

  // Content
  TITLE: { label: 'Title', example: 'The Great Adventure', fakerType: 'book.title' },
  SENTENCE: { label: 'Sentence', example: 'This is a sample sentence.', fakerType: 'lorem.sentence' },
  PARAGRAPH: { label: 'Paragraph', example: 'Lorem ipsum dolor sit amet...', fakerType: 'lorem.paragraph' },
  WORD: { label: 'Word', example: 'example', fakerType: 'lorem.word' },

  // Date & Time
  DATE: { label: 'Date', example: '2024-01-15', fakerType: 'date.birthday' },

  // Other
  COLOR: { label: 'Color', example: '#3498db', fakerType: 'color.hex' },
  FILE_NAME: { label: 'File Name', example: 'document.pdf', fakerType: 'system.fileName' },
}

export const getDummyTypeOptions = () => {
  return Object.entries(DUMMY_TYPES).map(([key, value]) => ({
    value: key,
    label: value.label,
    example: value.example,
    fakerType: value.fakerType
  }))
}

export const getDummyTypeByKey = (key) => {
  return DUMMY_TYPES[key]
}
