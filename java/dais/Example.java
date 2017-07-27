
package dais;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;
import java.util.function.Function;
import java.util.function.Predicate;

import dais.Maps;
import dais.Interceptor;
import dais.Chain;

public class Example {


    public static Map<Object,Object> example() {
        // TODO: Make a Context class, subclass of HashMap, to improve the type inference and quiet this noise down
        Map context = Maps.mapOf("terminators",
                                Arrays.asList((Predicate<Map<Object,Object>>) (Map<Object,Object> ctx) -> ctx.containsKey("b")));

        return Chain.execute(context,
                             Arrays.asList(new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                           ctx -> Maps.put(ctx, "leave-a", 11),
                                                           null),
                                           new Interceptor(ctx -> Maps.put(ctx, "b", 1),
                                                           null, null),
                                           new Interceptor(ctx -> Maps.put(ctx, "c", 1),
                                                           null, null)));
    }

    public static Map<Object,Object> example2() {
        // TODO: Make a Context class, subclass of HashMap, to improve the type inference and quiet this noise down
        Map context = Maps.mapOf("queue",
                                 new ArrayDeque(
                                     Arrays.asList(new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "a", 1),
                                                               (Map<Object,Object> ctx) -> Maps.put(ctx, "leave-a", 11),
                                                               Function.identity()),
                                               new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "b", 1),
                                                               Function.identity(),
                                                               Function.identity()),
                                               new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "c", 1),
                                                               Function.identity(),
                                                               Function.identity()))),

                                "terminators",
                                Arrays.asList((Predicate<Map<Object,Object>>) (Map<Object,Object> ctx) -> ctx.containsKey("b")));

        return Chain.execute(context);
    }

}
