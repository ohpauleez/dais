
package dais;

import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.Collection;

import java.util.ArrayDeque;

import java.util.function.Predicate;

import java.util.concurrent.Future;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import dais.IInterceptor;
import dais.ChainPhase;
import dais.Context;

/**
 * Dais Engine
 *
 * Given a Context (Map) which contains a Context.QUEUE_KEY of IInterceptors, execute the queue sequentially, and return the final Context.
 *
 * A series of rules/decisions are applied to decide how to execute the series of interceptors (commonly called the "interceptor chain").
 *
 * If the queue is empty, processing is finished and the Context is returned.
 * Otherwise, get the first IInterceptor off of the front of the queue.
 *
 * If that Interceptor is null, remove the Context.QUEUE_KEY from the Context and process the Leave Phase of the chain.
 * Otherwise, place the Interceptor on the Context.STACK_KEY, so it can be processed in the Leave Phase later.
 * Execute the Enter Phase of the Interceptor, returning a new/updated Context.
 *
 * If there is an Context.ERROR_KEY in the Context, handle the Error Phase.
 * Errors are handled by calling the `error` method of the IInterceptors in the Context's stack.
 * If the error is handled, the rest of the stack is process as a Leave Phase, calling the `leave` method of the remaining IInterceptors.
 *
 * If there are no errors, the Terminator Predicates are checked.
 * If any terminator returns true, the Context.QUEUE_KEY is removed from the Context and the Leave Phase is executed.
 *
 * Otherwise, keep looping through the queue (see the top of this doc block), until it is emtpy.
 *
 * Note: All modifications to the queue and stack execution should be made directly against the references to those objects.
 *       The queue is not rebound after Interceptor execution (when an updated context is returned).
 *       There are no checks in place to see if a program is using undefined behavior -- it is undefined.
 *
 *       To short-circuit chain execution, an Interceptor can call `clear` on the queue.
 *       To stop processing completely, an Interceptor can call `Engine.kill` on the context (this will clear the queue and stack)
 */

public interface Engine {

