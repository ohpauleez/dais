
package dais;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import dais.Server;
import dais.Chain;
import dais.Maps;

public class Vertx extends AbstractVerticle implements Server {

    public static final String VERTX_KEY = "dais.vertx";
    public static final String HTTPSERVER_KEY = "dais.vertx.httpServer";
    public static final String VERTX_REQUEST_KEY = "dais.vertx.request";


    @Override
    public void start(Future<Void> fut) {
    }

    public Map<Object,Object> startWith(final Map<Object,Object> context) {
        // Set server options if they exist
        // and build the HTTP Server
        Object opts = context.get(Server.SERVEROPTS_KEY);
        HttpServer httpServer;
        if (opts != null) {
            httpServer = vertx.createHttpServer((HttpServerOptions)opts);
        } else {
            httpServer = vertx.createHttpServer();
        }
        context.put(VERTX_KEY, vertx);
        context.put(HTTPSERVER_KEY, httpServer);

        // Setup predicates
        List<Predicate<Map<Object,Object>>> terminators = (List<Predicate<Map<Object,Object>>>) context.get(Chain.TERMINATORS_KEY);
        if (terminators == null) {
            terminators = new ArrayList<Predicate<Map<Object,Object>>>();
        }
        terminators.add(ctx -> {
            HttpServerRequest req = (HttpServerRequest)ctx.get(VERTX_REQUEST_KEY);
            if (req == null) {
                return true;
            }
            return req.response().ended() || req.response().closed();
        });
        context.put(Chain.TERMINATORS_KEY, terminators);

        // Hook up the Chain to Vertx
        httpServer.requestHandler(r -> {
            // !!!!!!!WRONG!!!!!!!!
            // This means for a single server, every request is getting/sharing the same context
            // but this is totally false, broken, and not thread-safe
            //
            // Worse, since the chain works destructively through the queue/stack instead of with pointers,
            // We'd need to make a copy of the interceptors per request, just to destroy them again.
            context.put(VERTX_REQUEST_KEY, r);
            Map<Object,Object> doneContext = Chain.execute(context);
            HttpServerResponse resp = r.response();
            if (resp.ended()) {
                // Already ended, do nothing
            } else {
                // We're assuming that the chain couldn't handle the response
                resp.setStatusCode(404);
                resp.end();
            }
        });

        // Start listening on the host and port
        // TODO: We need to pass in the equiv of "Service Map"
        httpServer.listen(8080, "localhost");

        // Return our context with the activated service
        return context;
    }

    public Map<Object,Object> stopWith(Map<Object,Object> context) {
        return context;
    }


}
