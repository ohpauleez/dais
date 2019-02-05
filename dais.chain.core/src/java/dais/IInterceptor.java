
package dais;

import java.util.function.Function;
import java.util.Map;


//TODO: Consider IOptionalInterceptor that returns Optionals

public interface IInterceptor {
    public Function<Map<Object,Object>,Map<Object,Object>> getEnter();
    public Function<Map<Object,Object>,Map<Object,Object>> getLeave();
    public Function<Map<Object,Object>,Map<Object,Object>> getError();

    static Map<Object,Object> enter(IInterceptor interceptor, Map<Object,Object> context) {
        Function<Map<Object,Object>,Map<Object,Object>> eFn = (interceptor != null) ? interceptor.getEnter() : null;
        if (eFn != null) {
            return eFn.apply(context);
        } else {
            return context;
        }
    }

    static Map<Object,Object> leave(IInterceptor interceptor, Map<Object,Object> context) {
        Function<Map<Object,Object>,Map<Object,Object>> lFn = (interceptor != null) ? interceptor.getLeave() : null;
        if (lFn != null) {
            return lFn.apply(context);
        } else {
            return context;
        }
    }

    static Map<Object,Object> error(IInterceptor interceptor, Map<Object,Object> context) {
        Function<Map<Object,Object>,Map<Object,Object>> errFn = (interceptor != null) ? interceptor.getError() : null;
        if (errFn != null) {
            return errFn.apply(context);
        } else {
            return context;
        }
    }
}