    static CompletionStage<Map<Object,Object>> doEnter(Map<Object,Object> context,
                                                       Deque<IInterceptor> queue,
                                                       Deque<IInterceptor> stack,
                                                       final List<Predicate<Map<Object,Object>>> terminators) {
        Map<Object,Object> result = null;

        //NOTE: It's assumed the queue has been null-checked by this point
        while (!queue.isEmpty()) {

            IInterceptor interceptor = queue.pollFirst();
            if (interceptor == null) {
                context.remove(Context.QUEUE_KEY);
                result = Engine.doLeave(context, stack);
            }
            stack.offerFirst(interceptor); // Pushing to the front allows iteration without reversing

            try {
                context = IInterceptor.enter(interceptor, context);
                result = context;

                if (context.get(Context.ERROR_KEY) != null) {
                    result = Engine.doError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                result = Engine.doError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove(Context.QUEUE_KEY);
                        result = Engine.doLeave(context, stack);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(result);

    }
    public default CompletionStage<Map<Object,Object>> handleEnter(Map<Object,Object> context,
                                                                   Deque<IInterceptor> queue,
                                                                   Deque<IInterceptor> stack,
                                                                   final List<Predicate<Map<Object,Object>>> terminators) {
        return Engine.doEnter(context, queue, stack, terminators);
    }

    static Map<Object,Object> doLeave(Map<Object,Object> context,
                                      Deque<IInterceptor> stack) {
        //NOTE: It's assumed the stack has been null-checked by this point

        /**
         * This for-each loop looks nice, but restarts the stack processing
         * when it switches back to "handleLeave".
        for (IInterceptor interceptor : stack) {
            try {
                context = IInterceptor.leave(interceptor, context);
                if (context.get(Context.ERROR_KEY) != null) {
                    return handleError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
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
                    return Engine.doError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                return Engine.doError(context, interceptor, stack);
            }
        }
        return context;
    }
    public default Map<Object,Object> handleLeave(Map<Object,Object> context,
                                                  Deque<IInterceptor> stack) {
        return Engine.doLeave(context, stack);
    }

    static Map<Object,Object> doError(Map<Object,Object> context,
                                      IInterceptor erroredInterceptor,
                                      Deque<IInterceptor> stack) {
        //NOTE: It's assumed the stack has been null-checked by this point

        /**
         * This for-each loop looks nice, but restarts the stack processing
         * when it switches back to "handleLeave".
        for (IInterceptor interceptor : stack) {
            Object err = context.get(Context.ERROR_KEY);
            if (err != null) {
                context = IInterceptor.error(interceptor, context);
            } else {
                return this.handleLeave(context, stack);
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
                return Engine.doLeave(context, stack);
            }
        }
        return context;
    }
    public default Map<Object,Object> handleError(Map<Object,Object> context,
                                                  IInterceptor erroredInterceptor,
                                                  Deque<IInterceptor> stack) {
        return Engine.doError(context, erroredInterceptor, stack);
    }

    static Map<Object,Object> doStaticEnter(Map<Object,Object> context,
                                            IInterceptor[] queue,
                                            Deque<IInterceptor> stack,
                                            List<Predicate<Map<Object,Object>>> terminators) {

        //NOTE: It's assumed the queue has been null-checked by this point
        for(IInterceptor interceptor : queue) {
            if (interceptor == null) {
                context.remove(Context.QUEUE_KEY);
                return Engine.doLeave(context, stack);
            }
            stack.offerFirst(interceptor);

            try {
                context = IInterceptor.enter(interceptor, context);

                if (context.get(Context.ERROR_KEY) != null) {
                    return Engine.doError(context, interceptor, stack);
                }
            } catch (Throwable t) {
                context.put(Context.ERROR_KEY, t);
                return Engine.doError(context, interceptor, stack);
            }

            if (terminators != null) {
                for (Predicate p : terminators) {
                    if (p != null && p.test(context)) {
                        context.remove(Context.QUEUE_KEY);
                        return Engine.doLeave(context, stack);
                    }
                }
            }
        }
        return context;
    }
    public default Map<Object,Object> handleStaticEnter(Map<Object,Object> context,
                                                        IInterceptor[] queue,
                                                        Deque<IInterceptor> stack,
                                                        List<Predicate<Map<Object,Object>>> terminators) {
        return Engine.doStaticEnter(context, queue, stack, terminators);
    }

    static Map<Object,Object> checkedExecute(Map<Object,Object> context) throws InterruptedException, ExecutionException {
        Object result = null;
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get(Context.TERMINATORS_KEY);
        Engine engine = (Engine)context.get(Context.ENGINE_KEY);

        Object queue = context.get(Context.QUEUE_KEY);
        if (queue == null) {
            result = context;
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>) context.get(Context.STACK_KEY);
        stack = (stack != null) ? stack : new ArrayDeque<IInterceptor>();

        if (queue instanceof Deque) {
            result = (engine != null) ? engine.handleEnter(context, (Deque<IInterceptor>) queue, stack, terminators)
                                      : Engine.doEnter(context, (Deque<IInterceptor>) queue, stack, terminators);
        } else if (queue instanceof IInterceptor[]) {
            result = (engine != null) ? engine.handleStaticEnter(context, (IInterceptor[]) queue, stack, terminators)
                                      : Engine.doStaticEnter(context, (IInterceptor[]) queue, stack, terminators);
        } else {
            result = context;
        }

        if (result instanceof CompletionStage) {
            return ((CompletionStage<Map<Object,Object>>) result).toCompletableFuture().get();
        } else if (result instanceof Future) {
            return ((Future<Map<Object,Object>>)result).get();
        } else {
            return (Map<Object,Object>)result;
        }
    }
    static Map<Object,Object> execute(Map<Object,Object> context) {
        try {
            return Engine.checkedExecute(context);
        } catch (InterruptedException|ExecutionException ex) {
            return null;
        }
    }
    static CompletionStage<Map<Object,Object>> completableExecute(Map<Object,Object> context) {
        Object result = null;
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get(Context.TERMINATORS_KEY);
        Engine engine = (Engine)context.get(Context.ENGINE_KEY);

        Object queue = context.get(Context.QUEUE_KEY);
        if (queue == null) {
            result = context;
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>) context.get(Context.STACK_KEY);
        stack = (stack != null) ? stack : new ArrayDeque<IInterceptor>();

        if (queue instanceof Deque) {
            result = (engine != null) ? engine.handleEnter(context, (Deque<IInterceptor>) queue, stack, terminators)
                                      : Engine.doEnter(context, (Deque<IInterceptor>) queue, stack, terminators);
        } else if (queue instanceof IInterceptor[]) {
            result = (engine != null) ? engine.handleStaticEnter(context, (IInterceptor[]) queue, stack, terminators)
                                      : Engine.doStaticEnter(context, (IInterceptor[]) queue, stack, terminators);
        } else {
            result = context;
        }

        if (result instanceof CompletionStage) {
            return (CompletionStage<Map<Object,Object>>) result;
        } else {
            return CompletableFuture.completedFuture((Map<Object,Object>)result);
        }
    }

    static Map<Object,Object> checkedExecute(Map<Object,Object> context, Collection<IInterceptor> queue) throws InterruptedException, ExecutionException {
        context.put(Context.QUEUE_KEY, new ArrayDeque<IInterceptor>(queue));
        return execute(context);
    }
    static Map<Object,Object> execute(Map<Object,Object> context, Collection<IInterceptor> queue) {
        try {
            return Engine.checkedExecute(context, queue);
        } catch (Exception e) {
            return null;
        }
    }

    static Map<Object,Object> executeStage(Map<Object,Object> context, ChainPhase phase) throws InterruptedException, ExecutionException {
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get(Context.TERMINATORS_KEY);
        Engine engine = (Engine)context.get(Context.ENGINE_KEY);

        Object result;

        Object queue = context.get(Context.QUEUE_KEY);
        if (queue == null) {
            result = context;
        }
        Deque<IInterceptor> stack = (Deque<IInterceptor>) context.get(Context.STACK_KEY);
        stack = (stack != null) ? stack : new ArrayDeque<IInterceptor>();

        switch(phase) {
            case ENTER:
                if (queue instanceof Deque) {
                    result = (engine != null) ? engine.handleEnter(context, (Deque<IInterceptor>) queue, stack, terminators)
                                              : Engine.doEnter(context, (Deque<IInterceptor>) queue, stack, terminators);
                } else if (queue instanceof IInterceptor[]) {
                    result = (engine != null) ? engine.handleStaticEnter(context, (IInterceptor[]) queue, stack, terminators)
                                              : Engine.doStaticEnter(context, (IInterceptor[]) queue, stack, terminators);
                } else {
                    result = context;
                }
                break;
            case LEAVE:
                result = (engine != null) ? engine.handleLeave(context, stack)
                                          : Engine.doLeave(context, stack);
                break;
            case ERROR:
                if (context.get(Context.ERROR_KEY) != null) {
                    result = (engine != null) ? engine.handleError(context, null, stack)
                                              : Engine.doError(context, null, stack);
                } else {
                    result = (engine != null) ? engine.handleLeave(context, stack)
                                              : Engine.doLeave(context, stack);
                }
                break;
            default:
                result = context;
        }

        if (result instanceof CompletionStage) {
            return ((CompletionStage<Map<Object,Object>>)result).toCompletableFuture().get();
        } else if (result instanceof Future) {
            return ((Future<Map<Object,Object>>)result).get();
        } else {
            return (Map<Object,Object>)result;
        }
    }

    static Map<Object,Object> kill(Map<Object,Object> context) {
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
}

