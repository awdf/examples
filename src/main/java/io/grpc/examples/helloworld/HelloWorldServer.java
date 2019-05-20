package io.grpc.examples.helloworld;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());
    private static final String REGEXP = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final Pattern PATTERN = Pattern.compile(REGEXP);


    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
            .addService(new GreeterImpl())
            .build()
            .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
          @Override
          public void run() {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            HelloWorldServer.this.stop();
            System.err.println("*** server shut down");
          }
        });
    }

    private void stop() {
        if (server != null) {
          server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
          server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            String target = req.getName();
            int status = 0;
            long time = 0L;

            logger.info("Getting " + target);

            if (PATTERN.matcher(target).matches()) try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(target))
                        .build();

                long startTime = System.currentTimeMillis();
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                status = response.statusCode();
                time = System.currentTimeMillis() - startTime;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            HelloReply reply = HelloReply.newBuilder().setMessage(String.format("Status Code: %d, Upload time: %d", status, time)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
