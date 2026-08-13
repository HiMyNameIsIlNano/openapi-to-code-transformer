package hi.mynameis.ilnano;

import java.util.Objects;

class NoopWriter implements ResultWriter<Output, String> {

    private final String location;

    public NoopWriter(String location) {
        this.location = location;
    }

    @Override
    public String write(Output data) {
        Objects.requireNonNull(data);

        return location;
    }
}
