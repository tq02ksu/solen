package top.fengpingtech.solen.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Coordinate {
    // longitude
    private double lng;
    // latitude
    private double lat;
}
