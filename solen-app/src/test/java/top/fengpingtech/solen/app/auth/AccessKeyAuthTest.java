package top.fengpingtech.solen.app.auth;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import top.fengpingtech.solen.SolenApplicationTests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessKeyAuthTest extends SolenApplicationTests {
    private String appKey = "test";
    private String secretKey = "test";
    private String passKey = "test";

    @Autowired
    MockMvc mockMvc;

    private String sign(String url, Long requestTime) {
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

    @Test
    public void testRemoteServer() throws Exception {
        String appKey = "test", secretKey = "test";
        Long requestTime = System.currentTimeMillis() / 1000;
        String sign = sign("/api/list", requestTime);

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .get()
                .url(HttpUrl.parse("http://iot.fengping-tech.top/api/list").newBuilder()
                        .addQueryParameter("requestTime", String.valueOf(requestTime))
                        .addQueryParameter("appKey", appKey)
                        .addQueryParameter("sign", sign)
                        .build())
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
}
