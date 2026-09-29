package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SourceFileWriterTest implements OpenApiLoader {

    private static final String SPEC = "src/test/resources/generation/annotated-api.yaml";
    private static final String BASE_PACKAGE = "com.acme.generated";

    @TempDir
    private Path outputDirectory;

    private GeneratedSources generated() {
        return new JavaGenerator().generate(load(SPEC), BASE_PACKAGE);
    }

    private List<Path> write() {
        return new SourceFileWriter(outputDirectory).write(generated());
    }

    private Path relative(Path written) {
        return outputDirectory.relativize(written);
    }

    @Test
    void writes_one_file_per_generated_source() {
        var sources = generated();

        assertThat(write()).hasSize(sources.all().size());
    }

    @Test
    void mirrors_the_package_structure_into_directories() {
        assertThat(write()).map(this::relative).map(Path::toString)
                .contains("com/acme/generated/api/PetsApi.java")
                .contains("com/acme/generated/api/OwnersApi.java")
                .contains("com/acme/generated/model/Pet.java")
                .contains("com/acme/generated/model/Owner.java");
    }

    @Test
    void names_each_file_after_the_public_type_it_declares() {
        assertThat(write()).map(Path::getFileName).map(Path::toString)
                .allSatisfy(fileName -> assertThat(fileName).endsWith(".java"))
                .contains("PetsApi.java", "Pet.java");
    }

    @Test
    void writes_the_generated_source_verbatim() throws Exception {
        var petsApi = write().stream()
                .filter(path -> path.endsWith("PetsApi.java"))
                .findFirst()
                .orElseThrow();

        assertThat(Files.readString(petsApi))
                .isEqualTo(generated().apis().stream()
                        .filter(source -> source.contains("interface PetsApi"))
                        .findFirst()
                        .orElseThrow());
    }

    @Test
    void creates_the_output_directory_when_it_does_not_exist() {
        var target = outputDirectory.resolve("does/not/exist/yet");

        assertThat(new SourceFileWriter(target).write(generated())).isNotEmpty();
        assertThat(target).isDirectory();
    }

    @Test
    void overwrites_sources_from_a_previous_run() throws Exception {
        var stale = outputDirectory.resolve("com/acme/generated/model/Pet.java");
        Files.createDirectories(stale.getParent());
        Files.writeString(stale, "// stale content");

        write();

        assertThat(Files.readString(stale))
                .doesNotContain("stale content")
                .contains("public record Pet(");
    }

    @Test
    void fails_when_a_source_declares_no_public_type() {
        var malformed = new GeneratedSources(List.of("package com.acme;"), List.of());

        assertThatThrownBy(() -> new SourceFileWriter(outputDirectory).write(malformed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No public type declared");
    }

    @Test
    void fails_when_the_output_location_is_not_writable() throws Exception {
        var blocked = Files.createFile(outputDirectory.resolve("blocked"));

        assertThatThrownBy(() -> new SourceFileWriter(blocked).write(generated()))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to write generated source");
    }
}
