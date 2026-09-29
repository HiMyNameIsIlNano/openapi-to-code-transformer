package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SpecLocationTest {

    @ParameterizedTest
    @CsvSource({
            "src/main/resources/openapi.yaml, openapi",
            "src/main/resources/pet-store.yml, pet-store",
            "src/main/resources/PetStore.json, PetStore",
            "openapi.yaml, openapi",
            "src\\main\\resources\\openapi.yaml, openapi",
            "https://example.test/specs/pet-store.yaml, pet-store",
            "https://example.test/spec?version=2, spec",
            "src/main/resources/pet store v1.yaml, pet_store_v1"
    })
    void derives_the_directory_name_from_the_document_file_name(String location, String expected) {
        assertThat(new SpecLocation(location).directoryName()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"''", "'/'", "'.yaml'", "'src/main/resources/'"})
    void falls_back_to_a_default_name_when_none_can_be_derived(String location) {
        assertThat(new SpecLocation(location).directoryName()).isEqualTo("openapi");
    }

    @Test
    void rejects_a_null_location() {
        assertThatThrownBy(() -> new SpecLocation(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value must not be null");
    }
}
