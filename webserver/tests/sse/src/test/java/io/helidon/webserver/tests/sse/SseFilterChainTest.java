/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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
package io.helidon.webserver.tests.sse;

import java.time.Duration;
import java.util.List;

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.sse.SseSink;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JUnit test reproducer for Helidon HTTP/2 SSE interleaved write bug.
 *
 * <h2>Problem</h2>
 * When SSE events are emitted in a <b>filter</b> before {@code chain.proceed()},
 * subsequent SSE events emitted by the <b>handler</b> are silently dropped.
 *
 * <h2>Test Cases</h2>
 *
 * <h3>Test 1: Filter emits, then Handler emits (BUG)</h3>
 * <p>Demonstrates the bug where handler events are lost.
 *
 * <h3>Test 2: Handler emits only (WORKS)</h3>
 * <p>Verifies that handler-only emissions work correctly.
 *
 * <h2>Expected vs Actual</h2>
 *
 * <table border="1">
 *   <tr>
 *     <th>Test Case</th>
 *     <th>Expected Events</th>
 *     <th>Actual Events (Bug)</th>
 *   </tr>
 *   <tr>
 *     <td>Filter then Handler</td>
 *     <td>2 (filter + handler)</td>
 *     <td>1 (filter only)</td>
 *   </tr>
 *   <tr>
 *     <td>Handler only</td>
 *     <td>1 (handler)</td>
 *     <td>1 (handler) ✓</td>
 *   </tr>
 * </table>
 *
 * <h2>Workaround</h2>
 * Do not emit SSE events in filters. Emit all events in handlers.
 *
 * @see <a href="https://github.com/helidon-io/helidon/pull/11506">Helidon PR #11506</a>
 */
@ServerTest
class SseFilterChainTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String HEADER_NAME = "X-Emit-In-Filter";

    private final WebServer webServer;

    SseFilterChainTest(WebServer webServer) {
        this.webServer = webServer;
    }

    /**
     * Sets up HTTP routing with filter and handler for bug demonstration.
     *
     * @param rules the HTTP routing rules
     */
    @SetUpRoute
    static void routing(HttpRules rules) {
        // Add filter that may emit SSE events before chain.proceed()
        rules.addFilter(SseFilterChainTest::bugDemonstratingFilter)
             .get("/filterThenHandler", SseFilterChainTest::handlerEmitEvent)
             .get("/handlerOnly", SseFilterChainTest::handlerEmitEvent);
    }

    /**
     * Test case 1: Filter emits SSE event, then handler emits SSE event.
     *
     * <p>This test demonstrates the bug. The filter emits an event before
     * calling {@code chain.proceed()}, then the handler emits another event.
     * Due to the bug, only the filter's event reaches the client.
     *
     * @throws Exception if test fails
     */
    @Test
    void testFilterEmitsThenHandlerEmits() throws Exception {
        // This test will FAIL due to the bug - handler event is lost
        testSse("/filterThenHandler",
                List.of("X-Emit-In-Filter: true"),
                "data:event-from-filter",
                "data:event-from-handler");
    }

    /**
     * Test case 2: Handler emits SSE event only.
     *
     * <p>This test verifies that handler-only emissions work correctly.
     * When the filter does not emit any events, the handler's event
     * reaches the client as expected.
     *
     * @throws Exception if test fails
     */
    @Test
    void testHandlerEmitsOnly() throws Exception {
        // This test passes - handler event reaches client
        testSse("/handlerOnly",
                List.of("X-Emit-In-Filter: false"),
                "data:event-from-handler");
    }

    /**
     * Filter that demonstrates the SSE bug.
     *
     * <p>When the request header {@code X-Emit-In-Filter} is "true",
     * this filter:
     * <ol>
     *   <li>Creates an SSE sink from the response</li>
     *   <li>Emits an event</li>
     *   <li>Closes the sink</li>
     *   <li>Calls {@code chain.proceed()}</li>
     * </ol>
     *
     * <p>This sequence causes subsequent handler emissions to be lost.
     *
     * @param chain the filter chain
     * @param request the server request
     * @param response the server response
     */
    private static void bugDemonstratingFilter(
            FilterChain chain,
            ServerRequest request,
            ServerResponse response) {

        boolean emitInFilter = request.headers()
                .value("X-Emit-In-Filter")
                .map("true"::equals)
                .orElse(false);

        if (emitInFilter) {
            // BUG CASE: Filter emits SSE event before chain.proceed()
            try (SseSink sink = response.sink(SseSink.TYPE)) {
                sink.emit(SseEvent.builder()
                        .name("message")
                        .data("event-from-filter")
                        .build());
            }
        }

        // Proceed to handler
        chain.proceed();
    }

    /**
     * Handler that emits an SSE event.
     *
     * <p>This handler always attempts to emit an SSE event. In the
     * "filter then handler" test case, this event will be lost due to the bug.
     *
     * @param request the server request
     * @param response the server response
     */
    private static void handlerEmitEvent(ServerRequest request, ServerResponse response) {
        try (SseSink sink = response.sink(SseSink.TYPE)) {
            sink.emit(SseEvent.builder()
                    .name("message")
                    .data("event-from-handler")
                    .build());
        }
    }

    /**
     * Helper method to test SSE endpoint and verify received events.
     *
     * @param path the request path
     * @param headers additional headers to send
     * @param expectedEvents the expected SSE events
     * @throws Exception if test fails
     */
    private void testSse(String path, Iterable<String> headers, String... expectedEvents) throws Exception {
        try (SimpleSseClient sseClient = SimpleSseClient.create("localhost",
                                                                webServer.port(),
                                                                path,
                                                                headers,
                                                                TIMEOUT)) {
            for (String expectedEvent : expectedEvents) {
                String actualEvent = sseClient.nextEvent();
                assertThat("Expected event: " + expectedEvent + ", but got: " + actualEvent,
                          actualEvent, is(expectedEvent));
            }
            // Ensure no more events
            assertThat(sseClient.nextEvent(), is(nullValue()));
        }
    }
}
