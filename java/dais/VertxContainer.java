
package dais;

import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

import io.vertx.core.Vertx;
import io.vertx.core.Verticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import dais.Server;
import dais.Chain;
import dais.Context;
import dais.ToInterceptor;

public class VertxContainer extends AbstractVerticle implements Server {

    public static final String VERTX_KEY = "dais.vertx";
    public static final String VERTX_CFG_KEY = "dais.vertx";
    public static final String HTTPSERVER_KEY = "dais.vertx.httpServer";
    public static final String VERTX_REQUEST_KEY = "dais.vertx.request";

    public final Map<Object,Object> serviceMap;

    public VertxContainer() {
        super();
        this.serviceMap = new HashMap();
    }
    public VertxContainer(Map<Object,Object> srvMap) {
        super();
        this.serviceMap = new HashMap(srvMap);
    }

    @Override
    public void start() {
        startWith(this.serviceMap);
    }

    @Override
    public void start(Future<Void> fut) {
        startWith(this.serviceMap);
    }

    public Map<Object,Object> startWith(final Map<Object,Object> serviceMap) {

        // Set server options if they exist
        // and build the HTTP Server
        Object opts = serviceMap.get(Server.SERVEROPTS_KEY);
        HttpServerOptions serveOpts;
        HttpServer httpServer;
        if (opts != null) {
            serveOpts = (HttpServerOptions)opts;
        } else {
            serveOpts = new HttpServerOptions();
            serveOpts.setPort(8080);
            serveOpts.setHost("localhost");
        }
        httpServer = vertx.createHttpServer(serveOpts);

        // Setup predicates
        List<Predicate<Map<Object,Object>>> maybeTerms = (List<Predicate<Map<Object,Object>>>) serviceMap.get(Context.TERMINATORS_KEY);
        if (maybeTerms == null) {
            maybeTerms = new ArrayList<Predicate<Map<Object,Object>>>();
        }
        maybeTerms.add(ctx -> {
            HttpServerRequest req = (HttpServerRequest)ctx.get(VERTX_REQUEST_KEY);
            if (req == null) {
                return true;
            }
            return req.response().ended() || req.response().closed();
        });
        final List<Predicate<Map<Object,Object>>> terminators = maybeTerms;
        // Setup interceptors
        // If they're `null`, that's your fault
        Deque<ToInterceptor> interceptors = (Deque<ToInterceptor>) serviceMap.get(Context.QUEUE_KEY);

        // Hook up the Chain to Vertx
        httpServer.requestHandler(r -> {

            Map<Object,Object> ctx = new Context()
                .withTerminators(terminators)
                .withInterceptors(interceptors);

            ctx.put(VERTX_KEY, vertx);
            ctx.put(HTTPSERVER_KEY, httpServer);
            ctx.put(VERTX_REQUEST_KEY, r);

            Chain.execute(ctx);
            HttpServerResponse resp = r.response();
            if (resp.ended()) {
                // Already ended, do nothing
            } else {
                // We're assuming that the chain couldn't handle the response
                resp.setStatusCode(404);
                resp.end();
            }
        });

        // Start listening on the host and port set in the HttpServerOptions
        httpServer.listen();

        // Return our serviceMap with the activated service
        serviceMap.put(VERTX_KEY, vertx);
        return serviceMap;
    }

    public Map<Object,Object> stopWith(Map<Object,Object> serviceMap) {
        return serviceMap;
    }

    //public static Map<Object,Object> deploy(Map<Object,Object> serviceMap) {
    public static void deploy(Map<Object,Object> serviceMap) {
        int procs = Runtime.getRuntime().availableProcessors();
		Vertx vertx = Vertx.vertx();
        //VertxContainer v = new VertxContainer(serviceMap);
		vertx.deployVerticle(() -> new VertxContainer(serviceMap),
				new DeploymentOptions().setInstances(procs*2), event -> {
					if (event.succeeded()) {
						System.out.println("Your Vert.x application is started!");
					} else {
						System.out.println("Unable to start your application");
					}
				});
        //return v.serviceMap;
    }

}

