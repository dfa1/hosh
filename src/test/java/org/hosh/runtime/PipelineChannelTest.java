package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hosh.spi.Record;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PipelineChannelTest {
	@Mock
	private Record one;

	@Test
	public void stopConsumer() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(one);
		sut.stopConsumer();
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
		Optional<Record> recv2 = sut.recv();
		assertThat(recv2).isEmpty();
	}

	@Test
	public void sendRecv() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(one);
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
	}
}