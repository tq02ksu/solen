package top.fengpingtech.solen.app.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeadersOverrideRequest extends HttpServletRequestWrapper {
    private final Map<String, String> headers;
    public HeadersOverrideRequest(HttpServletRequest request, String... headers) {
        super(request);
        this.headers = new HashMap<>();
        for (int i = 0; i < headers.length / 2; i ++) {
            if (headers[2 * i + 1] != null) {
                this.headers.put(headers[2 * i], headers[2 * i + 1]);
            }
        }
    }

    @Override
    public String getHeader(String name) {
        return headers.containsKey(name) ? headers.get(name) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (headers.containsKey(name)) {
            Iterator<String> it = Collections.singletonList(headers.get(name)).iterator();
            return new IteratorEnumeration<>(it);
        }

        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Iterator<String> list = Stream.concat(Collections.list(super.getHeaderNames()).stream(), headers.keySet().stream()).collect(Collectors.toList()).iterator();
        return new IteratorEnumeration<>(list);
    }

    public static class IteratorEnumeration<T> implements Enumeration<T> {
        private final Iterator<T> iterator;

        IteratorEnumeration(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

        public T nextElement() {
            return this.iterator.next();
        }
    }

}
