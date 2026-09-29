package hi.mynameis.ilnano;

import java.util.List;
import java.util.stream.Stream;

record GeneratedSources(List<String> models, List<String> apis) {

    List<String> all() {
        return Stream.concat(models.stream(), apis.stream()).toList();
    }
}
