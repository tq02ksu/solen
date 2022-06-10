package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class Coordinate {

    @Transient
    private CoordinateSystem system;

    // longitude
    private Double lng;
    // latitude
    private Double lat;
}
