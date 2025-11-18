# 🎬 Ouroboros User Guide

This guide demonstrates key workflows in Ouroboros through animated GIFs and step-by-step instructions.

**English** | [한국어](./ko/USER_GUIDE.md)

---

## 📋 Table of Contents

- [REST API Workflows](#rest-api-workflows)
  - [Creating and Using Schemas](#creating-and-using-schemas)
  - [Setting Authentication Before Running Tests](#setting-authentication-before-running-tests)
  - [Viewing Method Test Results](#viewing-method-test-results)
- [WebSocket/STOMP Workflows](#websocketstomp-workflows)
  - [Creating Schema → Message → Operation](#creating-schema--message--operation)

---

## REST API Workflows

### Creating and Using Schemas

**Workflow**: Create a reusable schema and use it in request/response definitions.

![REST API Workflow](./images/gif/rest-work-flow.gif)

**Steps**:
1. Navigate to the **Schemas** tab
2. Define schema properties (name, type, fields)
3. Set mock expressions for fields using DataFaker syntax
4. Save the schema
5. When creating/editing an API, reference the schema in request body or response to set fields

**Benefits**:
- ✅ Eliminate duplication by reusing schemas across multiple APIs
- ✅ Maintain consistency across your API specifications
- ✅ Update once, reflect everywhere

---

### Setting Authentication Before Running Tests

**Workflow**: Configure authentication settings before executing API tests.

![Authentication Setup](./images/gif/auth-setup.gif)

**Steps**:
1. Navigate to the **Try/Test** tab for your API
2. Click on **"Authentication"** or **"Auth"** button
3. Enter authentication values
4. Save authentication settings
5. Authentication will be automatically included in all test requests

**Benefits**:
- ✅ Secure API testing without exposing credentials in code
- ✅ Reusable authentication settings across multiple tests
- ✅ Support for various authentication methods

---

### Viewing Method Test Results

**Workflow**: Execute API tests and view detailed method-level performance results.

![Method Test Results](./images/gif/method-test-results.gif)

**Steps**:
1. Navigate to the **API Test** tab for your API
2. Fill in request parameters, headers, and body (if needed)
3. Click **"Run"** button
4. View the response in the **Response** panel
5. Navigate to the **"TEST"** tab
6. View method-level execution times:
   - Total request time
   - Service method execution times
7. Identify performance bottlenecks and slow methods

**Benefits**:
- ✅ Identify performance bottlenecks in your API
- ✅ Track method execution times
- ✅ Detect N+1 query problems
- ✅ Optimize slow database queries

---

## WebSocket/STOMP Workflows

### Creating Schema → Message → Operation

**Workflow**: Create a complete WebSocket API specification by building schemas, messages, and operations in sequence.

![WebSocket Workflow](./images/gif/websocket-workflow.gif)

**Steps**:

#### 1. Create Schema
1. Navigate to **WebSocket** → **Schemas** tab
2. Define schema properties
3. Save the schema

#### 2. Create Message
1. Navigate to **WebSocket** → **Messages** tab
2. Set message name
3. Configure payload and header
4. Save the message

#### 3. Create Operation
1. Navigate to **WebSocket** → **receive**, **reply** tab
2. Enter **address** and select **message**
   - When entering **address**, use full address like `/app/chat/send`
3. Save

> **⚠️ Important Notes**:
> - **Channel address format**: When writing the channel address in the specification, you must include the full path with prefix. For example, if your `@MessageMapping` annotation is `/chat/send` and your application destination prefix is `/app`, write `/app/chat/send` in the specification.
> - **Code scanning requirements**: Only methods annotated with `@MessageMapping` and `@SendTo` are scanned by Springwolf. Methods without these annotations will not be included in automatic code scanning.

**Complete Flow**:
```
Schema (Data Structure)
    ↓
Message (Message Definition with Schema Reference)
    ↓
Operation (Send/Receive Action with Message)
```

**Benefits**:
- ✅ Structured approach to WebSocket API design
- ✅ Reusable schemas and messages across operations
- ✅ Clear separation of concerns (data, message, action)
- ✅ Easy to maintain and update

---

## 🎯 Tips & Best Practices

### Schema Design
- **Start with schemas**: Define your data structures first before creating APIs
- **Use descriptive names**: Schema names should clearly indicate their purpose
- **Set mock expressions early**: Configure mock data generation during schema creation (use the Mock Expression field in the schema editor)

### Authentication
- **Test authentication**: Verify authentication works before running full API tests
- **Rotate credentials**: Regularly update authentication tokens for security

### Testing
- **Monitor performance**: Regularly check method execution times
- **Compare mock vs real**: Use mock responses to validate frontend, then test with real backend

### WebSocket Development
- **Design first**: Plan your WebSocket API structure before implementation
- **Reuse components**: Leverage schemas and messages across multiple operations
- **Include prefix in channel addresses**: Always write the full path with application destination prefix (e.g., `/app/chat/send` not `/chat/send`)
- **Use required annotations**: Add `@MessageMapping` and `@SendTo` annotations to enable code scanning

---

## 📚 Related Documentation

- [Quick Start Guide](../README.md#-quick-start)
- [API Documentation](./backend/docs/endpoints/README.md)
- [Try Feature Setup](./OUROBOROS_TRY_SETUP.md)
- [Troubleshooting](./backend/docs/troubleshooting/README.md)

---

## 💬 Need Help?

- 📝 [Create an Issue](https://github.com/whitesnakegang/ouroboros/issues/new)
- 💬 [Join Discussion](https://github.com/whitesnakegang/ouroboros/discussions)

---

<div align="center">

**Happy API Development with Ouroboros! 🐍**

Made with ❤️ by [Whitesnakegang](https://github.com/whitesnakegang)

</div>

