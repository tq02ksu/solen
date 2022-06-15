package top.fengpingtech.solen.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.DigestUtils;
import top.fengpingtech.solen.app.SolenApplicationTests;
import top.fengpingtech.solen.app.controller.bean.DeviceBean;
import top.fengpingtech.solen.app.controller.bean.DeviceQueryRequest;
import top.fengpingtech.solen.app.controller.bean.PageableResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DeviceControllerTest extends SolenApplicationTests {

    @Autowired
    private MockMvc mockMvc;

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
    void detail() throws Exception {

        DeviceQueryRequest request = new DeviceQueryRequest();
        // pageSize=10&pageNo=1&deviceId=&appKey=test&requestTime=1655040830&sign=c023ca9a6f93bf484a81b063876fd9cc
        request.setPageNo(1);
        request.setPageSize(10);
        PageableResponse<DeviceBean> response = deviceController.list(request);

        assertTrue(response.getTotal() > 0);

        String deviceId = response.getData().get(0).getDeviceId();


        String appKey = "admin";
        String appSecret = "123asdsecret123asdfadmin";
        long requestTime = System.currentTimeMillis();
        String uri = "/api/device/" + deviceId;
        String sign = md5(appSecret + requestTime + uri + appSecret);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(
                uri + "?appKey=" + appKey + "&requestTime=" + requestTime + "&sign=" + sign ))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    private String md5(String s) {
        return DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
    }
}