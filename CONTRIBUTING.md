# Contributing Guide

Thank you for your interest in contributing to the Ouroboros project! This document guides you on how to contribute to the project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Branch Strategy](#branch-strategy)
- [Commit Message Rules](#commit-message-rules)
- [Code Style Guide](#code-style-guide)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Community](#community)

---

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

---

## Getting Started

### How Can You Help?

Ouroboros welcomes various types of contributions:

- 🐛 **Bug Reports**: Report bugs you've discovered
- 💡 **Feature Suggestions**: Suggest new feature ideas
- 📝 **Documentation**: Fix typos and improve documentation
- 💻 **Code Contributions**: Fix bugs and implement new features
- 🌐 **Translations**: Translate documentation into other languages
- 🧪 **Testing**: Write and improve test cases
- 🎨 **UI/UX**: Improve frontend design and usability

### Finding Good First Issues

- **Good First Issue**: Suitable issues for first-time contributors
- **Help Wanted**: Issues that need help
- **Bug**: Issues requiring bug fixes
- **Enhancement**: New features or improvements

[View Issues](https://github.com/whitesnakegang/ouroboros/issues)

---

## How to Contribute

### 1. Fork & Clone

```bash
# 1. Fork the repository on GitHub
# 2. Clone your fork locally
git clone https://github.com/YOUR_USERNAME/ouroboros.git
cd ouroboros

# 3. Add the original repository as upstream
git remote add upstream https://github.com/whitesnakegang/ouroboros.git

# 4. Fetch the latest changes
git fetch upstream
```

### 2. Create a Branch

```bash
# Create a new branch from develop
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

### 3. Develop and Commit

```bash
# Add changes
git add .

# Commit following the commit message rules
git commit -m "feat: add new feature"
```

### 4. Push and Pull Request

```bash
# Push to your fork
git push origin feature/your-feature-name

# Create a Pull Request on GitHub
```

---

## Development Setup

### Backend (Spring Boot)

#### Requirements
- ☕ Java 17 or higher
- 📦 Gradle 8.14.3+ (Wrapper included)
- 🍃 Spring Boot 3.5.7

#### Setup and Run

```bash
cd backend

# Build
./gradlew build

# Run tests
./gradlew test

# Run application
./gradlew bootRun

# Publish to local Maven repository (for testing)
./gradlew publishToMavenLocal
```

### Frontend (React + TypeScript)

#### Requirements
- 📦 Node.js 18 or higher
- 📦 npm or yarn

#### Setup and Run

```bash
cd front

# Install dependencies
npm install

# Run development server
npm run dev

# Build
npm run build

# Lint check
npm run lint
```

---

## Branch Strategy

We use the **Git Flow** strategy.

### Branch Types

| Branch | Purpose | Base Branch |
|--------|---------|-------------|
| `main` | Production deployment branch | - |
| `develop` | Development integration branch | `main` |
| `feature/*` | New feature development | `develop` |
| `fix/*` | Bug fixes | `develop` |
| `hotfix/*` | Emergency fixes | `main` |
| `release/*` | Release preparation | `develop` |

### Branch Naming Rules

```
feature/rest-api-spec-crud
feature/mock-server-validation
fix/yaml-parser-null-pointer
fix/duplicate-schema-detection
hotfix/security-vulnerability
release/v0.1.0
```

### Workflow

#### Feature Development
```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature
# ... develop ...
git push origin feature/your-feature
# Create PR: feature/your-feature → develop
```

#### Bug Fix
```bash
git checkout develop
git pull upstream develop
git checkout -b fix/bug-name
# ... fix ...
git push origin fix/bug-name
# Create PR: fix/bug-name → develop
```

#### Hotfix
```bash
git checkout main
git pull upstream main
git checkout -b hotfix/critical-bug
# ... fix ...
git push origin hotfix/critical-bug
# Create PR: hotfix/critical-bug → main
# Then merge main → develop
```

---

## Commit Message Rules

We follow the **Conventional Commits** style.

### Structure

```
<type>: <short description>

[optional] body

[optional] footer
```

### Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat: add YAML import feature` |
| `fix` | Bug fix | `fix: resolve null pointer in parser` |
| `docs` | Documentation | `docs: update contributing guide` |
| `style` | Code formatting (no logic change) | `style: format code with prettier` |
| `refactor` | Code refactoring | `refactor: simplify validation logic` |
| `test` | Test code | `test: add unit tests for service` |
| `chore` | Build, package manager, etc. | `chore: update dependencies` |
| `perf` | Performance improvement | `perf: optimize YAML parsing` |
| `ci` | CI configuration | `ci: add github actions workflow` |
| `build` | Build-related files | `build: update gradle version` |

### Rules

1. **Subject**: Max 50 chars, lowercase first letter, no period
2. **Body**: Wrap at 72 chars, explain **what** and **why**
3. **Footer**: Breaking changes, issue references
4. **Language**: English or Korean (be consistent)

### Examples

#### Simple Commit
```
feat: add schema validation
```

#### Detailed Commit
```
feat: add automatic schema enrichment

- Automatically add x-ouroboros-mock field
- Automatically generate x-ouroboros-orders field
- Preserve existing values if present

Closes #123
```

#### Breaking Change
```
feat!: change API response format

BREAKING CHANGE: GlobalApiResponse structure changed
- error field now returns as array instead of object
- Migration guide: docs/migration/v0.2.0.md

Closes #234
```

---

## Code Style Guide

### Java (Backend)

#### Basic Principles
- Follow **Google Java Style Guide**
- **Javadoc Required**: All public classes, methods, fields
- **Comment Language**: English

#### Javadoc Example
```java
/**
 * Service interface for REST API specification management.
 * <p>
 * Manages REST API endpoint specifications in the OpenAPI paths section.
 * Supports full CRUD operations with automatic YAML file synchronization.
 *
 * @since 0.0.1
 */
public interface RestApiSpecService {
    
    /**
     * Creates a new REST API specification.
     * <p>
     * Validates uniqueness of path+method combination and generates a UUID if not provided.
     * Writes the specification to the OpenAPI YAML file.
     *
     * @param request REST API specification details
     * @return created specification with generated ID
     * @throws Exception if specification creation fails or duplicate path+method exists
     */
    RestApiSpecResponse createRestApiSpec(CreateRestApiRequest request) throws Exception;
}
```

#### Naming Conventions
- **Classes**: `PascalCase` (e.g., `RestApiSpecService`)
- **Methods/Variables**: `camelCase` (e.g., `createRestApiSpec`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
- **Packages**: `lowercase` (e.g., `kr.co.ouroboros.core.rest`)

#### Package-info
Each package must have `package-info.java`:

```java
/**
 * REST API specification management services.
 * <p>
 * This package provides CRUD operations for managing REST API specifications
 * in OpenAPI 3.1.0 format.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.spec.service;
```

### TypeScript/React (Frontend)

#### Basic Principles
- Follow **ESLint** rules
- Use **Functional Components**
- Enable **TypeScript Strict Mode**

#### Component Example
```typescript
interface Props {
  title: string;
  onSave: (data: SpecData) => void;
}

/**
 * API specification editor form component
 */
export const SpecForm: React.FC<Props> = ({ title, onSave }) => {
  // ...
};
```

#### Naming Conventions
- **Components**: `PascalCase` (e.g., `SpecForm`)
- **Functions/Variables**: `camelCase` (e.g., `handleSubmit`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `API_BASE_URL`)
- **Types/Interfaces**: `PascalCase` (e.g., `SpecFormProps`)

---

## Testing

### Backend Tests

#### Unit Tests
```java
@SpringBootTest
class RestApiSpecServiceTest {
    
    @Test
    void createRestApiSpec_ShouldGenerateId_WhenIdNotProvided() {
        // given
        CreateRestApiRequest request = CreateRestApiRequest.builder()
            .path("/api/test")
            .method("GET")
            .build();
        
        // when
        RestApiSpecResponse response = service.createRestApiSpec(request);
        
        // then
        assertNotNull(response.getId());
    }
}
```

#### Run Tests
```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests RestApiSpecServiceTest

# Test coverage
./gradlew test jacocoTestReport
```

### Frontend Tests

#### Component Tests
```typescript
describe('SpecForm', () => {
  it('should render correctly', () => {
    const { getByText } = render(<SpecForm title="Test" />);
    expect(getByText('Test')).toBeInTheDocument();
  });
});
```

---

## Pull Request Process

### PR Checklist

Before creating a PR, ensure:

- [ ] Pulled latest code from `develop` branch
- [ ] Follow branch naming rules
- [ ] Follow commit message rules
- [ ] Follow code style guide
- [ ] Written tests for new code
- [ ] All tests pass
- [ ] Written Javadoc/JSDoc
- [ ] Updated documentation (if needed)
- [ ] No linter errors

### PR Template

```markdown
## 📝 Changes

Briefly describe what this PR changes.

## 🎯 Related Issues

Closes #issue-number

## 🧪 How to Test

Explain how to test the changes.

1. ...
2. ...

## 📸 Screenshots (for UI changes)

Attach screenshots if there are UI changes.

## 📋 Checklist

- [ ] Follows code style guide
- [ ] Tests written
- [ ] Documentation updated
- [ ] Migration guide written (if breaking changes)
```

### Review Process

1. **Create PR**: Create PR to `develop` branch
2. **Auto Checks**: CI/CD automatically runs build and tests
3. **Code Review**: At least 1 maintainer approval required
4. **Revise**: Make changes based on review feedback
5. **Merge**: Maintainer merges after approval

### Merge Strategy

- **Squash and Merge**: Feature branches use squash merge
- **Merge Commit**: Release branches use merge commit
- **Rebase and Merge**: Hotfixes use rebase merge

---

## Issue Reporting

### Bug Reports

Found a bug? Create an issue with the following information:

#### Template

```markdown
## 🐛 Bug Description

Clearly and concisely describe the bug.

## 🔄 Steps to Reproduce

Steps to reproduce the bug:
1. Go to '...'
2. Click on '...'
3. Scroll to '...'
4. See error

## ✅ Expected Behavior

Describe what you expected to happen.

## 📸 Screenshots

Attach screenshots if possible.

## 🖥️ Environment

- OS: [e.g., Windows 10]
- Java Version: [e.g., 17]
- Spring Boot Version: [e.g., 3.5.7]
- Ouroboros Version: [e.g., 0.1.0]

## 📋 Additional Information

Add any other relevant information.
```

### Feature Requests

Want to suggest a new feature?

#### Template

```markdown
## 💡 Feature Description

Describe the feature you'd like to suggest.

## 🎯 Problem

What problem does this feature solve?

## 💻 Proposed Solution

Explain how you think it should be implemented.

## 🔄 Alternatives

What alternatives have you considered?

## 📋 Additional Information

Add any other relevant information or screenshots.
```

---

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and discussions
- **Pull Requests**: Code contributions and reviews

### Asking Questions

Have questions?

1. **Check FAQ**: Check [documentation](./backend/docs/) first
2. **Search Issues**: See if the same question already exists
3. **Create Discussion**: Post new questions in Discussions

### Getting Help

- 📚 [Project Documentation](./backend/PROJECT_DOCUMENTATION.md)
- 🔧 [Troubleshooting Guide](./backend/docs/troubleshooting/README.md)
- 📖 [API Documentation](./backend/docs/endpoints/README.md)

---

## License

By contributing to this project, you agree that your contributions will be licensed under the [Apache License 2.0](./LICENSE).

---

## Thank You!

Thank you for taking the time to contribute to the Ouroboros project! Your contributions help make this project better. 🎉

Feel free to reach out if you have any questions.

**Happy Coding!** 🚀

