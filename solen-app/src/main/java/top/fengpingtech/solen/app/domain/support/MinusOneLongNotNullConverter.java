package top.fengpingtech.solen.app.domain.support;

public class MinusOneLongNotNullConverter extends NumberNotNullConverter<Long> {

    @Override
    Long getDefault() {
        return -1L;
    }
}
