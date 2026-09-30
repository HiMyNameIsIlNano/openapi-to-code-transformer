package hi.mynameis.ilnano.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rewrites the generated marker-annotated interfaces into Spring Boot 4 HTTP service clients.
 *
 * <p>Each interface is annotated with {@code @HttpExchange} and its methods with the per-verb
 * {@code *Exchange} variants. Because the framework registers clients from a configuration class
 * rather than by scanning the interfaces, the recipe also writes one {@code @ImportHttpServices}
 * class covering the package the interfaces live in.
 */
public class SpringHttpServiceClientRecipe extends ScanningRecipe<SpringHttpServiceClientRecipe.ApiPackages> {

    private static final String CONFIG_CLASS = "HttpServiceClientConfig";

    @Option(displayName = "Client group name",
            description = "The `@ImportHttpServices` group, which is also the key the base URL is "
                    + "configured under: `spring.http.client.service.group.<group>.base-url`.",
            example = "pet-store",
            required = false)
    private final String group;

    public SpringHttpServiceClientRecipe() {
        this(null);
    }

    public SpringHttpServiceClientRecipe(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String getDisplayName() {
        return "Rewrite generated API interfaces into Spring Boot 4 HTTP service clients";
    }

    @Override
    public String getDescription() {
        return "Replaces the `@ApiInterface`, `@ApiAnnotation` and `@ApiParam` markers on the "
                + "generated interfaces with `@HttpExchange` and the Spring web binding "
                + "annotations, and adds an `@ImportHttpServices` configuration class that "
                + "registers them as beans. Interfaces without the markers are left alone.";
    }

    /**
     * The packages the generated interfaces were found in, collected while scanning so that the
     * configuration class can be written into the right place and point at the right packages.
     */
    public static final class ApiPackages {

        private final Set<String> names = new TreeSet<>();

        private boolean configExists;

        void add(String name) {
            names.add(name);
        }

        void markConfigExists() {
            configExists = true;
        }

        /**
         * Whether a configuration class still has to be written.
         */
        boolean needsConfig() {
            return !configExists && !names.isEmpty();
        }

        /**
         * Where the configuration class goes: the shortest of the API packages, which is the one
         * that encloses the others if they are nested and an arbitrary but stable choice if not.
         */
        String configPackage() {
            return names.stream().min(java.util.Comparator.comparingInt(String::length)).orElseThrow();
        }

        List<String> all() {
            return List.copyOf(names);
        }
    }

    @Override
    public ApiPackages getInitialValue(ExecutionContext ctx) {
        return new ApiPackages();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ApiPackages acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, ExecutionContext ctx) {
                if (CONFIG_CLASS.equals(declaration.getSimpleName())) {
                    acc.markConfigExists();
                } else if (RewriteGeneratedApi.isGeneratedApi(declaration)) {
                    packageOf(getCursor().firstEnclosing(J.CompilationUnit.class)).ifPresent(acc::add);
                }
                return declaration;
            }
        };
    }

    /**
     * Writes the configuration class. Nothing is generated when no marker-annotated interface was
     * found, or when a class of that name already exists — a second run must not duplicate it.
     */
    @Override
    public Collection<SourceFile> generate(ApiPackages acc, ExecutionContext ctx) {
        if (!acc.needsConfig()) {
            return List.of();
        }

        var configPackage = acc.configPackage();

        var sourcePath = java.nio.file.Path.of(
                configPackage.replace('.', '/'), CONFIG_CLASS + ".java");

        return JavaParser.fromJavaVersion()
                .build()
                .parse(ctx, configSource(configPackage, acc.all()))
                .map(source -> (SourceFile) source.withSourcePath(sourcePath))
                .collect(java.util.stream.Collectors.toList());
    }

    private String configSource(String configPackage, List<String> apiPackages) {
        return """
                package %s;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.web.service.registry.ImportHttpServices;

                /**
                 * Registers the generated HTTP service clients as beans.
                 *
                 * <p>Configure the base URL as
                 * {@code spring.http.client.service.group.%s.base-url}.
                 */
                @ImportHttpServices(group = "%s", basePackages = %s)
                @Configuration(proxyBeanMethods = false)
                public class %s {
                }
                """.formatted(
                configPackage, groupName(configPackage), groupName(configPackage),
                basePackages(apiPackages), CONFIG_CLASS);
    }

    /**
     * The configured group, or the last segment of the package the clients live in.
     */
    private String groupName(String configPackage) {
        if (group != null && !group.isBlank()) {
            return group;
        }

        var lastDot = configPackage.lastIndexOf('.');

        return lastDot < 0 ? configPackage : configPackage.substring(lastDot + 1);
    }

    private String basePackages(List<String> apiPackages) {
        var quoted = apiPackages.stream().map("\"%s\""::formatted).toList();

        return quoted.size() == 1 ? quoted.get(0) : "{" + String.join(", ", quoted) + "}";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ApiPackages acc) {
        return new RewriteGeneratedApi(new SpringFlavour());
    }

    private static Optional<String> packageOf(J.CompilationUnit source) {
        return Optional.ofNullable(source)
                .map(J.CompilationUnit::getPackageDeclaration)
                .map(declaration -> declaration.getExpression().printTrimmed());
    }
}
