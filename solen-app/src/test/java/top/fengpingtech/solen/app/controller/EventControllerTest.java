package top.fengpingtech.solen.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import top.fengpingtech.solen.app.SolenApplicationTests;

import static org.junit.jupiter.api.Assertions.*;

class EventControllerTest extends SolenApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list() throws Exception {
        String appKey = "admin";
        String appSecret = "123asdsecret123asdfadmin";
        long requestTime = System.currentTimeMillis();
        String uri = "/api/event/list";
        String sign = md5(appSecret + requestTime + uri + appSecret);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(
                uri + "?appKey=" + appKey + "&requestTime=" + requestTime + "&sign=" + sign))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }
}