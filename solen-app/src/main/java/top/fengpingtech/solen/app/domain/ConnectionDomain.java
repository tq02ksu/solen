package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "connection")
public class ConnectionDomain {
    @Id
    private String connectionId;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "deviceId")
    private DeviceDomain device;
}
