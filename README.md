# openapi-generator Maven Plugin

Generates Java API interfaces and model records from an OpenAPI 3 document.

For every tag in the document the plugin generates one annotated interface (`PetsApi`,
`OwnersApi`, …) into `<basePackage>.api`, and for every schema under `components/schemas`
one record into `<basePackage>.model`. Both YAML and JSON documents are supported.

The generated sources carry `@ApiInterface`, `@ApiAnnotation` and `@ApiParam`, which
describe the tag, the HTTP method, the path, the produced media type and the location of
every parameter. They are meant to be consumed by an OpenRewrite recipe that turns them
into framework-specific code.

## Usage

### Adding the plugin

The goal is `generate` and it is bound to the `generate-sources` phase by default, so
declaring the execution without a `<phase>` is enough:

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
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

The generated sources reference the plugin's annotations, so if you compile them in the
same module you also need the plugin on the compile classpath. The annotations have
`SOURCE` retention, which makes `provided` the right scope:

```xml
<dependency>
    <groupId>hi.mynameis.ilnano</groupId>
    <artifactId>openapi-generator</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Configuration

| Parameter | Property | Default | Description |
|---|---|---|---|
| `specDefinition` | `specDefinition` | `${project.basedir}/src/main/resources/openapi.yaml` | Path or URL of the OpenAPI document. |
| `outputFolder` | `outputFolder` | `${project.build.directory}/generated-sources/<spec name>` | Directory the sources are written to. |
| `basePackage` | `basePackage` | `hi.mynameis.ilnano.generated` | Root package; `.api` and `.model` are appended. |

By default the output goes into `target/generated-sources/` plus a directory named after
the OpenAPI document, so `src/main/resources/pet-store.yaml` produces
`target/generated-sources/pet-store/`. This keeps two specs in the same module from
overwriting each other. Set `outputFolder` to choose the directory yourself, in which
case the spec name is not appended.

Whichever directory is used is registered as a compile source root, so the generated
code is compiled into your artifact without any extra `build-helper` configuration.

A fully configured execution, rebound to a different phase:

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
            </configuration>
        </execution>
    </executions>
</plugin>
```

To generate from more than one document, declare one `<execution>` per spec with its own
`<id>` and `<configuration>`.

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

The `openapi:` prefix resolves only when the plugin is declared in the project's
`<build>` section, or when `hi.mynameis.ilnano` is listed as a `<pluginGroup>` in your
`settings.xml`. Otherwise use the fully qualified form, which always works:

```bash
mvn hi.mynameis.ilnano:openapi-generator:0.1.0-SNAPSHOT:generate
```

Every parameter has a matching user property, so it can be overridden on the command
line:

```bash
mvn openapi:generate \
    -DspecDefinition=src/main/resources/my-api.yaml \
    -DoutputFolder=target/my-api \
    -DbasePackage=com.demo.api
```

The plugin logs a summary of what it wrote:

```
[INFO] Generated 2 API interfaces and 2 models (4 files) into /path/to/target/generated-sources/my-api
```

## Developing the plugin

### Building and testing

```bash
mvn test      # unit tests only
mvn verify    # unit tests plus the integration tests described below
```

The integration tests under `src/it/` run the plugin through a real Maven lifecycle via
`maven-invoker-plugin`. Each project there has an `invoker.properties` that defines the
goals to run and a `verify.groovy` that asserts the result:

- `default-configuration` — the goal fires in `generate-sources` without a `<phase>` in
  the pom, and writes to the default location.
- `custom-configuration` — the execution is rebound to `process-sources` and both the
  spec and `outputFolder` are overridden.
- `compiles-generated-sources` — runs up to `compile` and asserts the generated classes
  land in `target/classes`, proving the source root registration.

`src/it/` is hand-written test source and belongs in version control. The invoker copies
each project into `target/it/` and installs the plugin into `target/local-repo/` before
running them; both are build output and are ignored.

To run a single integration test project:

```bash
mvn verify -Dinvoker.test=custom-configuration
```

Failures are easiest to diagnose from the build log of the cloned project, for example
`target/it/custom-configuration/build.log`. To skip the integration tests entirely, use
`-Dinvoker.skip=true`.

### Installing a local build to try the plugin by hand

To use the plugin from another project on your machine, install it into your local
repository (`~/.m2/repository`):

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
</project>
EOF

mvn hi.mynameis.ilnano:openapi-generator:0.1.0-SNAPSHOT:generate \
    -DspecDefinition=src/main/resources/my-api.yaml \
    -DbasePackage=com.demo.api

find target/generated-sources -name '*.java'
```

Because `0.1.0-SNAPSHOT` is a snapshot, a later `mvn install` replaces it in place — no
version bump needed while iterating. If a consuming project seems to hold on to an older
build, force a re-resolve with `mvn -U`.

To remove the local install again:

```bash
rm -rf ~/.m2/repository/hi/mynameis/ilnano/openapi-generator
```
