package hi.mynameis.ilnano.rewrite;

import org.openrewrite.Recipe;

/**
 * The recipes that rewrite the generated marker-annotated sources into framework-specific clients.
 *
 * <p>The only way into this package: the recipes themselves are public so that OpenRewrite can also
 * run them standalone, and this hands the plugin instances without it needing to know which class
 * implements which flavour.
 */
public final class ClientRecipes {

    private ClientRecipes() {
    }

    /**
     * {@code jakarta.ws.rs} plus MicroProfile {@code @RegisterRestClient}.
     */
    public static Recipe quarkus() {
        return new QuarkusRestClientRecipe();
    }

    /**
     * Spring Boot 4 {@code @HttpExchange} clients and their {@code @ImportHttpServices} config.
     */
    public static Recipe spring() {
        return new SpringHttpServiceClientRecipe();
    }

    /**
     * Spring clients whose {@code @ImportHttpServices} group, and therefore the property their base
     * URL is configured under, is {@code group}.
     */
    public static Recipe spring(String group) {
        return new SpringHttpServiceClientRecipe(group);
    }
}
