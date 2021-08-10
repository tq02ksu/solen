package top.fengpingtech.solen.app.controller.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageableResponse<T> {
    private Long total;

    private List<T> data;
}
