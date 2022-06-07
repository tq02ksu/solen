package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.app.model.Coordinate;

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

    private Integer status;

    private Long lac;

    private Long ci;

    @Embedded
    private Coordinate coordinate;

    @OneToMany
    private List<ConnectionDomain> connection;
}
