
package dais;

import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.Collection;

import java.util.ArrayDeque;

import java.util.function.Predicate;

import dais.IInterceptor;

// NOTE: This is programmed against the common denominator -- a Map and null checks (no Optionals)
//       Use the dais.Maps utility class for Optional-oriented interactions with the Context map

public class Chain {

    //TODO: For now, let's just execute only forward
    public static final Map<Object,Object> execute(Map<Object,Object> context) {
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get("terminators");

        Object queue = context.get("queue");
        if (queue == null) {
            return context;
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>) context.get("stack");
        stack = (stack != null) ? stack : new ArrayDeque<IInterceptor>();

        if (queue instanceof Deque) {
            return handleEnter(context, (Deque<IInterceptor>) queue, stack, terminators);
        } else if (queue instanceof IInterceptor[]) {
            return handleArrayEnter(context, (IInterceptor[]) queue, stack, terminators);
        } else {
            return context;
        }
    }
    public static final Map<Object,Object> execute(Map<Object,Object> context, Collection<IInterceptor> queue) {
        context.put("queue", new ArrayDeque<IInterceptor>(queue));
        return execute(context);
    }

    public static final Map<Object,Object> handleEnter(Map<Object,Object> context,
                                                       Deque<IInterceptor> queue,
                                                       Deque<IInterceptor> stack,
                                                       List<Predicate<Map<Object,Object>>> terminators) {

        while (queue.size() > 0) {

            IInterceptor interceptor = queue.pollFirst();
            if (interceptor == null) {
                context.remove("queue");
                return handleLeave(context, stack);
            }
            stack.offerFirst(interceptor);

            try {
                context = IInterceptor.enter(interceptor, context);

                if (context.get("error") != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put("error", t);
                return handleError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove("queue");
                        return handleLeave(context, stack);
                    }
                }
            }
        }
        return context;

    }
    public static final Map<Object,Object> handleLeave(Map<Object,Object> context,
                                                       Deque<IInterceptor> stack) {
        for (IInterceptor interceptor : stack) {
            try {
                context = IInterceptor.leave(interceptor, context);
                if (context.get("error") != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put("error", t);
                return handleError(context, interceptor, stack);
            }
        }
        return context;
    }
    public static final Map<Object,Object> handleError(Map<Object,Object> context,
                                                       IInterceptor erroredInterceptor,
                                                       Deque<IInterceptor> stack) {
        for (IInterceptor interceptor : stack) {
            Object err = context.get("error");
            if (err != null) {
                context = IInterceptor.error(interceptor, context);
            } else {
                return handleLeave(context, stack);
            }
        }
        return context;
    }

    public static final Map<Object,Object> handleArrayEnter(Map<Object,Object> context,
                                                            IInterceptor[] queue,
                                                            Deque<IInterceptor> stack,
                                                            List<Predicate<Map<Object,Object>>> terminators) {

        for(IInterceptor interceptor : queue) {
            if (interceptor == null) {
                context.remove("queue");
                return handleLeave(context, stack);
            }
            stack.offerFirst(interceptor);

            try {
                context = IInterceptor.enter(interceptor, context);

                if (context.get("error") != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put("error", t);
                return handleError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove("queue");
                        return handleLeave(context, stack);
                    }
                }
            }
        }
        return context;
    }

}

