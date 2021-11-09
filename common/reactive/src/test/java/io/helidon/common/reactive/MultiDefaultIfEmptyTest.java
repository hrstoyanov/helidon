/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.common.reactive;

import java.io.IOException;
import java.util.concurrent.SubmissionPublisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MultiDefaultIfEmptyTest {

    @Test
    public void normal() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.just(1)
                .defaultIfEmpty(2)
                .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void normalWithSupplier() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.just(1)
                .defaultIfEmpty(() -> 2)
                .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void fallback() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Integer>empty()
                .defaultIfEmpty(2)
                .subscribe(ts);

        ts.assertResult(2);
    }

    @Test
    public void fallbackWithSupplier() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Integer>empty()
                .defaultIfEmpty(() -> 2)
                .subscribe(ts);

        ts.assertResult(2);
    }

    @Test
    public void fallbackBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Multi.<Integer>empty()
                .defaultIfEmpty(2)
                .subscribe(ts);

        ts
                .assertEmpty()
                .request(1)
                .assertResult(2);
    }

    @Test
    public void fallbackBackpressuredWithSupplier() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Multi.<Integer>empty()
                .defaultIfEmpty(() -> 2)
                .subscribe(ts);

        ts
                .assertEmpty()
                .request(1)
                .assertResult(2);
    }

    @Test
    public void error() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Integer>error(new IOException())
                .defaultIfEmpty(2)
                .subscribe(ts);

        ts.assertFailure(IOException.class);
    }

    @Test
    public void errorWithSupplier() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Integer>error(new IOException())
                .defaultIfEmpty(() -> 2)
                .subscribe(ts);

        ts.assertFailure(IOException.class);
    }

    @Test
    public void cancel() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(Runnable::run, 128);
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.create(sp)
                .defaultIfEmpty(2)
                .subscribe(ts);

        assertTrue(sp.hasSubscribers());

        ts.assertEmpty();

        ts.cancel();

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void cancelWithSupplier() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>(Runnable::run, 128);
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.create(sp)
                .defaultIfEmpty(() -> 2)
                .subscribe(ts);

        assertTrue(sp.hasSubscribers());

        ts.assertEmpty();

        ts.cancel();

        assertFalse(sp.hasSubscribers());
    }

}
