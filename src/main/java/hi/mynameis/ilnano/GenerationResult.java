package hi.mynameis.ilnano;

import org.openrewrite.SourceFile;

import java.util.List;
import java.util.Optional;

record GenerationResult(List<SourceFile> models,
                        List<SourceFile> apis) {

    public GenerationResult {
        models = Optional.ofNullable(models)
                .map(List::copyOf)
                .orElse(List.of());

        apis = Optional.ofNullable(apis)
                .map(List::copyOf)
                .orElse(List.of());
    }

}


