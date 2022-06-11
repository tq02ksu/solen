package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Coordinate {

    private CoordinateSystem system;

    // longitude
    private Double lng;

    // latitude
    private Double lat;
}
