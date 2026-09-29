package hi.mynameis.ilnano;

import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class ParameterNamer {

    private static final String FALLBACK = "param";

    String toJavaName(String specName) {
        var camelCase = toLowerCamelCase(specName);

        if (camelCase.isEmpty()) {
            return FALLBACK;
        }
        if (SourceVersion.isKeyword(camelCase)) {
            return camelCase + "Param";
        }
        return Character.isJavaIdentifierStart(camelCase.charAt(0))
                ? camelCase
                : FALLBACK + camelCase;
    }

    private String toLowerCamelCase(String specName) {
        var words = Arrays.stream(specName.split("[^A-Za-z0-9]+"))
                .filter(word -> !word.isEmpty())
                .toList();

        return IntStream.range(0, words.size())
                .mapToObj(index -> index == 0
                        ? decapitalize(words.get(index))
                        : capitalize(words.get(index)))
                .collect(Collectors.joining());
    }

    private String decapitalize(String word) {
        return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }

    private String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
