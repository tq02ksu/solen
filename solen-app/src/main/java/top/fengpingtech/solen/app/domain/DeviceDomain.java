package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "device")
public class DeviceDomain {
    @Id
    private String deviceId;

    @Enumerated(value = EnumType.STRING)
    private ConnectionStatus status;

    private Long lac;

    private Long ci;

    private String idCode;

    private Integer inputStat;

    private Integer outputStat;

    private Integer rssi;

    private Double voltage;

    private Double temperature;

    private Integer gravity;

    private Integer uptime;

    @Embedded
    private Coordinate coordinate;

    @OneToMany
    private List<ConnectionDomain> connection;
}
