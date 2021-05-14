package top.fengpingtech.solen.server;

import java.util.concurrent.atomic.AtomicLong;

public interface IdGenerator {
    IdGenerator DEFAULT = new IdGenerator() {
        private AtomicLong holder = new AtomicLong(0L);

        @Override
        public Long nextVal() {
            return holder.getAndIncrement();
        }
    };

    Long nextVal();
}
