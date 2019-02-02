
'use strict'

const QUEUE_KEY = "dais.queue";
const STACK_KEY = "dais.stack";
const TERMINATORS_KEY = "dais.terminators";
const ERROR_KEY = "dais.error";
const ENGINE_KEY = "dais.engine";

const ChainPhase = Object.freeze({
    ENTER: Symbol("enter"),
    LEAVE: Symbol("leave"),
    ERROR: Symbol("error")
});

class Context {

    static get QUEUE_KEY() {
        return QUEUE_KEY;
    }
    static get STACK_KEY() {
        return STACK_KEY;
    }
    static get TERMINATORS_KEY() {
        return TERMINATORS_KEY;
    }
    static get ERROR_KEY() {
        return ERROR_KEY;
    }
    static get ENGINE_KEY() {
        return ENGINE_KEY;
    }

    withTerminators(terminators) {
        this[TERMINATORS_KEY] = terminators;
        return this;
    }

    withInterceptors(interceptors) {
        this[QUEUE_KEY] = interceptors;
        return this;
    }
}

class Interceptor {

    constructor(enter, leave, error) {
        if (typeof enter == 'function') {
            this.enterFn = enter;
        } else if (typeof enter == 'object') {
            this.enterFn = enter.enter;
        } else {
            this.enterFn = enter;
        }

        if (typeof leave == 'function') {
            // `leave` was passed in as a function, let's just use it
            this.leaveFn = leave;
        } else if (typeof leave == 'object') {
            // `leave` was passed in as an Object, let's see if it has a `leave` prop
            this.leaveFn = leave.leave;
        } else if ((!leave) && (typeof enter == 'object')) {
            // `leave` wasn't passed in, but `enter` is an object that might have a `leave` prop
            this.leaveFn = enter.leave;
        } else {
            this.leaveFn = leave;
        }

        if (typeof error == 'function') {
            // `error` was passed in as a function, let's just use it
            this.errorFn = error;
        } else if (typeof error == 'object') {
            // `error` was passed in as an Object, let's see if it has a `error` prop
            this.errorFn = error.error;
        } else if ((!error) && (typeof enter == 'object')) {
            // `error` wasn't passed in, but `enter` is an object that might have a `error` prop
            this.errorFn = enter.error;
        } else {
            this.errorFn = error;
        }

        this.stages = Object.freeze({
            'enter': this.enterFn,
            'leave': this.leaveFn,
            'error': this.errorFn
        });
    }

    getEnter() {
        return this.enterFn;
    }
    get enter() {
        return this.enterFn;
    }
    getLeave() {
        return this.leaveFn;
    }
    get leave() {
        return this.leaveFn;
    }
    getError() {
        return this.errorFn;
    }
    get error() {
        return this.errorFn;
    }

    getStage(stage) {
        ret = null;
        switch (stage) {
            case 'enter':
                ret = this.enterFn;
                break;
            case ChainPhase.ENTER:
                ret = this.enterFn;
                break;
            case 'leave':
                ret = this.leaveFn;
                break;
            case ChainPhase.LEAVE:
                ret = this.leaveFn;
                break;
            case 'error':
                ret = this.errorFn;
                break;
            case ChainPhase.ERROR:
                ret = this.errorFn;
                break;
            default:
                ret = null;
        }
        return ret;
    }

    toInterceptor() {
        return this;
    }
}



// The Dais interceptor chain in pure, naive JavaScript (using mutable data types)

class Engine {

    static doLeave(context) {
        let stack = context[STACK_KEY];
        let stackLen = stack && stack.length;
        if (!stack) {
            return context;
        }
        for(let i = 0; i < stackLen; i++) {
            let interceptor = stack.pop();
            try {
                if (interceptor.leave) {
                    context = interceptor.leave(context);
                }
                if (context[ERROR_KEY] !== undefined) {
                    return Engine.doError(context);
                }
            } catch (e) {
                context[ERROR_KEY] = e;
                return Engine.doError(context);
            }
        }
        return context;
    }

    static doError(context) {
        let stack = context[STACK_KEY];
        let stackLen = stack && stack.length;
        if (!stack) {
            return context;
        }
        for(let i = 0; i < stackLen; i++) {
            let error = context[ERROR_KEY];
            if (error === undefined) {
                return Engine.doLeave(context)
            }

            let interceptor = stack.pop();
            if (interceptor.error) {
                context = interceptor.error(context);
            }
        }
        return context;
    }

    static doEnter(context) {
        let queue = context[QUEUE_KEY];
        let stack = context[STACK_KEY] || [];
        let terminators = context[TERMINATORS_KEY];
        if (!queue) {
            return context;
        }

        while (queue.length > 0) {
            let interceptor = queue[0];
            if (!interceptor) {
                delete context[QUEUE_KEY];
                context[STACK_KEY] = stack;
                return Engine.doLeave(context);
            }

            queue.shift();
            stack.push(interceptor);

            try {
                if (interceptor.enter) {
                    context = interceptor.enter(context);
                }
                if (context[ERROR_KEY] !== undefined) {
                    context[STACK_KEY] = stack;
                    return Engine.doError(context);
                }
            } catch (e) {
                context[ERROR_KEY] = e;
                context[STACK_KEY] = stack;
                return Engine.doError(context);
            }
            if (terminators !== undefined) {
                if (terminators.some((elem, i, array) => elem.call(this, context))) {
                    delete context[QUEUE_KEY];
                    context[STACK_KEY] = stack;
                    return Engine.doLeave(context);
                }
            }
            // Make sure we pick up any destructive updates to the queue within the context
            queue = context[QUEUE_KEY];
        }
        return context;
    }

    static doStaticEnter(context) {
        let queue = Object.freeze(context[QUEUE_KEY]);
        let queueLen = queue && queue.length;
        let stack = context[STACK_KEY] || [];
        let terminators = context[TERMINATORS_KEY];
        if (!queue) {
            return context;
        }

        for(let i = 0; i < queueLen; i++) {
            let interceptor = queue[i];
            if (!interceptor) {
                delete context[QUEUE_KEY];
                context[STACK_KEY] = stack;
                return Engine.doLeave(context);
            }

            stack.push(interceptor);

            try {
                if (interceptor.enter) {
                    context = interceptor.enter(context);
                }
                if (context[ERROR_KEY] !== undefined) {
                    context[STACK_KEY] = stack;
                    return Engine.doError(context);
                }
            } catch (e) {
                context[ERROR_KEY] = e;
                context[STACK_KEY] = stack;
                return Engine.doError(context);
            }
            if (terminators !== undefined) {
                if (terminators.some((elem, i, array) => elem.call(this, context))) {
                    delete context[QUEUE_KEY];
                    context[STACK_KEY] = stack;
                    return Engine.doLeave(context);
                }
            }
        }
        return context;
    }

    static execute(context, interceptors) {
        if (interceptors) {
            context[QUEUE_KEY] = interceptors;
        }
        context[STACK_KEY] = context[STACK_KEY] || [];
        return Engine.doEnter(context);
    }

    static completeableExecute(context, interceptors) {
        return new Promise(Engine.execute(context, interceptors));
    }

    static kill(context) {
        queue = context[QUEUE_KEY];
        while (queue && queue.length) { queue.pop(); }
        stack = context[STACK_KEY];
        while (stack && stack.length) { stack.pop(); }
        return context;
    }

}

