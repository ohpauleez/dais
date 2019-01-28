
package dais;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.Predicate;

import dais.ToInterceptor;
import dais.Interceptor;

public class Context extends HashMap<Object,Object> {

    public static final String QUEUE_KEY = "dais.queue";
    public static final String STACK_KEY = "dais.stack";
    public static final String TERMINATORS_KEY = "dais.terminators";
    public static final String ERROR_KEY = "dais.error";
    public static final String CHAIN_KEY = "dais.chain";
    //public static final String EXECUTOR_KEY = "dais.executor";

    public Context() {
        super(4, 2.f);
    }

    public Context withTerminators(List<Predicate<Map<Object,Object>>> terminators) {
        this.put(TERMINATORS_KEY, terminators);
        return this;
    }
    public Context withTerminators(Predicate<Map<Object,Object>>... terminators) {
        return this.withTerminators(Arrays.asList(terminators));
    }

    public Context withInterceptors(Collection<ToInterceptor> interceptors) {
        this.put(QUEUE_KEY, new ArrayDeque(Arrays.asList(interceptors.stream()
                                                                      .map(i -> i.toInterceptor())
                                                                      .toArray())));
        return this;
    }
    public Context withInterceptors(ToInterceptor... interceptors) {
        return this.withInterceptors(Arrays.asList(interceptors));
    }
    public Context withInterceptors(Interceptor... interceptors) {
        this.put(QUEUE_KEY, new ArrayDeque(Arrays.asList(interceptors)));
        return this;
    }

    public Context withStaticInterceptors(Interceptor... interceptors) {
        this.put(QUEUE_KEY, interceptors);
        return this;
    }

    public Context withKeyVals(Object... keyvals) {
        if ((keyvals.length % 2) != 0) {
            throw new IllegalArgumentException("Found a Context key without a value -- You must pass a value for all withKeyVals keys");
        }

        int kvLength = keyvals.length;
        for (int i =0; i < kvLength; i+=2) {
            this.put(keyvals[i], keyvals[i+1]);
        }
        return this;
    }
}

