package top.fengpingtech.solen.server.netty;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EventModel {
    int value() default 0;

    String desc() default "";
}
