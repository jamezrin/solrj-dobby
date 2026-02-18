# Integration Testing

This project includes integration tests that use Testcontainers to run a real Apache Solr instance in Docker for end-to-end testing.

See [Testcontainers 2.0 documentation](https://java.testcontainers.org/) for more details.

## Prerequisites

- Docker must be installed and running
- Your user must have permission to access Docker (usually by being in the `docker` group)
- Docker socket must be accessible at `/var/run/docker.sock`

## Running Integration Tests

### Run all integration tests:

```bash
./gradlew integrationTest
```

### Run a specific integration test:

```bash
./gradlew integrationTest --tests "DocumentCrudIntegrationTest"
```

## Test Structure

### Base Class

`AbstractSolrIntegrationTest` provides the foundation for all integration tests:

- Spins up a Solr 9 container using Testcontainers
- Provides a pre-configured `SolrClient` for direct Solr operations
- Provides a pre-configured `Dobby` instance for document conversion
- Handles cleanup between tests

### Available Test Classes

1. **DocumentCrudIntegrationTest** - Tests basic CRUD operations:
   - Create and retrieve single documents
   - Batch operations with multiple documents
   - Complex types (dates, lists, primitives)
   - Document updates and deletes
   - Query with filters

2. **SolrJCompatibilityIntegrationTest** - Tests SolrJ compatibility:
   - Comparison with standard DocumentObjectBinder
   - DobbyDocumentObjectBinder integration
   - Field naming strategies
   - Various field types
