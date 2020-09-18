package top.fengpingtech.solen;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class LETest {
    public static void main(String[] args) {
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
        cache.get("asf", key -> "ssss");

        System.out.println(cache.asMap());
    }
}
