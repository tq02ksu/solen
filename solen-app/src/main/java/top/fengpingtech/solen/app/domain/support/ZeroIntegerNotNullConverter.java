package top.fengpingtech.solen.app.domain.support;

public class ZeroIntegerNotNullConverter extends NumberNotNullConverter<Integer> {

    @Override
    Integer getDefault() {
        return 0;
    }
}
