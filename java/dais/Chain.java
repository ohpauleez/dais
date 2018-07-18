
package dais;

import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.Collection;

import java.util.ArrayDeque;

import java.util.function.Predicate;

import dais.IInterceptor;
import dais.Context;

// NOTE: This is programmed against the common denominator -- a Map and null checks (no Optionals)
//       Use the dais.Maps utility class for Optional-oriented interactions with the Context map

/**
 * Dais Chain
 *
 * Given a Context (Map) which contains a "dais.queue" of IInterceptors, execute the queue sequentially, and return the final Context.
 *
 * A series of rules/decisions are applied to decide how to execute a Chain.
 *
 * If the queue is empty, processing is finished and the Context is returned.
 * Otherwise, get the first IInterceptor off of the front of the queue.
 *
 * If that Interceptor is null, remove the "dais.queue" from the Context and process the Leave Phase of the chain.
 * Otherwise, place the Interceptor on the "dais.stack", so it can be processed in the Leave Phase later.
 * Execute the Enter Phase of the Interceptor, returning a new/updated Context.
 *
 * If there is an "error" in the Context, handle the Error Phase.
 * Errors are handled by calling the `error` method of the IInterceptors in the Context's stack.
 * If the error is handled, the rest of the stack is process as a Leave Phase, calling the `leave` method of the remaining IInterceptors.
 *
 * If there are no errors, the Terminator Predicates are checked.
 * If any terminator returns true, the "dais.queue" is removed from the Context and the Leave Phase is executed.
 *
 * Otherwise, keep looping through the queue (see the top of this doc block), until it is emtpy.
 *
 * Note: All modifications to the queue and stack execution should be made directly against the references to those objects.
 *       The queue is not rebound after Interceptor execution (when an updated context is returned).
 *       There are no checks in place to see if a program is using undefined behavior -- it is undefined.
 *
 *       To short-circuit chain execution, an Interceptor can call `clear` on the queue.
 */
public class Chain {


    //TODO: For now, let's just execute only forward
    public static final Map<Object,Object> execute(Map<Object,Object> context) {
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get(Context.TERMINATORS_KEY);

        Object queue = context.get(Context.QUEUE_KEY);
        if (queue == null) {
            return context;
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>) context.get(Context.STACK_KEY);
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
        context.put(Context.QUEUE_KEY, new ArrayDeque<IInterceptor>(queue));
        return execute(context);
    }

    public static final Map<Object,Object> kill(Map<Object,Object> context) {
        Deque<IInterceptor> queue = (Deque<IInterceptor>)context.get(Context.QUEUE_KEY);
        if (queue != null) {
            queue.clear();
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>)context.get(Context.STACK_KEY);
        if (stack != null) {
            stack.clear();
        }
        return context;
    }

    public static final Map<Object,Object> handleEnter(Map<Object,Object> context,
                                                       Deque<IInterceptor> queue,
                                                       Deque<IInterceptor> stack,
                                                       final List<Predicate<Map<Object,Object>>> terminators) {

        //NOTE: It's assumed the queue has been null-checked by this point
        while (!queue.isEmpty()) {

            IInterceptor interceptor = queue.pollFirst();
            if (interceptor == null) {
                context.remove(Context.QUEUE_KEY);
                return handleLeave(context, stack);
            }
            stack.offerFirst(interceptor); // Pushing to the front allows iteration without reversing

            try {
                context = IInterceptor.enter(interceptor, context);

                if (context.get(Context.ERROR_KEY) != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                return handleError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove(Context.QUEUE_KEY);
                        return handleLeave(context, stack);
                    }
                }
            }
        }
        // 'Leave' is only triggered by terminators
        // If you never hit a terminator, and there are no more interceptors,
        // you're done.
        return context;

    }
    public static final Map<Object,Object> handleLeave(Map<Object,Object> context,
                                                       Deque<IInterceptor> stack) {
        //NOTE: It's assumed the stack has been null-checked by this point

        /**
         * This for-each loop looks nice, but restarts the stack processing
         * when it switches back to "handleLeave".
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
        }*/
        while (!stack.isEmpty()) {
            IInterceptor interceptor = stack.pollFirst(); // Our 'stack' is already "reversed" so grab the head
            // Interceptors are null-checked when added to the stack
            // so we shouldn't have to do that here.
            // If an NPE happens, the user manipulated the stack directly,
            // and that's their fault
            try {
                context = IInterceptor.leave(interceptor, context);
                if (context.get(Context.ERROR_KEY) != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                return handleError(context, interceptor, stack);
            }
        }
        return context;
    }
    public static final Map<Object,Object> handleError(Map<Object,Object> context,
                                                       IInterceptor erroredInterceptor,
                                                       Deque<IInterceptor> stack) {
        //NOTE: It's assumed the stack has been null-checked by this point

        /**
         * This for-each loop looks nice, but restarts the stack processing
         * when it switches back to "handleLeave".
        for (IInterceptor interceptor : stack) {
            Object err = context.get("error");
            if (err != null) {
                context = IInterceptor.error(interceptor, context);
            } else {
                return handleLeave(context, stack);
            }
        }
        */
        while (!stack.isEmpty()) {
            Object err = context.get(Context.ERROR_KEY);
            // Interceptors are null-checked when added to the stack
            // so we shouldn't have to do that here.
            // If an NPE happens, the user manipulated the stack directly,
            // and that's their fault
            if (err != null) {
                IInterceptor interceptor = stack.pollFirst(); // Our 'stack' is already "reversed" so grab the head
                // No need for a 'try/catch' here, because it's captured in 'handleLeave'
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

        //NOTE: It's assumed the queue has been null-checked by this point
        for(IInterceptor interceptor : queue) {
            if (interceptor == null) {
                context.remove(Context.QUEUE_KEY);
                return handleLeave(context, stack);
            }
            stack.offerFirst(interceptor);

            try {
                context = IInterceptor.enter(interceptor, context);

                if (context.get(Context.ERROR_KEY) != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                return handleError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove(Context.QUEUE_KEY);
                        return handleLeave(context, stack);
                    }
                }
            }
        }
        return context;
    }

}

