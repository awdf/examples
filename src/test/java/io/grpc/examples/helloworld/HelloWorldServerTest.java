/*
 * Copyright 2016 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.examples.helloworld.HelloWorldServer.GreeterImpl;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HelloWorldServerTest {
  private static final String RESOURCE = "https://www.google.";
  private static final Pattern PATTERN = Pattern.compile("(\\S+|\\s)+");
  private static final List<String> countries = Arrays.asList(
          "org", "com", "ua", "ru"
//          , "it", "cn", "au", "il", "uk", "it",
//          "hk", "sp", "us", "ma", "tw", "jp", "zw", "va", "sa", "qa"
  );
  private static GreeterGrpc.GreeterBlockingStub blockingStub = null;

  @RegisterExtension
  public static final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @BeforeAll
  public static void buildInstance() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    grpcCleanup.register(InProcessServerBuilder
            .forName(serverName).directExecutor().addService(new GreeterImpl()).build().start());

    blockingStub = GreeterGrpc.newBlockingStub(
            grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  @Test
  public void greeterImpl_replyMessage() throws Exception {
    assertEquals("Status Code: 0, Upload time: 0", pingSite("snap"));
  }

  @Test
  public void singleThreadBlockingCall(){
    ExecutorService executor = Executors.newSingleThreadExecutor();
    List<CompletableFuture<String>> futures = countries.stream()
            .map(f -> CompletableFuture.completedFuture(f)
                    .thenApplyAsync(domain -> pingSite(RESOURCE + domain), executor))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenRun(() -> futures.forEach(
                    cf -> assertTrue(PATTERN.matcher(cf.getNow(null)).matches())
            ))
            .join();
    executor.shutdown();
  }

  @Test
  public void singleThreadNonBlockingCall() throws InterruptedException{
    ExecutorService executor = Executors.newSingleThreadExecutor();
    List<CompletableFuture<String>> futures = countries.stream()
            .map(f -> CompletableFuture.completedFuture(f)
                    .thenApplyAsync(domain -> pingSite(RESOURCE + domain), executor))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenRun(() -> futures.forEach(
                    cf -> assertTrue(PATTERN.matcher(cf.getNow(null)).matches())
            ));
    executor.shutdown();

    Thread.sleep(5000); //imitate main thread working process.
  }

  @Test
  public void multiThreadBlockingCall(){
    ExecutorService executor = Executors.newCachedThreadPool();
    List<CompletableFuture<String>> futures = countries.stream()
            .map(f -> CompletableFuture.completedFuture(f)
                    .thenApplyAsync(domain -> pingSite(RESOURCE + domain), executor))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenRun(() -> futures.forEach(
                    cf -> assertTrue(PATTERN.matcher(cf.getNow(null)).matches())
            ))
            .join();
    executor.shutdown();
  }

  @Test
  public void multiThreadNonBlockingCall() throws InterruptedException{
    ExecutorService executor = Executors.newCachedThreadPool();
    List<CompletableFuture<String>> futures = countries.stream()
            .map(f -> CompletableFuture.completedFuture(f)
                    .thenApplyAsync(domain -> pingSite(RESOURCE + domain), executor))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenRun(() -> futures.forEach(
                    cf -> assertTrue(PATTERN.matcher(cf.getNow(null)).matches())
            ));
    executor.shutdown();

    Thread.sleep(5000); //imitate main thread working process.
  }

  private String pingSite(String site) {
    return blockingStub.sayHello(HelloRequest.newBuilder().setName(site).build()).getMessage();
  }
}
