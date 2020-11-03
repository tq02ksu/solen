package top.fengpingtech.solen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tenant {
    private String id;

    private String appKey;

    private String secretKey;

    private String passKey;

    private String desc;

    private List<String> devicePatterns;

    private List<String> roles;
}
