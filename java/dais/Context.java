
package dais;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.Predicate;

import dais.ToInterceptor;
import dais.Interceptor;

public class Context extends HashMap<Object,Object> {

    public Context() {
        super(4, 2.f);
    }

    public Context withTerminators(List<Predicate<Map<Object,Object>>> terminators) {
        this.put("dais.terminators", terminators);
        return this;
    }
    public Context withTerminators(Predicate<Map<Object,Object>>... terminators) {
        return this.withTerminators(Arrays.asList(terminators));
    }

    public Context withInterceptors(List<ToInterceptor> interceptors) {
        this.put("dais.queue", new ArrayDeque(Arrays.asList(interceptors.stream()
                                                                   .map(i -> i.toInterceptor())
                                                                   .toArray())));
        return this;
    }
    public Context withInterceptors(ToInterceptor... interceptors) {
        return this.withInterceptors(Arrays.asList(interceptors));
    }
    public Context withInterceptors(Interceptor... interceptors) {
        this.put("dais.queue", new ArrayDeque(Arrays.asList(interceptors)));
        return this;
    }
}
