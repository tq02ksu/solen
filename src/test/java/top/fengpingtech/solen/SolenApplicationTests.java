package top.fengpingtech.solen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.fengpingtech.solen.model.Connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SolenApplicationTests {
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testJson() throws Exception {
		List<Connection> list = new ArrayList<>(Arrays.asList(	new Connection()));
		int total = list.size();
		int pageNo = 1;
		int pageSize = 10;
		int start = Integer.max(0, (pageNo - 1) * pageSize);
		int size = Integer.max(0, Integer.min(pageSize, total - start));
		Object obj = new HashMap<String, Object>() {
			{
				put("total", total);
				put("data", list.subList(start, size));
			}
		};

		String json = objectMapper.writeValueAsString(obj);
		System.out.println(json);
	}
}
