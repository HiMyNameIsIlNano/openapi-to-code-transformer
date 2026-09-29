package hi.mynameis.ilnano;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

/**
 * This class lifts inline (anonymous) object schemas into named top-level component
 * schemas, replacing each inline definition with a $ref.
 * <p>
 * The given spec is never modified. A deep copy is made, hoisting is applied to the copy,
 * and the copy is returned.
 */
final class InlineObjectHoister {

    InlineObjectHoister() {
    }

    /**
     * Returns a new, flattened spec. The input {@code spec} is left untouched.
     */
    OpenAPI hoist(OpenAPI spec) {
        OpenAPI hoisted = new OpenApiSpecHelper().deepCopy(spec);

        if (hoisted.getComponents() == null
                || hoisted.getComponents().getSchemas() == null) {
            return hoisted;
        }
        Map<String, Schema> schemas = hoisted.getComponents().getSchemas();

        Deque<WorkItem> queue = new ArrayDeque<>();
        for (Map.Entry<String, Schema> entry : new ArrayList<>(schemas.entrySet())) {
            queue.push(new WorkItem(entry.getValue(), entry.getKey()));
        }

        while (!queue.isEmpty()) {
            WorkItem item = queue.pop();
            Schema<?> parent = item.schema();

            if (parent.getProperties() != null) {
                for (String propName : new ArrayList<>(parent.getProperties().keySet())) {
                    Schema<?> propSchema = parent.getProperties().get(propName);
                    Schema<?> target = unwrapArrays(propSchema);

                    if (isInlineObject(target)) {
                        String newName = uniqueName(
                                schemas, item.nameHint() + capitalize(propName));
                        schemas.put(newName, target);
                        if (propSchema == target) {
                            // Replace the property slot itself; mutating the leaf would
                            // also gut the schema we just registered under newName.
                            parent.getProperties().put(propName, refTo(newName));
                        } else {
                            rewireToRef(propSchema, target, newName);
                        }
                        queue.push(new WorkItem(target, newName));
                    }
                }
            }

            Schema<?> arrayLeaf = unwrapArrays(parent);
            if (arrayLeaf != parent && isInlineObject(arrayLeaf)) {
                String newName = uniqueName(schemas, item.nameHint() + "Item");
                schemas.put(newName, arrayLeaf);
                rewireToRef(parent, arrayLeaf, newName);
                queue.push(new WorkItem(arrayLeaf, newName));
            }
        }
        return hoisted;
    }

    private boolean isInlineObject(Schema<?> s) {
        return s != null
                && s.get$ref() == null
                && ("object".equals(s.getType())
                || (s.getType() == null && s.getProperties() != null))
                && s.getProperties() != null
                && !s.getProperties().isEmpty();
    }

    private Schema<?> unwrapArrays(Schema<?> s) {
        Schema<?> current = s;
        while (current != null && "array".equals(current.getType())
                && current.getItems() != null) {
            current = current.getItems();
        }
        return current;
    }

    private void rewireToRef(Schema<?> holder, Schema<?> inlineLeaf, String newName) {
        Schema<?> current = holder;
        while ("array".equals(current.getType())
                && current.getItems() != null
                && "array".equals(current.getItems().getType())) {
            current = current.getItems();
        }
        current.setItems(refTo(newName));
    }

    private Schema<Object> refTo(String newName) {
        Schema<Object> refSchema = new Schema<>();
        refSchema.set$ref("#/components/schemas/" + newName);
        return refSchema;
    }

    private String uniqueName(Map<String, Schema> schemas, String base) {
        String candidate = base;
        int i = 1;
        while (schemas.containsKey(candidate)) {
            candidate = base + (++i);
        }
        return candidate;
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty())
                ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record WorkItem(Schema<?> schema, String nameHint) {
    }
}