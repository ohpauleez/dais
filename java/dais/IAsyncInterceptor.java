
package dais;

import java.util.function.Function;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;


//TODO: Consider IOptionalInterceptor that returns Optionals

public interface IAsyncInterceptor {
    public Function<Map<Object,Object>,CompletionStage<Map<Object,Object>>> getAsyncEnter();
    public boolean isAsyncInterceptor();

    static CompletionStage<Map<Object,Object>> enterAsync(IAsyncInterceptor interceptor, Map<Object,Object> context) {
        Function<Map<Object,Object>,CompletionStage<Map<Object,Object>>> eFn = ((interceptor != null) && interceptor.isAsyncInterceptor()) ? interceptor.getAsyncEnter() : null;
        if (eFn != null) {
            return eFn.apply(context);
        } else {
            return CompletableFuture.completedFuture(context);
        }
    }
}

