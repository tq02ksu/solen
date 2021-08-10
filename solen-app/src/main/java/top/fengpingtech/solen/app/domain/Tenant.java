package top.fengpingtech.solen.app.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Tenant {
    @Id
    @GeneratedValue
    private Long id;

    private String appKey;

    private String desc;

    private String secretKey;

    private String passKey;
}
