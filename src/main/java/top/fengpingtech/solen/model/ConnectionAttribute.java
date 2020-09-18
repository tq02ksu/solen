package top.fengpingtech.solen.model;

import java.util.HashMap;

public class ConnectionAttribute extends HashMap<String, String> {
    public ConnectionAttribute(Connection connection) {
        if (connection.getLac() != null) {
            put("lac", String.valueOf(connection.getLac()));
        }
        if (connection.getCi() != null) {
            put("ci", String.valueOf(connection.getCi()));
        }

        if (connection.getInputStat() != null) {
            put("inputStat", String.valueOf(connection.getInputStat()));
        }

        if (connection.getOutputStat() != null) {
            put("outputStat", String.valueOf(connection.getOutputStat()));
        }

        // coordinate
        if (connection.getCoordinate() != null) {
            put("coordinateLat", String.valueOf(connection.getCoordinate().getLat()));
            put("coordinateLng", String.valueOf(connection.getCoordinate().getLng()));
        }
    }
}
