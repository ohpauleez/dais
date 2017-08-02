
package dais;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;
import java.util.function.Function;
import java.util.function.Predicate;

import dais.Maps;
import dais.Interceptor;
import dais.Context; // Fluent API for the HashMap
import dais.Chain;

public class Example {


    public static Map<Object,Object> example() {
        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"))
                                   .withInterceptors(new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                                     ctx -> Maps.put(ctx, "leave-a", 11),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "b", 2),
                                                                     null, null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "c", 3),
                                                                     null, null));

        return Chain.execute(context);
    }

    public static Map<Object,Object> example2() {
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"));

        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        return Chain.execute(context,
                             Arrays.asList(new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                           ctx -> Maps.put(ctx, "leave-a", 11),
                                                           null),
                                           new Interceptor(ctx -> Maps.put(ctx, "b", 2),
                                                           null, null),
                                           new Interceptor(ctx -> Maps.put(ctx, "c", 3),
                                                           null, null)));
    }

    public static Map<Object,Object> example3() {
        // TODO: Make a Context class, subclass of HashMap, to improve the type inference and quiet this noise down
        Map context = Maps.mapOf("dais.terminators",
                                Arrays.asList((Predicate<Map<Object,Object>>) (Map<Object,Object> ctx) -> ctx.containsKey("b")));

        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        return Chain.execute(context,
                             Arrays.asList(new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                           ctx -> Maps.put(ctx, "leave-a", 11),
                                                           null),
                                           new Interceptor(ctx -> Maps.put(ctx, "b", 2),
                                                           null, null),
                                           new Interceptor(ctx -> Maps.put(ctx, "c", 3),
                                                           null, null)));
    }

    public static Map<Object,Object> example4() {
        // TODO: Make a Context class, subclass of HashMap, to improve the type inference and quiet this noise down
        Map context = Maps.mapOf("dais.queue",
                                 new ArrayDeque(
                                     Arrays.asList(new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "a", 1),
                                                               (Map<Object,Object> ctx) -> Maps.put(ctx, "leave-a", 11),
                                                               Function.identity()),
                                               new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "b", 2),
                                                               Function.identity(),
                                                               Function.identity()),
                                               new Interceptor((Map<Object,Object> ctx) -> Maps.put(ctx, "c", 3),
                                                               Function.identity(),
                                                               Function.identity()))),

                                "dais.terminators",
                                Arrays.asList((Predicate<Map<Object,Object>>) (Map<Object,Object> ctx) -> ctx.containsKey("b")));

        return Chain.execute(context);
    }

}
