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

import hosh.doc.Bug;
import hosh.runtime.Compiler.CompileError;
import hosh.runtime.Compiler.Program;
import hosh.runtime.Compiler.Statement;
import hosh.spi.Command;
import hosh.spi.CommandWrapper;
import hosh.spi.State;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class CompilerTest {

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private Command command;

	@Mock(stubOnly = true)
	private Command anotherCommand;

	@Mock(stubOnly = true)
	private CommandWrapper<?> commandWrapper;

	@Mock(stubOnly = true)
	private CommandResolver commandResolver;

	@InjectMocks
	private Compiler sut;

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "rejected by the compiler")
	@Test
	public void incompletePipeline() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls | take 2 | "))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1:12: incomplete pipeline near '|'");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/42", description = "rejected by the compiler")
	@Test
	public void extraBraces() {
		assertThatThrownBy(() -> sut.compile("withTime { ls } } "))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: unnecessary closing '}'");
	}

	@Test
	public void pipelineOfCommandsWithoutArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("count");
		Program program = sut.compile("ls | count");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
		});
	}

	@Test
	public void pipelineOfCommandsWithArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("grep");
		Program program = sut.compile("ls /home | grep /regex/");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements)
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
			assertThat(statement.getArguments()).isEmpty();
		});
	}

	@Test
	public void commandWithConstant() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");
		Program program = sut.compile("cd /tmp");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments())
				.hasSize(1)
				.first().satisfies(arg -> {
				assertThat(arg).isInstanceOf(Compiler.Constant.class);
			});
		});
	}

	@Test
	public void commandWithVariable() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");
		Program program = sut.compile("cd ${DIR}");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments())
				.hasSize(1)
				.first().satisfies(arg -> {
				assertThat(arg).isInstanceOf(Compiler.Variable.class);
			});
		});
	}

	@Test
	public void commandWithVariableOrFallback() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");
		Program program = sut.compile("cd ${DIR!/tmp}");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments())
				.hasSize(1)
				.first().satisfies(arg -> {
				assertThat(arg).isInstanceOf(Compiler.VariableOrFallback.class);
			});

		});
	}

	@Test
	public void commandWithoutArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		Program program = sut.compile("env");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments()).isEmpty();
		});
	}

	@Test
	public void commandWithArgument() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		Program program = sut.compile("env --system");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments()).hasSize(1);
		});
	}

	@Test
	public void commandWithArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
		Program program = sut.compile("git commit --amend");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments()).hasSize(2);
		});
	}

	@Test
	public void commandNotRegisteredInAPipeline() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		assertThatThrownBy(() -> sut.compile("env | env2"))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: 'env2' unknown command");
	}

	@Test
	public void wrappedCommand() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime -t -a { git push }");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getLocation()).isEqualTo("withTime");
			assertThat(statement.getDetails()).isEqualTo("wrapper: withTime-t-a{gitpush}");
			assertThat(statement.getArguments()).hasSize(2);
		});
	}

	@Test
	public void nestedWrappedCommands() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime { withTime { git push } }");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isInstanceOf(DefaultCommandWrapper.class);
		});
	}

	@Test
	public void commandWrapperUsedAsCommand() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		assertThatThrownBy(() -> sut.compile("withTime"))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: 'withTime' is a command wrapper");
	}

	@Test
	public void emptyCommandWrapper() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		assertThatThrownBy(() -> sut.compile("withTime { }"))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: 'withTime' with empty wrapping statement");
	}

	@Test
	public void commandUsedAsCommandWrapper() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern } "))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: 'ls' is not a command wrapper");
	}

	@Test
	public void unknownCommand() {
		doReturn(Optional.empty()).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern }"))
			.isInstanceOf(CompileError.class)
			.hasMessage("line 1: 'ls' unknown command wrapper");
	}

	@Test
	public void commandWrapperAsProducer() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("benchmark");
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");
		Program program = sut.compile("benchmark 50 { ls } | schema");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getLocation()).isEqualTo("");
			assertThat(statement.getDetails()).isEqualTo("pipeline: benchmark50{ls}|schema");
			assertThat(statement.getArguments()).isEmpty();
			assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
			PipelineCommand pipeline = (PipelineCommand) statement.getCommand();
			assertThat(pipeline.getProducer().getCommand()).isInstanceOf(DefaultCommandWrapper.class);
			assertThat(pipeline.getConsumer().getCommand()).isSameAs(anotherCommand);
		});
	}

	@Disabled
	@Bug(description = "command cannot be dynamic", issue = "https://github.com/dfa1/hosh/issues/63")
	@Test
	public void commandAsVariableExpansion() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("echo");
		Program program = sut.compile("${JAVA_HOME}/bin/java");
		assertThat(program.getStatements()).hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getArguments())
				.hasSize(0);
		});
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/112")
	@Test
	public void commentedOutCommandIsNotCompiled() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cmd");
		Program program = sut.compile("cmd # ls /tmp");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getLocation()).isEqualTo("cmd");
			assertThat(statement.getDetails()).isEqualTo("simple: cmd");
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments()).isEmpty();
		});
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/112")
	@Test
	public void commandAfterCommentBlock() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cmd");
		Program program = sut.compile("#\ncmd");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getCommand()).isSameAs(command);
			assertThat(statement.getArguments()).isEmpty();
		});
	}

	@Test
	public void sequenceOfSimpleCommands() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");
		Program program = sut.compile("ls /tmp; schema");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getLocation()).isEmpty();
			assertThat(statement.getDetails()).isEqualTo("sequence: ls/tmp;schema");
			assertThat(statement.getArguments()).isEmpty();
			assertThat(statement.getCommand()).isInstanceOf(SequenceCommand.class);
			SequenceCommand sequenceCommand = (SequenceCommand) statement.getCommand();
			assertThat(sequenceCommand.getFirst().getArguments()).hasSize(1);
			assertThat(sequenceCommand.getSecond().getArguments()).hasSize(0);
		});
	}

	@Test
	public void sequenceOfPipelines() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("schema");
		Program program = sut.compile("ls /tmp | schema; ls /tmp | schema");
		assertThat(program.getStatements())
			.hasSize(1)
			.first().satisfies(statement -> {
			assertThat(statement.getLocation()).isEmpty();
			assertThat(statement.getDetails()).isEqualTo("sequence: ls/tmp|schema;ls/tmp|schema");
			assertThat(statement.getArguments()).isEmpty();
			assertThat(statement.getCommand()).isInstanceOf(SequenceCommand.class);
		});
	}

	@Test
	public void lambda() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("echo");
		Program program = sut.compile("ls | { path -> echo ${path} }");
		assertThat(program.getStatements())
			.hasSize(1)
			.first()
			.satisfies(statement -> {
				assertThat(statement.getLocation()).isEmpty();
				assertThat(statement.getDetails()).isEqualTo("pipeline: ls|{path->echo${path}}");
				assertThat(statement.getArguments()).isEmpty();
				assertThat(statement.getCommand())
					.asInstanceOf(InstanceOfAssertFactories.type(PipelineCommand.class))
					.satisfies(pipelineCommand -> {
						assertThat(pipelineCommand.getConsumer().getCommand())
							.asInstanceOf(InstanceOfAssertFactories.type(LambdaCommand.class))
							.satisfies(lambdaCommand -> {
								assertThat(lambdaCommand.getKey()).isEqualTo("path");
							});
					});
			});
	}

	/**
	 * Not sure if the following tests belong to compiler, they are more integration tests (e.g. compile + resolve).
	 */
	@Nested
	class Strings {

		@Test
		public void emptySingleQuotedString() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("echo");
			Program program = sut.compile("echo ''");
			assertThat(program.getStatements())
				.hasSize(1).first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolve = argument.resolve(state);
					assertThat(resolve).isEqualTo("");
				});
			});
		}

		@Test
		public void singleQuotedString() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");
			Program program = sut.compile("vim 'file with spaces'");
			assertThat(program.getStatements())
				.hasSize(1).first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolve = argument.resolve(state);
					assertThat(resolve).isEqualTo("file with spaces");
				});
			});
		}

		@Test
		public void singleQuotedStringWithVariables() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
			Program program = sut.compile("git '${HOME}${BIN}'");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolved = argument.resolve(state);
					assertThat(resolved).isEqualTo("${HOME}${BIN}");
				});
			});
		}

		@Test
		public void emptyDoubleQuotedString() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");
			Program program = sut.compile("vim \"\"");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolve = argument.resolve(state);
					assertThat(resolve).isEqualTo("");
				});
			});
		}

		@Test
		public void doubleQuotedString() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");
			Program program = sut.compile("vim \"file with spaces\"");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolve = argument.resolve(state);
					assertThat(resolve).isEqualTo("file with spaces");
				});
			});
		}

		@Test
		public void doubleQuotedStringWithVariable() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of("HOME", "/home/dfa")).when(state).getVariables();
			Program program = sut.compile("ls \"${HOME}\"");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolved = argument.resolve(state);
					assertThat(resolved).isEqualTo("/home/dfa");
				});
			});
		}

		@Test
		public void doubleQuotedStringWithVariables() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of("HOME", "/home/dfa", "BIN", "bin")).when(state).getVariables();
			Program program = sut.compile("ls \"${HOME}/${BIN}\"");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolved = argument.resolve(state);
					assertThat(resolved).isEqualTo("/home/dfa/bin");
				});
			});
		}

		@Test
		public void doubleQuotedStringWithFallback() {
			doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
			doReturn(Map.of("HOME", "/home/dfa")).when(state).getVariables();
			Program program = sut.compile("ls \"${HOME!/home}/${BIN!bin}\"");
			assertThat(program.getStatements())
				.hasSize(1)
				.first().satisfies(statement -> {
				assertThat(statement.getCommand()).isSameAs(command);
				assertThat(statement.getArguments()).hasSize(1).first().satisfies(argument -> {
					String resolved = argument.resolve(state);
					assertThat(resolved).isEqualTo("/home/dfa/bin");
				});
			});
		}
	}

}