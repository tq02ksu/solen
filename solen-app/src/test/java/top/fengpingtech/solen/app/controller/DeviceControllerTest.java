package top.fengpingtech.solen.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import top.fengpingtech.solen.app.SolenApplicationTests;
import top.fengpingtech.solen.app.controller.bean.DeviceBean;
import top.fengpingtech.solen.app.controller.bean.DeviceQueryRequest;
import top.fengpingtech.solen.app.controller.bean.PageableResponse;

import static org.junit.jupiter.api.Assertions.*;

class DeviceControllerTest extends SolenApplicationTests {

    @Autowired
    private DeviceController deviceController;

    @Test
    void list() {
        DeviceQueryRequest request = new DeviceQueryRequest();
        // pageSize=10&pageNo=1&deviceId=&appKey=test&requestTime=1655040830&sign=c023ca9a6f93bf484a81b063876fd9cc
        request.setPageNo(1);
        request.setPageSize(10);
        PageableResponse<DeviceBean> response = deviceController.list(request);
        System.out.println(response);
    }

    @Test
    void detail() {
        DeviceQueryRequest request = new DeviceQueryRequest();
        // pageSize=10&pageNo=1&deviceId=&appKey=test&requestTime=1655040830&sign=c023ca9a6f93bf484a81b063876fd9cc
        request.setPageNo(1);
        request.setPageSize(10);
        PageableResponse<DeviceBean> response = deviceController.list(request);

        assertTrue(response.getTotal() > 0);

        String deviceId = response.getData().get(0).getDeviceId();

        DeviceBean device = deviceController.detail(deviceId);
        System.out.println(device);
    }
}