package com.minecraft.launcher.model.version.arguments.jvm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.minecraft.launcher.model.rule.Rule;

import java.io.IOException;
import java.util.List;

public class JvmDeserializer extends JsonDeserializer<Jvm> {

    @Override
    public Jvm deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode root = jsonParser.readValueAsTree();

        if(root.isTextual()) {
            return new Jvm(List.of(root.asText()), null);
        }

        JsonNode rulesNode = root.get("rules");
        List<Rule> rules = (rulesNode == null)
                ? null
                : mapper.convertValue(rulesNode, new TypeReference<>() {});

        JsonNode val = root.get("value");
        if (val == null) {
            throw new JsonMappingException(jsonParser, "missing required field 'value'");
        }
        if (val.isArray()) {
            List<String> value = mapper.convertValue(val,
                    new TypeReference<>() {});
            return new Jvm(value, rules);
        }
        return new Jvm(List.of(val.asText()), rules);
    }

}
