# openapi-generator Maven Plugin

Generates framework-specific HTTP client interfaces and model records from an OpenAPI 3
document.

For every tag in the document the plugin produces one interface (`PetsApi`, `OwnersApi`, …)
into `<basePackage>.api`, and for every schema under `components/schemas` one record into
`<basePackage>.model`. Both YAML and JSON documents are supported.

Generation happens in two stages. First the interfaces are emitted carrying the plugin's own
marker annotations — `@ApiInterface`, `@ApiAnnotation` and `@ApiParam` — which describe the
tag, the HTTP method, the path, the produced media types and the location of every
parameter. Then a **transformation** rewrites those markers into a real framework's
annotations using OpenRewrite.

The intermediate marker-annotated sources are never written to disk. They exist only in
memory between the two stages, so nothing can compile them by accident and your project does
not need this plugin on its compile path.

## Usage

### Adding the plugin

The goal is `generate` and it is bound to the `generate-sources` phase by default, so
declaring the execution without a `<phase>` is enough. At least one `<transformation>` is
required:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>hi.mynameis.ilnano</groupId>
            <artifactId>openapi-generator</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>generate</id>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <transformations>
                            <transformation>
                                <flavour>QUARKUS</flavour>
                            </transformation>
                        </transformations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

You then add whichever framework the transformation targets as an ordinary dependency — see
the two sections below. The plugin itself is **not** needed as a dependency: its markers are
gone by the time anything is written.

### Configuration

| Parameter | Property | Default | Description |
|---|---|---|---|
| `specDefinition` | `specDefinition` | `${project.basedir}/src/main/resources/openapi.yaml` | Path or URL of the OpenAPI document. |
| `outputFolder` | `outputFolder` | `${project.build.directory}/generated-sources/<spec name>` | Directory the sources are written to. |
| `basePackage` | `basePackage` | `hi.mynameis.ilnano.generated` | Root package; `.api` and `.model` are appended. |
| `transformations` | — | *(none — mandatory)* | What to rewrite the generated sources into. |

By default the output goes into `target/generated-sources/` plus a directory named after the
OpenAPI document, so `src/main/resources/pet-store.yaml` produces
`target/generated-sources/pet-store/`. Each transformation then gets its own subdirectory
named after its flavour, so a `QUARKUS` transformation writes into
`target/generated-sources/pet-store/quarkus/`.

Every directory written is registered as a compile source root, so the generated code is
compiled into your artifact without any extra `build-helper` configuration.

## The transformations block

`<transformations>` holds one or more `<transformation>` entries. It is empty by default, and
**at least one entry is mandatory** — the marker-annotated sources are not useful on their
own, so a build that configures none fails rather than writing something nothing can compile:

```
[ERROR] Failed to execute goal ...:generate (generate) on project my-app:
        At least one <transformation> must be configured inside <transformations>.
        Valid flavours are QUARKUS, SPRING.
```

Each entry accepts:

| Element | Required | Default | Description |
|---|---|---|---|
| `flavour` | yes | — | `QUARKUS` or `SPRING`. Case-insensitive. |
| `basePackage` | no | the mojo's `basePackage` | Root package for this transformation's output. |
| `outputFolder` | no | `<output folder>/<flavour>` | Where this transformation writes. |
| `group` | no | the last segment of the package | Client group name. `SPRING` only. |

### `QUARKUS` — MicroProfile REST clients

Rewrites the interfaces into `jakarta.ws.rs` annotations registered with MicroProfile's
`@RegisterRestClient`, which is what a Quarkus project using `quarkus-rest-client-jackson`
expects:

```java
@RegisterRestClient(configKey = "books-api")
@Path("/books")
public interface BooksApi {
    @GET
    @Produces("application/json")
    BookPage listBooks(@QueryParam("q") String q, @HeaderParam("X-Request-Id") UUID xRequestId);

    @GET
    @Path("/{isbn}")
    @Produces({"application/json", "application/xml"})
    Book getBook(@PathParam("isbn") String isbn);

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    Book createBook(BookRequest body);
}
```

The `configKey` is derived from the tag in kebab-case, so the tag `Books` gives `books-api`
and the base URL is configured as:

```properties
quarkus.rest-client.books-api.url=https://api.bookstore.example/v2
```

Parameters map to `@PathParam`, `@QueryParam`, `@HeaderParam` and `@CookieParam` by their
location in the spec. A request body carries no annotation at all, because in JAX-RS the
single unannotated parameter *is* the entity.

Dependencies needed:

```xml
<dependency>
    <groupId>jakarta.ws.rs</groupId>
    <artifactId>jakarta.ws.rs-api</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>org.eclipse.microprofile.rest.client</groupId>
    <artifactId>microprofile-rest-client-api</artifactId>
    <version>4.0</version>
</dependency>
```

In a real Quarkus project the `quarkus-rest-client-jackson` extension brings both in, so
declaring them separately is not needed.

