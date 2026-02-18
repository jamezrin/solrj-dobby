<div align="center">
  <img src="./docs/dobby.png" width="180" />

  # Dobby

  *Dobby has no master. Dobby is a free library.*

  A modern, extensible replacement for SolrJ's `DocumentObjectBinder`.<br/>
  Java 21 &bull; Records &bull; java.time &bull; Enums &bull; Nested docs &bull; Custom adapters

  [![CI](https://github.com/jamezrin/solrj-dobby/actions/workflows/ci.yml/badge.svg)](https://github.com/jamezrin/solrj-dobby/actions/workflows/ci.yml)
  [![Maven Central](https://img.shields.io/maven-central/v/com.jamezrin/solrj-dobby)](https://central.sonatype.com/artifact/com.jamezrin/solrj-dobby)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

</div>

---

## Why Dobby?

The name stands for **D**ocument **O**bject **B**inder, **B**ut **Y**ours - a nod to what it replaces (`DocumentObjectBinder`) and a reminder that it's yours to extend. The Harry Potter reference is just a bonus.

SolrJ's `DocumentObjectBinder` was written a long time ago, and it shows. It doesn't support records, `java.time`, enums, or `Optional`. It can't handle custom type conversions. It only allows one `child=true` field per class. It uses deprecated APIs internally and requires a no-arg constructor for everything.

Dobby fixes all of that with a clean, Gson-inspired architecture: you configure a builder, it produces an immutable instance, and a chain of type adapters handles all conversions. It's extensible, composable, and thread-safe.

| | DocumentObjectBinder | Dobby |
|---|:---:|:---:|
| Java records | | **Yes** |
| `Instant`, `LocalDate`, `ZonedDateTime` | | **Yes** |
| Enums | | **Yes** |
| `Optional<T>` | | **Yes** |
| Custom type adapters | | **Yes** |
| Multiple nested doc fields | | **Yes** |
| `Map<String, V>` dynamic fields | | **Yes** |
| Field naming strategies | | **Yes** |
| Existing `@Field` beans (zero changes) | **Yes** | **Yes** |

## Quick start

```java
// Create once, reuse everywhere (thread-safe)
Dobby dobby = Dobby.builder().build();
```

### Reading from Solr

```java
SolrDocument doc = /* from a Solr query */;

Product product = dobby.fromDoc(doc, Product.class);

// or in bulk
List<Product> products = dobby.fromDocs(response.getResults(), Product.class);
```

### Writing to Solr

```java
Product product = new Product("p1", "Widget", 19.99);

SolrInputDocument doc = dobby.toDoc(product);
solrClient.add("products", doc);

// Or convert multiple objects at once
List<Product> products = List.of(product1, product2, product3);
List<SolrInputDocument> docs = dobby.toDocs(products);
solrClient.add("products", docs);
```

## Annotating your classes

Use `@SolrField` on fields, setter methods, or record components:

```java
public class Product {
    @SolrField("id")
    public String id;

    @SolrField("name")
    public String name;

    @SolrField("price")
    public double price;

    @SolrField("created_at")
    public Instant createdAt;

    @SolrField("status")
    public Status status; // enum - stored by name

    @SolrField("tags")
    public List<String> tags;

    @SolrField(value = "variants", nested = true)
    public List<Variant> variants; // nested child documents
}
```

Records work out of the box:

```java
public record Product(
    @SolrField("id") String id,
    @SolrField("name") String name,
    @SolrField("score") double score
) {}
```

## Nested documents

Mark nested fields with `nested = true`. Dobby reads from both named field values and `getChildDocuments()`, and writes them as child documents. Unlike SolrJ, you can have multiple nested fields:

```java
public class Order {
    @SolrField("id")
    public String id;

    @SolrField(value = "items", nested = true)
    public List<LineItem> items;

    @SolrField(value = "shipping", nested = true)
    public ShippingInfo shipping; // single nested object works too
}
```

## SolrJ compatibility

Existing classes annotated with SolrJ's `@Field` work without changes - Dobby picks them up automatically. `@SolrField` takes priority when both annotations are present on the same field, so you can migrate incrementally:

```java
public class LegacyBean {
    @Field // SolrJ annotation - still works
    public String id;

    @Field("product_name")
    public String name;

    @Field(child = true)
    public List<LegacyChild> children;
}
```

To disable this, call `.enableSolrJCompat(false)` on the builder.

## Drop-in replacement for DocumentObjectBinder

If you have existing code that uses SolrJ's `DocumentObjectBinder`, you can swap it for `DobbyDocumentObjectBinder` without changing any other code:

```java
// Before:
DocumentObjectBinder binder = new DocumentObjectBinder();

// After (drop-in replacement):
DocumentObjectBinder binder = new DobbyDocumentObjectBinder();
```

This gives you Dobby's enhanced capabilities (records, java.time, enums, custom adapters) while maintaining full compatibility with the existing `DocumentObjectBinder` API.

For more control over the Dobby configuration:

```java
Dobby dobby = Dobby.builder()
    .registerAdapter(Money.class, new MoneyAdapter())
    .build();
DocumentObjectBinder binder = new DobbyDocumentObjectBinder(dobby);
```

## Custom type adapters

Register your own adapters for types Dobby doesn't handle natively:

```java
Dobby dobby = Dobby.builder()
    .registerAdapter(Money.class, new TypeAdapter<Money>() {
        @Override
        public Money read(Object solrValue) {
            return Money.parse((String) solrValue);
        }

        @Override
        public Object write(Money value) {
            return value.toString();
        }
    })
    .build();
```

User-registered adapters always take highest priority.

## Field naming strategies

When `@SolrField` has no explicit name, a naming strategy translates the Java field name:

```java
Dobby dobby = Dobby.builder()
    .fieldNamingStrategy(FieldNamingStrategy.LOWER_UNDERSCORE)
    .build();

public class Person {
    @SolrField public String firstName;  // maps to "first_name"
    @SolrField public String lastName;   // maps to "last_name"
}
```

Built-in strategies: `IDENTITY` (default), `LOWER_UNDERSCORE`, `LOWER_CASE`. Or implement your own - it's a `@FunctionalInterface`.

## Requirements

- Java 21+
- SolrJ 9.10.1 (included transitively)

## Contributing

### Building from source

Clone the repo and build with the included Gradle wrapper - no global Gradle install needed:

```bash
git clone https://github.com/jamezrin/solrj-dobby.git
cd solrj-dobby
./gradlew build
```

This compiles the library, runs tests, and checks code formatting. Java 21 is required - the Gradle toolchain will auto-download it if your `JAVA_HOME` doesn't match.

**Code formatting:** This project uses Spotless with Google Java Format. Run `./gradlew spotlessApply` to format your code before committing. The build will fail if code is not properly formatted.

### Running tests

```bash
./gradlew test
```

### Integration tests

Integration tests use Testcontainers to run against a real Solr instance in Docker:

```bash
./gradlew integrationTest
```

These tests verify end-to-end document creation and retrieval. See `src/test/java/com/jamezrin/solrj/dobby/integration/` for details.

### Project structure

```
src/main/java/com/jamezrin/solrj/dobby/
├── Dobby.java                  # Entry point
├── DobbyBuilder.java           # Builder
├── TypeAdapter.java            # Read/write abstraction
├── TypeAdapterFactory.java     # Factory interface
├── TypeToken.java              # Generic type capture
├── FieldNamingStrategy.java    # Name translation
├── BeanProperties.java         # Getter/setter introspection
├── annotation/
│   └── SolrField.java          # @SolrField annotation
├── adapter/                    # Built-in adapter factories
│   ├── PrimitiveAdapterFactory.java
│   ├── JavaTimeAdapterFactory.java
│   ├── EnumAdapterFactory.java
│   ├── CollectionAdapterFactory.java
│   ├── ArrayAdapterFactory.java
│   ├── MapAdapterFactory.java
│   ├── OptionalAdapterFactory.java
│   └── ReflectiveAdapterFactory.java
└── compat/
    ├── SolrJCompatAdapterFactory.java
    └── DobbyDocumentObjectBinder.java
```

### Guidelines

- **Tests are required.** Every bug fix or new feature should include tests. Run `./gradlew test` before submitting.
- **Keep the adapter chain design.** New type support should be added as a `TypeAdapterFactory`, not by modifying `ReflectiveAdapterFactory`. User-registered factories always take priority.
- **No new dependencies.** Dobby's only runtime dependency is SolrJ. Think twice before adding another.
- **Java 21 features welcome.** Records, sealed classes, pattern matching - use what the language gives you.
- **Follow existing style.** No IDE-specific formatting configs, just match what's already there.

### Submitting changes

1. Fork the repo and create a branch from `main`.
2. Make your changes, add tests.
3. Run `./gradlew build` - it must pass cleanly.
4. Open a pull request with a clear description of what and why.
