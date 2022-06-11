package top.fengpingtech.solen.app.domain.support;

public class MinusOneIntegerNotNullConverter extends NumberNotNullConverter<Integer> {

    @Override
    Integer getDefault() {
        return -1;
    }
}
