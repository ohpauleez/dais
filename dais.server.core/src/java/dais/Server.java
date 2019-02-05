
package dais;

import java.util.Map;
import java.util.function.Function;

public interface Server {

    public static final String SERVER_KEY = "dais.server";
    public static final String SERVEROPTS_KEY = "dais.serverOptions";
    public static final String STARTFN_KEY = "dais.startFn";
    public static final String STOPFN_KEY = "dais.stopFn";

    //public Map<Object,Object> start();
    public Map<Object,Object> startWith(Map<Object,Object> serviceMap);
    public Map<Object,Object> stopWith(Map<Object,Object> serviceMap);

    static Map<Object,Object> addServerToServiceMap(Server server, Map<Object,Object> serviceMap) {
        serviceMap.put(SERVER_KEY, server);
        serviceMap.put(STARTFN_KEY, (Function<Map<Object,Object>,Map<Object,Object>>)(Map<Object,Object> ctx) -> server.startWith(ctx));
        serviceMap.put(STOPFN_KEY, (Function<Map<Object,Object>,Map<Object,Object>>)(Map<Object,Object> ctx) -> server.stopWith(ctx));
        return serviceMap;
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getServiceFn(Map<Object,Object> serviceMap, String fnKey) {
        Object maybeServiceFn = serviceMap.get(fnKey);
        if (maybeServiceFn != null) {
            return (Function<Map<Object,Object>,Map<Object,Object>>)maybeServiceFn;
        }
        return null;
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getStartFn(Map<Object,Object> serviceMap) {
        return getServiceFn(serviceMap, STARTFN_KEY);
    }

    static Function<Map<Object,Object>,Map<Object,Object>> getStopFn(Map<Object,Object> serviceMap) {
        return getServiceFn(serviceMap, STOPFN_KEY);
    }

    static Map<Object,Object> start(Map<Object,Object> serviceMap) {
        Function<Map<Object,Object>,Map<Object,Object>> maybeStartFn = getStartFn(serviceMap);
        if (maybeStartFn != null) {
            return maybeStartFn.apply(serviceMap);
        }
        return serviceMap;
    }

    static Map<Object,Object> stop(Map<Object,Object> serviceMap) {
        Function<Map<Object,Object>,Map<Object,Object>> maybeStopFn = getStopFn(serviceMap);
        if (maybeStopFn != null) {
            return maybeStopFn.apply(serviceMap);
        }
        return serviceMap;
    }
}

