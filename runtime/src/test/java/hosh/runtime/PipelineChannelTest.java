/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.runtime;

import hosh.spi.Record;
import hosh.test.support.WithExecutor;
import hosh.test.support.WithThread;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PipelineChannelTest {

	@RegisterExtension
	final WithThread withThread = new WithThread();

	@RegisterExtension
	public final WithExecutor withExecutor = new WithExecutor(Executors.newVirtualThreadExecutor());

	@Mock(stubOnly = true)
	Record record;

	@Test
	void stopConsumer() throws ExecutionException, InterruptedException {
		PipelineChannel sut = new PipelineChannel();
		Future<?> recv = withExecutor.submit(() -> {
			Optional<Record> recv1 = sut.recv();
			assertThat(recv1).contains(record);
			Optional<Record> recv2 = sut.recv();
			assertThat(recv2).isEmpty();
		});
		Future<?> send = withExecutor.submit(() -> {
			sut.send(record);
			sut.stopConsumer();
		});
		send.get();
		recv.get();
	}

	@Test
	void sendRecv() throws ExecutionException, InterruptedException {
		PipelineChannel sut = new PipelineChannel();
		Future<?> recv = withExecutor.submit(() -> {
			Optional<Record> recv1 = sut.recv();
			assertThat(recv1).contains(record);
		});
		Future<?> send = withExecutor.submit(() -> sut.send(record));
		recv.get();
		send.get();
	}

	@Test
	void sendInterrupted() {
		PipelineChannel sut = new PipelineChannel();
		withThread.interrupt();
		sut.send(record);
		assertThat(withThread.isInterrupted()).isTrue();
	}

	@Test
	void recvInterrupted() {
		PipelineChannel sut = new PipelineChannel();
		withThread.interrupt();
		Optional<Record> recv = sut.recv();
		assertThat(recv).isEmpty();
	}

}
