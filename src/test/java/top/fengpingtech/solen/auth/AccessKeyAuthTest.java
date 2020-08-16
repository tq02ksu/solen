package top.fengpingtech.solen.auth;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import top.fengpingtech.solen.SolenApplicationTests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessKeyAuthTest extends SolenApplicationTests {
    private String appKey = "test";
    private String secretKey = "$2a$10$thdy4TLr809cq6kpHdT1JOeTS.IiZfVFnjfkuxgqs.cSj53ffCWV.";
    private String passKey = "test";

    @Autowired
    MockMvc mockMvc;

    String sign (String url, Long requestTime) {
        String msg = String.format("%s%s%s%s", secretKey, requestTime, url, secretKey);
        return new AppKeySignatureChecker().digest(msg);
    }

    @Test
    public void testAccessKeyAuth() throws Exception {
        Long requestTime = System.currentTimeMillis() / 1000;
        mockMvc.perform(get("/api/list")
                .param("requestTime", String.valueOf(requestTime))
                .param("appKey", appKey)
                .param("sign", sign("/api/list", requestTime))
        ).andExpect(status().isOk());
    }
}
