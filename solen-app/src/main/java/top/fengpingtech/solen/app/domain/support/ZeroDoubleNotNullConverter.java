package top.fengpingtech.solen.app.domain.support;

public class ZeroDoubleNotNullConverter extends NumberNotNullConverter<Double> {

    @Override
    Double getDefault() {
        return 0D;
    }
}
