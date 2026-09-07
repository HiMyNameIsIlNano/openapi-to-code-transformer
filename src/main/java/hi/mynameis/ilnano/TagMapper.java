package hi.mynameis.ilnano;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

class TagMapper implements Mapper {

    private final UnaryOperator<String> mapper;

    TagMapper(UnaryOperator<String> mapper) {
        this.mapper = mapper;
    }


    /**
     * It maps a (group of) tag(s) to a single tag. If the provided input has a size bigger than 1, then only the first tag is taken
     * and the rest is discarded.
     *
     *
     **/
    @Override
    public ApiTag mapTags(List<String> tags) {
        if (Objects.isNull(tags)) {
            return new ApiTag("Default");
        }

        if (tags.isEmpty()) {
            return new ApiTag("Default");
        }

        var tag = tags.get(0);
        return new ApiTag(mapper.apply(tag));
    }

}
