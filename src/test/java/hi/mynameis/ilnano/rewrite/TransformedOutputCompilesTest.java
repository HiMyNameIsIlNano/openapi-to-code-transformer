package hi.mynameis.ilnano.rewrite;

import hi.mynameis.ilnano.JavaSourceCompiler;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiles the transformed sources against the real {@code jakarta.ws.rs}, MicroProfile and Spring
 * annotations, which are test dependencies of this project.
 *
 * <p>The text assertions elsewhere cannot catch an annotation member that does not exist or a
 * meta-annotation used where it is not allowed; this can.
 */
final class TransformedOutputCompilesTest {

    private final RecipeRunner runner = new RecipeRunner();

    private List<String> transformAll(Recipe recipe) {
        var sources = new ArrayList<>(GeneratedApiFixture.models());
        sources.add(GeneratedApiFixture.PETS_API);
        sources.add(GeneratedApiFixture.OWNERS_API);
        sources.add(GeneratedApiFixture.EXOTIC_API);

        return runner.run(recipe, sources);
    }

    @Test
    void the_quarkus_output_compiles() {
        var errors = new JavaSourceCompiler().compile(transformAll(new QuarkusRestClientRecipe()));

        assertThat(errors).isEmpty();
    }

    @Test
    void the_spring_output_compiles() {
        var errors = new JavaSourceCompiler()
                .compile(transformAll(new SpringHttpServiceClientRecipe()));

        assertThat(errors).isEmpty();
    }
}