#### Verbs JAX-RS has no annotation for

`jakarta.ws.rs` ships `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE`, `@HEAD` and `@OPTIONS`,
but not `@TRACE`. `jakarta.ws.rs.HttpMethod` cannot stand in, because it targets
`ANNOTATION_TYPE` — it declares a verb rather than annotating a method. So when your spec
uses such a verb, the transformation generates the annotation next to the interfaces:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("TRACE")
public @interface TRACE {
}
```

### `SPRING` — Spring Boot 4 HTTP service clients

Rewrites the interfaces into `@HttpExchange` clients, as described in
[Spring Boot 4 HTTP service clients](https://ankurm.com/spring-boot-4-http-service-clients):

```java
@HttpExchange("/orders")
public interface OrdersApi {
    @PostExchange(accept = "application/json")
    Order placeOrder(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody OrderRequest body);

    @GetExchange(value = "/{orderId}", accept = "application/json")
    Order getOrder(@PathVariable UUID orderId, @CookieValue(required = false) String session);
}
```

Spring registers HTTP clients from a configuration class rather than by scanning the
interfaces, so the transformation also writes one:

```java
@ImportHttpServices(group = "bookstore", basePackages = "com.bookstore.client.spring.api")
@Configuration(proxyBeanMethods = false)
public class HttpServiceClientConfig {
}
```

The group is what keys the base URL, and can be set with `<group>`:

```properties
spring.http.client.service.group.bookstore.base-url=https://api.bookstore.example/v2
spring.http.client.service.read-timeout=10s
```

Make sure the generated package is inside your application's component-scan path, otherwise
the configuration class is never picked up and no client beans are created.

Parameters map to `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@CookieValue` and
`@RequestBody`. Since Spring binds by parameter name, the wire name is only spelled out when
it differs from the Java one — `X-Request-Id` becomes
`@RequestHeader("X-Request-Id") UUID xRequestId`, while `limit` needs no name at all. Optional
parameters get `required = false`.

Dependencies needed (Spring Framework 7 / Spring Boot 4):

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>7.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>7.0.0</version>
</dependency>
```

`spring-boot-starter-web` brings both in. Note that `@RequestMapping` is deliberately not
used: the client registry only recognises `@HttpExchange` and its per-verb variants.

### How paths are split

The path prefix shared by every operation of a tag is hoisted onto the interface, and each
method keeps only the remainder. Given `/books`, `/books/{isbn}` and `/books/{isbn}/cover`,
the interface carries `/books` and the methods carry nothing, `/{isbn}` and `/{isbn}/cover`.

A tag with a single operation is the exception: it keeps its full path on the method, since
hoisting all of it would leave the method with no path at all.

### Generating several flavours from one spec

Both flavours can be produced in one execution, but each needs its own `<basePackage>`: they
declare the same class names, so sharing a package would put duplicate classes on the compile
path. The plugin checks for this and fails with a clear message rather than leaving it to
`javac`:

```xml
<configuration>
    <specDefinition>${project.basedir}/src/main/resources/bookstore.yaml</specDefinition>
    <basePackage>com.bookstore.client</basePackage>
    <transformations>
        <transformation>
            <flavour>QUARKUS</flavour>
            <basePackage>com.bookstore.client.quarkus</basePackage>
        </transformation>
        <transformation>
            <flavour>SPRING</flavour>
            <basePackage>com.bookstore.client.spring</basePackage>
            <group>bookstore</group>
        </transformation>
    </transformations>
</configuration>
```

To generate from more than one *document*, declare one `<execution>` per spec with its own
`<id>` and `<configuration>`.

### A fully configured execution

```xml
<plugin>
    <groupId>hi.mynameis.ilnano</groupId>
    <artifactId>openapi-generator</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>generate</id>
            <phase>process-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <specDefinition>${project.basedir}/spec/pet-store.yaml</specDefinition>
                <outputFolder>${project.build.directory}/my-api</outputFolder>
                <basePackage>com.acme.custom</basePackage>
                <transformations>
                    <transformation>
                        <flavour>SPRING</flavour>
                        <group>pet-store</group>
                        <outputFolder>${project.build.directory}/my-api/clients</outputFolder>
                    </transformation>
                </transformations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Running it

With the execution bound as above, generation happens as part of the normal build:

```bash
mvn clean generate-sources   # generate only
mvn clean compile            # generate and compile
```

To run the goal on its own, without binding it to a phase:

```bash
mvn openapi:generate
```

The `openapi:` prefix resolves only when the plugin is declared in the project's `<build>`
section, or when `hi.mynameis.ilnano` is listed as a `<pluginGroup>` in your `settings.xml`.
Otherwise use the fully qualified form, which always works:

```bash
mvn hi.mynameis.ilnano:openapi-generator:0.1.0-SNAPSHOT:generate
```

The mojo's own parameters have matching user properties, so they can be overridden on the
command line. `transformations` is a structured block and has none, so it always comes from
the pom:

```bash
mvn openapi:generate \
    -DspecDefinition=src/main/resources/my-api.yaml \
    -DoutputFolder=target/my-api \
    -DbasePackage=com.demo.api
