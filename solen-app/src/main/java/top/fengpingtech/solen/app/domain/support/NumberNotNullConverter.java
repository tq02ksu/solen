package top.fengpingtech.solen.app.domain.support;

import javax.persistence.AttributeConverter;

abstract class NumberNotNullConverter <T extends Number> implements AttributeConverter<T, T> {

    abstract T getDefault();

    @Override
    public T convertToDatabaseColumn(T attribute) {
        return attribute == null ? getDefault() : attribute;

    }

    @Override
    public T convertToEntityAttribute(T dbData) {
        return getDefault().equals(dbData) ? null : dbData;
    }
}
