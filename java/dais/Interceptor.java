
package dais;

import dais.ToInterceptor;
import dais.IInterceptor;
import dais.Maps;

import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Stream;

public class Interceptor implements IInterceptor, ToInterceptor {

    public final Function<Map<Object,Object>,Map<Object,Object>> enterFn;
    public final Function<Map<Object,Object>,Map<Object,Object>> leaveFn;
    public final Function<Map<Object,Object>,Map<Object,Object>> errorFn;
    public final Map<String, Function<Map<Object,Object>,Map<Object,Object>>> stages;

    public Interceptor(Function<Map<Object,Object>,Map<Object,Object>> enter,
                       Function<Map<Object,Object>,Map<Object,Object>> leave,
                       Function<Map<Object,Object>,Map<Object,Object>> error) {
        // Note: This doesn't allow for nulls
        //this.stages = Collections.unmodifiableMap(Stream.of(
        //                Maps.entry("enter", enter),
        //                Maps.entry("leave", leave),
        //                Maps.entry("error", error)).
        //                collect(Maps.entriesToMap()));
        Map<String, Function<Map<Object,Object>,Map<Object,Object>>> tempMap = new HashMap<>();
        tempMap.put("enter", enter);
        tempMap.put("leave", leave);
        tempMap.put("error", error);
        this.stages = Collections.unmodifiableMap(tempMap);
        this.enterFn = enter;
        this.leaveFn = leave;
        this.errorFn = error;
    }

    public Interceptor(Map<String, Function<Map<Object,Object>,Map<Object,Object>>> initialStages) {
        this.stages = Collections.unmodifiableMap(initialStages);
        this.enterFn = this.stages.get("enter");
        this.leaveFn = this.stages.get("leave");
        this.errorFn = this.stages.get("error");
    }

    /* IInterceptor
     * -----------------*/
    public Function<Map<Object,Object>,Map<Object,Object>> getEnter() {
        return this.enterFn;
    }
    public Function<Map<Object,Object>,Map<Object,Object>> getLeave() {
        return this.leaveFn;
    }
    public Function<Map<Object,Object>,Map<Object,Object>> getError() {
        return this.errorFn;
    }

    public Function<Map<Object,Object>,Map<Object,Object>> getStage(String stageName) {
        return this.stages.get(stageName);
    }

    /* ToInterceptor
     * ------------------*/
    public IInterceptor toInterceptor() {
        return this;
    }

    //public String toString() {
    //    return "Interceptor{enter: "+this.enterFn+", leave: "+this.leaveFn+", error: "+this.errorFn+"}";
    //}
}

