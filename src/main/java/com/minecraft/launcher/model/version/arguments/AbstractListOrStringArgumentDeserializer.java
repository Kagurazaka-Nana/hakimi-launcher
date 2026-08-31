package com.minecraft.launcher.model.version.arguments;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.minecraft.launcher.model.rule.Rule;

import java.io.IOException;
import java.util.List;

/**
 * Base deserializer for launch-argument entries that may appear either as a
 * plain string or as a {@code {rules, value}} object (e.g. {@code game} /
 * {@code jvm} in the version JSON). The resolved value is always normalized to
 * {@code List<String} before being handed to {@link #build} so that consumers
 * get a uniform list regardless of the source shape.
 *
 * @param <T> the concrete argument type produced by {@link #build}
 */
public abstract class AbstractListOrStringArgumentDeserializer<T> extends JsonDeserializer<T> {

    /** Object mapper for converting nested JSON nodes. */
    protected final ObjectMapper mapper = new ObjectMapper();

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode root = jsonParser.readValueAsTree();

        if (root.isTextual()) {
            return build(List.of(root.asText()), null);
        }

        List<Rule> rules = readRules(root);
        if (root.has("value")) {
            return build(readValue(root.get("value")), rules);
        }
        throw new JsonMappingException(jsonParser, "missing required field 'value'");
    }

    private List<Rule> readRules(JsonNode root) {
        JsonNode rulesNode = root.get("rules");
        return (rulesNode == null)
                ? null
                : mapper.convertValue(rulesNode, new TypeReference<>() {});
    }

    private List<String> readValue(JsonNode val) {
        if (val.isArray()) {
            return mapper.convertValue(val, new TypeReference<>() {});
        }
        return List.of(val.asText());
    }

    /** Subclasses instantiate their concrete type from normalized value and rules. */
    protected abstract T build(List<String> value, List<Rule> rules);

}
