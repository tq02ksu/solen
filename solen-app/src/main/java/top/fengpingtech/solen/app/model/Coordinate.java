package top.fengpingtech.solen.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class Coordinate {
    private CoordinateSystem system;

    // longitude
    private double lng;
    // latitude
    private double lat;
}
