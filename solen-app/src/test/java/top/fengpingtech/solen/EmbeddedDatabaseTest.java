package top.fengpingtech.solen;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class EmbeddedDatabaseTest {

    @Test
    void testHsqldb() throws Exception {
        String url = "jdbc:hsqldb:file:test-data";
        String username = "sa";
        String password = "";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {

            String sql = "insert into CATEGORY (nam) values ( ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "");
            }
        }
    }

    private String loadResource(String f) throws Exception {
        ClassPathResource resource = new ClassPathResource(f);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             InputStream in = resource.getInputStream()) {
            for (int b = in.read(); b != -1; b = in.read()) {
                out.write(b);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
