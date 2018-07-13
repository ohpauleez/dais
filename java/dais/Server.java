
package dais;

import java.util.Map;
import java.util.function.Function;

public interface Server {

    public static final String SERVER_KEY = "dais.server";
    public static final String SERVEROPTS_KEY = "dais.serverOptions";
    public static final String STARTFN_KEY = "dais.startFn";
    public static final String STOPFN_KEY = "dais.stopFn";

    //public Map<Object,Object> start();
    public Map<Object,Object> startWith(Map<Object,Object> context);
    public Map<Object,Object> stopWith(Map<Object,Object> context);

    static Map<Object,Object> addServerToContext(Server server, Map<Object,Object> context) {
        context.put(SERVER_KEY, server);
        context.put(STARTFN_KEY, (Function<Map<Object,Object>,Map<Object,Object>>)(Map<Object,Object> ctx) -> server.startWith(ctx));
        context.put(STOPFN_KEY, (Function<Map<Object,Object>,Map<Object,Object>>)(Map<Object,Object> ctx) -> server.stopWith(ctx));
        return context;
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getCtxFn(Map<Object,Object> context, String fnKey) {
        Object maybeCtxFn = context.get(fnKey);
        if (maybeCtxFn != null) {
            return (Function<Map<Object,Object>,Map<Object,Object>>)maybeCtxFn;
        }
        return null;
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getStartFn(Map<Object,Object> context) {
        return getCtxFn(context, STARTFN_KEY);
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getStopFn(Map<Object,Object> context) {
        return getCtxFn(context, STOPFN_KEY);
    }

    static Map<Object,Object> start(Map<Object,Object> context) {
        Function<Map<Object,Object>,Map<Object,Object>> maybeStartFn = getStartFn(context);
        if (maybeStartFn != null) {
            return maybeStartFn.apply(context);
        }
        return context;
    }

    static Map<Object,Object> stop(Map<Object,Object> context) {
        Function<Map<Object,Object>,Map<Object,Object>> maybeStopFn = getStopFn(context);
        if (maybeStopFn != null) {
            return maybeStopFn.apply(context);
        }
        return context;
    }
}

