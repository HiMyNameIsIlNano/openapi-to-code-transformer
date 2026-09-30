package hi.mynameis.ilnano;

import hi.mynameis.ilnano.rewrite.ClientRecipes;
import org.openrewrite.Recipe;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The target framework of a {@link Transformation}.
 */
public enum TransformationFlavour {

    /**
     * {@code jakarta.ws.rs} clients registered with MicroProfile's {@code @RegisterRestClient},
     * suited to a Quarkus project.
     */
    QUARKUS {
        @Override
        public Recipe recipe() {
            return ClientRecipes.quarkus();
        }
    },

    /**
     * Spring Boot 4 HTTP service clients built on {@code @HttpExchange}, plus the
     * {@code @ImportHttpServices} configuration class that registers them.
     */
    SPRING {
        @Override
        public Recipe recipe() {
            return ClientRecipes.spring();
        }
    };

    /**
     * The recipe that rewrites the marker-annotated sources into this flavour.
     */
    public abstract Recipe recipe();

    /**
     * Parses the value configured in the pom, accepting any capitalisation.
     *
     * @throws IllegalArgumentException with the list of valid values, since this surfaces to the
     *                                  user as a build error
     */
    static TransformationFlavour from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown flavour '%s'. Valid values are %s.".formatted(value, valid()));
        }
    }

    static String valid() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
