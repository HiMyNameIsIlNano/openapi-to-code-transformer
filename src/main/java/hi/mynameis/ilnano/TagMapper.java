package hi.mynameis.ilnano;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

final class TagMapper implements Mapper {

    private static final ApiTag DEFAULT_TAG = new ApiTag("Default");

    private final UnaryOperator<String> nameStyle;

    TagMapper(UnaryOperator<String> nameStyle) {
        this.nameStyle = nameStyle;
    }

    @Override
    public ApiTag mapTags(List<String> tags) {
        return Optional.ofNullable(tags)
                .flatMap(candidates -> candidates.stream().findFirst())
                .map(nameStyle)
                .map(ApiTag::new)
                .orElse(DEFAULT_TAG);
    }
}
