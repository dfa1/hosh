/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.Collections;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.hosh.testsupport.SneakySignal;
import org.hosh.testsupport.WithThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SupervisorTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@Mock
	private Channel err;

	@Mock(stubOnly = true)
	private Statement statement;

	@Mock(stubOnly = true)
	private Command command;

	@InjectMocks
	private Supervisor sut;

	@AfterEach
	public void cleanup() {
		sut.close();
	}

	@Test
	public void noSubmit() {
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(0);
	}

	@Test
	public void handleSignals() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> {
			SneakySignal.raise("INT");
			return ExitStatus.success();
		});
		ExitStatus waitForAll = sut.waitForAll(err);
		assertThat(waitForAll.isError()).isTrue();
	}

	@Test
	public void handleInterruptions() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> {
			Thread.sleep(10_000);
			return ExitStatus.success();
		});
		Thread.currentThread().interrupt(); // next call to Future.get() will throw InterruptedException
		ExitStatus waitForAll = sut.waitForAll(err);
		assertThat(waitForAll.isError()).isTrue();
	}

	@Test
	public void allSubmitInSuccess() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> ExitStatus.success());
		sut.submit(statement, () -> ExitStatus.success());
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(0);
	}

	@Test
	public void oneSubmitInError() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> ExitStatus.success());
		sut.submit(statement, () -> ExitStatus.of(10));
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(10);
	}

	@Test
	public void oneSubmitWithException() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> {
			throw new NullPointerException("simulated error");
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(1);
		then(err).should().send(Record.of(Keys.ERROR, Values.ofText("simulated error")));
	}

	@Test
	public void oneSubmitWithExceptionButNoMessage() {
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Collections.emptyList());
		sut.submit(statement, () -> {
			throw new NullPointerException();
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(1);
		then(err).should().send(Record.of(Keys.ERROR, Values.ofText("(no message provided)")));
	}

	@Test
	public void setThreadNameWithArgs() {
		given(command.describe()).willReturn("TestCommand");
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Arrays.asList("-a", "-b"));
		sut.submit(statement, () -> {
			assertThat(Thread.currentThread().getName()).isEqualTo("command='TestCommand -a -b'");
			return ExitStatus.success();
		});
		sut.waitForAll(err);
		then(err).shouldHaveZeroInteractions(); // checking no assertion failures happened
	}

	@Test
	public void setThreadNameWithoutArgs() {
		given(command.describe()).willReturn("TestCommand");
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(Arrays.asList());
		sut.submit(statement, () -> {
			assertThat(Thread.currentThread().getName()).isEqualTo("command='TestCommand'");
			return ExitStatus.success();
		});
		sut.waitForAll(err);
		then(err).shouldHaveNoMoreInteractions(); // checking no assertion failures happened
	}
}
