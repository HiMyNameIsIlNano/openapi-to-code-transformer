package hi.mynameis.ilnano;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class TagMatcherTest implements OpenApiLoader {

    private final TagMatcher testSubject = new TagMatcher();

    @Test
    void groups_operations_under_their_first_tag_capitalized() {
        var byTag =
                testSubject.groupByTag(load("src/test/resources/grouping/multi-tag.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Pets"), new ApiTag("Owners"));
        assertThat(byTag.get(new ApiTag("Pets"))).hasSize(1);
        assertThat(byTag.get(new ApiTag("Owners"))).hasSize(1);
        assertThat(byTag.get(new ApiTag("Pets")).get(0).operationId()).isEqualTo("listPets");
    }

    @Test
    void collects_multiple_operations_sharing_a_tag_into_one_list() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/same-tag-multiple-ops.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Pets"));
        assertThat(byTag.get(new ApiTag("Pets")))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listPets", "createPet", "getPet");
    }

    @Test
    void separate_operation_per_verb_on_same_path() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/same-tag-multiple-ops.yaml"));

        // /pets has GET and POST → two distinct entries
        assertThat(byTag.get(new ApiTag("Pets")))
                .filteredOn(operationInfo -> "/pets".equals(operationInfo.path()))
                .extracting(OperationInfo::httpMethod)
                .containsExactlyInAnyOrder("GET", "POST");
    }

    @Test
    void untagged_operations_fall_into_default_bucket() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/untagged.yaml"));

        assertThat(byTag).hasSize(1)
                .containsOnlyKeys(new ApiTag("Default"));

        assertThat(byTag.get(new ApiTag("Default")).get(0).operationId()).isEqualTo("health");
    }

    @Test
    void no_paths_yields_empty_map() {
        var openAPI = load("src/test/resources/grouping/no-paths.yaml");

        var byTag = testSubject.groupByTag(openAPI);

        assertThat(byTag).isEmpty();
    }

    @Test
    void yaml_and_json_produce_identical_grouping() {
        var fromYaml = testSubject.groupByTag(load("src/test/resources/grouping/multi-tag.yaml"));
        var fromJson = testSubject.groupByTag(load("src/test/resources/grouping/multi-tag.json"));

        assertThat(fromJson.keySet()).isEqualTo(fromYaml.keySet());
        fromYaml.forEach((tag, operationInfos) ->
                assertThat(fromJson.get(tag))
                        .extracting(OperationInfo::operationId)
                        .containsExactlyInAnyOrderElementsOf(
                                operationInfos.stream().map(OperationInfo::operationId).toList()));
    }

    @Test
    void first_tag_wins_when_operation_has_multiple_tags() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/multiple-tags-per-operation.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Pets"), new ApiTag("Admin"));
        assertThat(byTag.get(new ApiTag("Pets")))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listPets", "getPet");

        assertThat(byTag).doesNotContainKey(new ApiTag("Animals"));
        assertThat(byTag).doesNotContainKey(new ApiTag("Store"));
    }

    @Test
    void tag_is_capitalized_even_when_all_lowercase() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/lowercase-tags.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Inventory"));
        assertThat(byTag.get(new ApiTag("Inventory")))
                .extracting(OperationInfo::operationId)
                .containsExactly("getStock");
    }

    @Test
    void tag_already_capitalized_stays_unchanged() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/already-capitalized-tags.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Billing"));
        assertThat(byTag.get(new ApiTag("Billing")))
                .extracting(OperationInfo::operationId)
                .containsExactly("getInvoices");
    }

    @Test
    void null_openapi_throws_npe() {
        assertThatThrownBy(() -> testSubject.groupByTag(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("openAPI must not be null");
    }

    @Test
    void complex_api_groups_operations_across_many_paths_and_tags() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/complex-api.yaml"));

        assertThat(byTag).containsOnlyKeys(
                new ApiTag("Users"),
                new ApiTag("Orders"),
                new ApiTag("Products"),
                new ApiTag("Default")
        );

        assertThat(byTag.get(new ApiTag("Users")))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listUsers", "createUser", "getUser", "updateUser", "deleteUser");

        assertThat(byTag.get(new ApiTag("Orders")))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listOrders", "createOrder", "getOrder");

        assertThat(byTag.get(new ApiTag("Products")))
                .extracting(OperationInfo::operationId)
                .containsExactlyInAnyOrder("listProducts", "getProduct");

        assertThat(byTag.get(new ApiTag("Default")))
                .extracting(OperationInfo::operationId)
                .containsExactly("healthCheck");
    }

    @Test
    void complex_api_preserves_http_method_and_path() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/complex-api.yaml"));

        assertThat(byTag.get(new ApiTag("Users")))
                .filteredOn(operationInfo -> "getUser".equals(operationInfo.operationId()))
                .singleElement()
                .satisfies(operationInfo -> {
                    assertThat(operationInfo.httpMethod()).isEqualTo("GET");
                    assertThat(operationInfo.path()).isEqualTo("/users/{id}");
                });

        assertThat(byTag.get(new ApiTag("Orders")))
                .filteredOn(operationInfo -> "createOrder".equals(operationInfo.operationId()))
                .singleElement()
                .satisfies(operationInfo -> {
                    assertThat(operationInfo.httpMethod()).isEqualTo("POST");
                    assertThat(operationInfo.path()).isEqualTo("/orders");
                });
    }

    @Test
    void single_path_with_all_http_verbs() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/all-verbs.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Resources"));
        assertThat(byTag.get(new ApiTag("Resources")))
                .extracting(OperationInfo::httpMethod)
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE");
    }

    @Test
    void empty_tags_list_falls_into_default_bucket() {
        var byTag = testSubject.groupByTag(load("src/test/resources/grouping/empty-tags-list.yaml"));

        assertThat(byTag).containsOnlyKeys(new ApiTag("Default"));
        assertThat(byTag.get(new ApiTag("Default")))
                .extracting(OperationInfo::operationId)
                .containsExactly("ping");
    }
}