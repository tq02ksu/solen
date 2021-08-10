package top.fengpingtech.solen.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.fengpingtech.solen.server.model.EventType;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class EventDomain {
    @Id
    @GeneratedValue
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "deviceId")
    private Device device;

    @Enumerated
    private EventType type;

    @Temporal(TemporalType.TIMESTAMP)
    private Date time;

    @Convert(converter = MapConverter.class)
    private Map<String, String> details;
}
