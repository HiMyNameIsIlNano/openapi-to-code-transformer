package hi.mynameis.ilnano.rewrite;

import java.util.Arrays;
import java.util.List;

/**
 * A request path split into segments, so that the part shared by every operation of an interface
 * can be hoisted onto the interface itself and each method only keeps what is left.
 */
record ResourcePath(List<String> segments) {

    private static final String SEPARATOR = "/";

    static ResourcePath of(String path) {
        return new ResourcePath(Arrays.stream(path.split(SEPARATOR))
                .filter(segment -> !segment.isEmpty())
                .toList());
    }

    /**
     * The longest prefix shared with {@code other}. A path template segment such as
     * {@code {petId}} is compared verbatim, which is what makes {@code /pets/{petId}} and
     * {@code /pets/{petId}/owner} share two segments.
     */
    ResourcePath commonPrefixWith(ResourcePath other) {
        var shared = 0;
        var limit = Math.min(segments.size(), other.segments.size());

        while (shared < limit && segments.get(shared).equals(other.segments.get(shared))) {
            shared++;
        }
        return new ResourcePath(segments.subList(0, shared));
    }

    /**
     * This path with {@code prefix} removed from its front. Returns an empty path when the two are
     * equal, which is how a method ends up with no path annotation of its own.
     */
    ResourcePath relativeTo(ResourcePath prefix) {
        return isPrefixedBy(prefix)
                ? new ResourcePath(segments.subList(prefix.segments.size(), segments.size()))
                : this;
    }

    private boolean isPrefixedBy(ResourcePath prefix) {
        return prefix.segments.size() <= segments.size()
                && segments.subList(0, prefix.segments.size()).equals(prefix.segments);
    }

    boolean isEmpty() {
        return segments.isEmpty();
    }

    /**
     * The path in the leading-slash form the frameworks expect, for example {@code /pets/{petId}}.
     */
    @Override
    public String toString() {
        return SEPARATOR + String.join(SEPARATOR, segments);
    }
}