```

The plugin logs one line per transformation:

```
[INFO] Applied transformation[flavour=QUARKUS], writing 17 files into /path/to/target/generated-sources/bookstore/quarkus
[INFO] Applied transformation[flavour=SPRING], writing 18 files into /path/to/target/generated-sources/bookstore/spring
```

## Known limitation: enum schemas

A schema that is `type: string` with an `enum` list is currently emitted as an empty record:

```java
public record Genre() {
}
```

The generator has no enum handling, so the constants are dropped. Code still compiles,
because fields are typed `Genre` either way, but the type carries no values.

## Developing the plugin

### Building and testing

```bash
mvn test      # unit tests only
mvn verify    # unit tests plus the integration tests described below
```

The transformation tests compile their output against the real `jakarta.ws.rs`, MicroProfile
and Spring annotations, which are test dependencies of this project. That is what catches an
annotation member that does not exist or a meta-annotation used where it is not allowed —
things a text assertion cannot see.

The integration tests under `src/it/` run the plugin through a real Maven lifecycle via
`maven-invoker-plugin`. Each project there has an `invoker.properties` that defines the goals
to run and a `verify.groovy` that asserts the result:

- `default-configuration` — the goal fires in `generate-sources` without a `<phase>` in the
  pom, applies the `QUARKUS` flavour and writes to the default location.
- `custom-configuration` — the execution is rebound to `process-sources`, the spec and
  `outputFolder` are overridden, and the `SPRING` flavour is used with an explicit `group`.
- `compiles-generated-sources` — runs up to `compile` and asserts the transformed classes land
  in `target/classes`, proving both the source root registration and that the output is valid
  against the real framework APIs.
- `no-transformation` — configures no transformation and asserts the build fails with the
  expected message and writes nothing.

`src/it/` is hand-written test source and belongs in version control. The invoker copies each
project into `target/it/` and installs the plugin into `target/local-repo/` before running
them; both are build output and are ignored.

To run a single integration test project:

```bash
mvn verify -Dinvoker.test=custom-configuration
```

Failures are easiest to diagnose from the build log of the cloned project, for example
`target/it/custom-configuration/build.log`. To skip the integration tests entirely, use
`-Dinvoker.skip=true`.

### How the transformations are built

The rewriting lives in `hi.mynameis.ilnano.rewrite`. Everything shared — finding the markers,
hoisting the path prefix, replacing annotations, fixing imports — sits in
`RewriteGeneratedApi`. A framework only implements `ClientFlavour`, which answers three
questions: which annotations go on the interface, on a method, and on a parameter.

Adding a flavour therefore means writing one `ClientFlavour`, one `Recipe` that wires it up,
and one constant in `TransformationFlavour`.

Two things about the markers are worth knowing before changing this code. They have `SOURCE`
retention and the generator is usually not on the parse classpath, so their types arrive as
`JavaType.Unknown`: they have to be matched by simple name, and `TypeUtils` and
`AnnotationMatcher` do not work on them. And replacing the annotations of an individual
parameter through its own coordinates silently does nothing, so the whole parameter list is
rebuilt through the method's `replaceParameters()` instead.

### Installing a local build to try the plugin by hand

To use the plugin from another project on your machine, install it into your local repository
(`~/.m2/repository`):

```bash
mvn clean install
```

That runs the full test suite first. To install quickly while iterating, skip the tests:

```bash
mvn clean install -DskipTests -Dinvoker.skip=true
```

The artifact is now available to any local project at the version in `pom.xml`
(`0.1.0-SNAPSHOT`). A scratch project is enough to try it:

```bash
mkdir -p /tmp/try-openapi/src/main/resources
cd /tmp/try-openapi
cp /path/to/your-spec.yaml src/main/resources/my-api.yaml
cat > pom.xml <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>try</groupId>
    <artifactId>try-openapi</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>hi.mynameis.ilnano</groupId>
                <artifactId>openapi-generator</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <configuration>
                    <specDefinition>src/main/resources/my-api.yaml</specDefinition>
                    <basePackage>com.demo.api</basePackage>
                    <transformations>
                        <transformation>
                            <flavour>QUARKUS</flavour>
                        </transformation>
                    </transformations>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

mvn openapi:generate
find target/generated-sources -name '*.java'
```

Because `0.1.0-SNAPSHOT` is a snapshot, a later `mvn install` replaces it in place — no
version bump needed while iterating. If a consuming project seems to hold on to an older
build, force a re-resolve with `mvn -U`.

There is also a fuller sample at `../bookstore-api-sample`, which runs both flavours against a
spec written to exercise every verb and parameter location.

To remove the local install again:

```bash
rm -rf ~/.m2/repository/hi/mynameis/ilnano/openapi-generator
```
