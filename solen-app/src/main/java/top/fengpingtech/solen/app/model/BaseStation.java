package top.fengpingtech.solen.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseStation {
    private int valid;
    private int mcc;
    private int mnc;
    private int lac;
    private int cellId;
    private int signal;
}
