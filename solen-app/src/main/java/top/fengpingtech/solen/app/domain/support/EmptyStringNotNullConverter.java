package top.fengpingtech.solen.app.domain.support;

import javax.persistence.AttributeConverter;

public class EmptyStringNotNullConverter implements AttributeConverter<String, String> {
    static final String EMPTY = "";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? EMPTY : attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData.equals(EMPTY) ? null : dbData;
    }
}
