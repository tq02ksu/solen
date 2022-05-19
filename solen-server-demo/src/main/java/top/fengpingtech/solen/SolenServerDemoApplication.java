package top.fengpingtech.solen;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import top.fengpingtech.solen.server.IdGenerator;
import top.fengpingtech.solen.server.SolenServer;
import top.fengpingtech.solen.server.config.ServerProperties;
import top.fengpingtech.solen.server.netty.SolenNettyServer;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class SolenServerDemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(SolenServerDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SolenServerDemoApplication.class, args);
    }

    @Bean
    public SolenServer serverStart() {
        ServerProperties properties = new ServerProperties();
        properties.setPort(54321); // 设置监听端口
        properties.setEventProcessor(events -> System.out.println("receiving events: " + events)); // 设置消息处理函数
        properties.setEventIdGenerator(new IdGenerator() {
            final AtomicLong counter = new AtomicLong(0);
            @Override
            public Long nextVal() {
                return counter.getAndIncrement();
            }
        }); // 提供事件ID生成器，如果不关注也可以
        properties.setIoThreads(2); // IO线程数，一般为2-4就够
        properties.setDaemon(true);
        properties.setWorkerThreads(4); // 工作线程数，可以根据服务器处理能力和事件处理速度综合考虑，为CPU核数的2-10倍。一般不要超过500
        SolenNettyServer solenNettyServer = new SolenNettyServer(properties);
        solenNettyServer.start();

        return solenNettyServer;
    }

    @Bean
    CommandLineParser commandLineParser() {
        return new DefaultParser();
    }

    @Bean
    CommandLineRunner applicationHolder(SolenServer solenServer) {
        return arguments -> {
            try (Scanner scanner = new Scanner(System.in)) {
                for (String line = ""; !line.trim().equals("close"); line = scanner.nextLine()) {
                    String[] args = line.trim().split("[ ]+");
                    if (args.length == 0) {
                        continue;
                    }

                    if (args[0].equalsIgnoreCase("send")) {
                        handleSend(solenServer, args);
                        continue;
                    }

                    String command = line.trim();
                    if (!command.isEmpty()) {
                        System.out.println("unknown command: " + command);
                    }
                }
            }
        };
    }

    private void handleSend(SolenServer solenServer, String[] args) {
        try {
            Assert.isTrue(args.length == 3, "argument length must be three");
            String deviceId = args[1];
            String message = args[2];
            solenServer.getDeviceService().sendMessage(deviceId, message);
            System.out.println("message sent");
        } catch (Exception e) {
            logger.error("error while handle send", e);
        }
    }
}
