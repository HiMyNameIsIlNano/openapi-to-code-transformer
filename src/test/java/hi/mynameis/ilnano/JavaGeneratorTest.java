package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class JavaGeneratorTest implements OpenApiLoader {

    private final JavaGenerator processor = new JavaGenerator();

    @Test
    void groups_operations_under_their_first_tag_capitalized() {
        Map<String, List<OperationInfo>> byTag =
                processor.groupByTag(load("src/test/resources/grouping/multi-tag.yaml"));

        assertThat(byTag).containsOnlyKeys("Pets", "Owners");
        assertThat(byTag.get("Pets")).hasSize(1);
        assertThat(byTag.get("Owners")).hasSize(1);
        assertThat(byTag.get("Pets").get(0).operationId()).isEqualTo("listPets");
    }

    /*@Test
    void collectsMultipleOperationsSharingATagIntoOneList() {
        Map<String, List<OperationInfo>> byTag =
                processor.groupByTag(load("same-tag-multiple-ops.yaml"));

        assertThat(byTag).containsOnlyKeys("Pets");
        assertThat(byTag.get("Pets"))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listPets", "createPet", "getPet");
    }

    @Test
    void separateOperationPerVerbOnSamePath() {
        Map<String, List<OperationInfo>> byTag =
                processor.groupByTag(load("same-tag-multiple-ops.yaml"));

        // /pets has GET and POST → two distinct entries
        assertThat(byTag.get("Pets"))
                .filteredOn(op -> "/pets".equals(op.path()))
                .extracting(OperationInfo::httpMethod)
                .containsExactlyInAnyOrder("GET", "POST");
    }

    @Test
    void untaggedOperationsFallIntoDefaultBucket() {
        Map<String, List<OperationInfo>> byTag =
                processor.groupByTag(load("untagged.yaml"));

        assertThat(byTag).containsOnlyKeys("Default");
        assertThat(byTag.get("Default").get(0).operationId()).isEqualTo("health");
    }*/

    @Test
    void no_paths_yields_empty_map() {
        var openAPI = load("src/test/resources/grouping/no-paths.yaml");

        Map<String, List<OperationInfo>> byTag = processor.groupByTag(openAPI);

        assertThat(byTag).isEmpty();
    }

    /*@Test
    void yamlAndJsonProduceIdenticalGrouping() {
        Map<String, List<OperationInfo>> fromYaml =
                processor.groupByTag(load("multi-tag.yaml"));
        Map<String, List<OperationInfo>> fromJson =
                processor.groupByTag(load("multi-tag.json"));

        // same keys, same operation ids per key — format must not matter
        assertThat(fromJson.keySet()).isEqualTo(fromYaml.keySet());
        fromYaml.forEach((tag, ops) ->
                assertThat(fromJson.get(tag))
                        .extracting(OperationInfo::operationId)
                        .containsExactlyInAnyOrderElementsOf(
                                ops.stream().map(OperationInfo::operationId).toList()));
    }*/

}