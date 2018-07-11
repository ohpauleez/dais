
package dais;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;
import java.util.function.Function;
import java.util.function.Predicate;

import java.util.Random;

import dais.Maps;
import dais.Interceptor;
import dais.Context; // Fluent API for the HashMap
import dais.Chain;

public class Example {

    public static final Interceptor interA = new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                             ctx -> Maps.put(ctx, "leave-a", 11),
                                                             null);
    public static final Interceptor interB = new Interceptor(ctx -> Maps.put(ctx, "b", 2), null, null);
    public static final Interceptor interC = new Interceptor(ctx -> Maps.put(ctx, "c", 3), null, null);


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

    public static Map<Object,Object> example1() {
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"))
                                   .withInterceptors(interA, interB, interC);

        return Chain.execute(context);
    }

    public static Map<Object,Object> exampleLong() {
        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("n"))
                                   .withInterceptors(
                                                     new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                                     ctx -> Maps.put(ctx, "leave-a", 11),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "b", 2),
                                                                     null, null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "c", 3),
                                                                     null, null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "d", 4),
                                                                     ctx -> Maps.put(ctx, "leave-d", 14),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "e", 5),
                                                                     ctx -> Maps.put(ctx, "leave-e", 15),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "a", 6),
                                                                     ctx -> Maps.put(ctx, "leave-f", 16),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "g", 7),
                                                                     null,
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "h", 8),
                                                                     ctx -> Maps.put(ctx, "leave-h", 18),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "b", 9),
                                                                     ctx -> Maps.put(ctx, "leave-i", 19),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "j", 10),
                                                                     ctx -> Maps.put(ctx, "leave-j", 101),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "k", 11),
                                                                     ctx -> Maps.put(ctx, "leave-k", 111),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "c", 12),
                                                                     ctx -> Maps.put(ctx, "leave-l", 121),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "m", 13),
                                                                     ctx -> Maps.put(ctx, "leave-m", 131),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "n", 14),
                                                                     ctx -> Maps.put(ctx, "leave-n", 141),
                                                                     null));

        return Chain.execute(context);
    }

    public static Map<Object,Object> exampleLongRandom() {
        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        Random random = new Random();
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("n"))
                                   .withInterceptors(new Interceptor(ctx -> Maps.put(ctx, "a", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     ctx -> Maps.put(ctx, "leave-a", (int)ctx.get("a")+10),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "b", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     null, null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "c", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     null, null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "d", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     ctx -> Maps.put(ctx, "leave-d", (int)ctx.get("d")+10),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "e", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     ctx -> Maps.put(ctx, "leave-e", (int)ctx.get("e")+10),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "a", 6),
                                                                     ctx -> Maps.put(ctx, "leave-f", (int)ctx.get("a")+10),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "g", 7),
                                                                     null,
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "h", 8),
                                                                     ctx -> Maps.put(ctx, "leave-h", 18),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "b", 9),
                                                                     ctx -> Maps.put(ctx, "leave-i", 19),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "j", random.nextInt(Integer.MAX_VALUE / 32)),
                                                                     ctx -> Maps.put(ctx, "leave-j", (int)ctx.get("j")+100),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "k", 11),
                                                                     ctx -> Maps.put(ctx, "leave-k", 111),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "c", 12),
                                                                     ctx -> Maps.put(ctx, "leave-l", 121),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "m", 13),
                                                                     ctx -> Maps.put(ctx, "leave-m", 131),
                                                                     null),
                                                     new Interceptor(ctx -> Maps.put(ctx, "n", 14),
                                                                     ctx -> Maps.put(ctx, "leave-n", 141),
                                                                     null));

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

    public static Map<Object,Object> example2S() {
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"));

        return Chain.execute(context,
                             Arrays.asList(interA, interB, interC));
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

    public static Map<Object,Object> exampleStatic() {
        // Note: When you're actually using the Chain,
        //       you'd be better off creating static Interceptors in a class once
        //       and just always reusing them.
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"))
                                   .withStaticInterceptors(new Interceptor(ctx -> Maps.put(ctx, "a", 1),
                                                                           ctx -> Maps.put(ctx, "leave-a", 11),
                                                                           null),
                                                           new Interceptor(ctx -> Maps.put(ctx, "b", 2),
                                                                           null, null),
                                                           new Interceptor(ctx -> Maps.put(ctx, "c", 3),
                                                                           null, null));

        return Chain.execute(context);
    }

    public static Map<Object,Object> exampleStatic1() {
        Map context = new Context().withTerminators(ctx -> ctx.containsKey("b"))
                                   .withStaticInterceptors(interA, interB, interC);

        return Chain.execute(context);
    }
}

