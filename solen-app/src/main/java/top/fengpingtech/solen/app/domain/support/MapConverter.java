package top.fengpingtech.solen.app.domain.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Map;

public class MapConverter implements AttributeConverter<Map<String, String>,String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("error serialize json", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(
                    Map.class, String.class, String.class);
            return objectMapper.readValue(dbData, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("error while read json", e);
        }
    }
}
