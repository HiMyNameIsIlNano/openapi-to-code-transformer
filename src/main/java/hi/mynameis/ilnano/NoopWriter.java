package hi.mynameis.ilnano;

import java.io.File;
import java.util.Objects;

public class NoopWriter implements ResultWriter<Output, File> {

    private final File directory;

    public NoopWriter(File directory) {
        this.directory = directory;
    }

    @Override
    public File write(Output data) {
        Objects.requireNonNull(data);

        return directory;
    }
}
