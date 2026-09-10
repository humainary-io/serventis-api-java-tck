# Serventis Java API Technology Compatibility Kit

Executable compatibility tests for the Serventis Java API on a Substrates provider.

The TCK does not build or install either API or the provider. Those artifacts must already be
available to Maven before the tests are started.

## Preconditions

Verify all of the following before running the TCK:

1. **Java 26 is active.** `JAVA_HOME` and `java --version` must select JDK 26.
2. **The published Humainary artifacts are Maven-resolvable.** The defaults are:
    - `io.humainary.substrates:humainary-substrates-api:3.1.2` — the Substrates API
    - `io.humainary.serventis:humainary-serventis-api:3.1.2` — the Serventis API
    - `io.humainary.specs:humainary-specs-api:3.1.2` — the `@SpecDoc` / `@SpecRef` traceability
      annotations the test sources reference. Scope `provided`: needed to compile the tests, and
      `SOURCE`-retained, so none of it reaches the run
    - `io.humainary.testkit:humainary-substrates-api-tck:3.1.2` (`test-jar`) — the shared test
      support these tests build on
3. **The Substrates provider artifact is Maven-resolvable.** You must know its Maven `groupId`,
   `artifactId`, and `version`. Configure any remote repositories and credentials in Maven settings,
   or install unpublished artifacts into the local Maven repository.
4. **The provider is discoverable at runtime.** Its class must extend
   `io.humainary.substrates.spi.CortexProvider`, have a public no-argument constructor, and be
   selected
   using one of these mechanisms:
    - Preferably, the provider artifact contains
      `META-INF/services/io.humainary.substrates.spi.CortexProvider` with exactly one provider
      class.
    - Alternatively, pass `-Dio.humainary.substrates.spi.provider=<provider-class>` when running
      Maven.
5. **Only the intended provider is selected.** If multiple provider artifacts are visible through
   `ServiceLoader`, select one explicitly with the system property above.

The Maven wrapper is included; a system Maven installation is not required. On first use, the
wrapper
must be able to download Maven 3.9.16 unless that distribution is already cached. On Windows,
replace
`./mvnw` with `mvnw.cmd`.

## Run the TCK

Run from the repository root and supply all three provider coordinates:

```sh
./mvnw clean test \
  -Dsubstrates.spi.groupId=com.example \
  -Dsubstrates.spi.artifactId=example-substrates-provider \
  -Dsubstrates.spi.version=1.0.0
```

Supplying `substrates.spi.artifactId` activates the provider dependency. The corresponding
`substrates.spi.groupId` and `substrates.spi.version` properties are therefore also required.

If the provider does not use `ServiceLoader`, select its provider class explicitly:

```sh
./mvnw clean test \
  -Dsubstrates.spi.groupId=com.example \
  -Dsubstrates.spi.artifactId=example-substrates-provider \
  -Dsubstrates.spi.version=1.0.0 \
  -Dio.humainary.substrates.spi.provider=com.example.ExampleCortexProvider
```

This TCK defaults to `3.1.2` for every Humainary artifact it resolves. Intentional overrides can be
supplied per artifact:

| Property                 | Artifact                                    |
|--------------------------|---------------------------------------------|
| `substrates.api.version` | `humainary-substrates-api`                  |
| `serventis.api.version`  | `humainary-serventis-api`                   |
| `specs.api.version`      | `humainary-specs-api`                       |
| `substrates.tck.version` | `humainary-substrates-api-tck` (`test-jar`) |

A result then applies to that combination of API versions and provider.

## Interpret the result

The default run executes 1227 tests in this revision:

- `BUILD SUCCESS`, with zero failures and zero errors, means the two APIs and configured provider
  passed as a combination.
- Any failed or errored test means the combination did not pass.
- Successful compilation alone is not a TCK pass.

Some tests may deliberately exercise failure behavior. Use the final Maven/JUnit summary as the
result.

Executable suites are named `<SurfaceOrConcern>ContractTest`.
