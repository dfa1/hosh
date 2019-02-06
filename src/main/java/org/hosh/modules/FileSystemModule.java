/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.modules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Value;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("cd", new ChangeDirectory());
		commandRegistry.registerCommand("ls", new ListFiles());
		commandRegistry.registerCommand("cwd", new CurrentWorkingDirectory());
		commandRegistry.registerCommand("lines", new Lines());
		commandRegistry.registerCommand("find", new Find());
	}

	public static class ListFiles implements Command, StateAware {
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() > 1) {
				err.send(Record.of("message", Values.ofText("expected at most 1 argument")));
				return ExitStatus.error();
			}
			final Path cwd = state.getCwd();
			final Path dir;
			if (args.size() == 1) {
				Path arg = Paths.get(args.get(0));
				if (arg.isAbsolute()) {
					dir = arg;
				} else {
					dir = cwd.resolve(arg).toAbsolutePath();
				}
			} else {
				dir = cwd;
			}
			logger.debug("listing {}", dir);
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
				for (Path path : stream) {
					Value size = Files.isRegularFile(path) ? Values.ofHumanizedSize(Files.size(path)) : Values.none();
					Record entry = Record.of("path", Values.ofLocalPath(path.getFileName()), "size", size);
					out.send(entry);
				}
				return ExitStatus.success();
			} catch (NotDirectoryException e) {
				err.send(Record.of("error", Values.ofText("not a directory: " + e.getMessage())));
				return ExitStatus.error();
			} catch (IOException e) {
				err.send(Record.of("error", Values.ofText("error: " + e.getMessage())));
				return ExitStatus.error();
			}
		}
	}

	public static class CurrentWorkingDirectory implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			out.send(Record.of("cwd", Values.ofLocalPath(state.getCwd())));
			return ExitStatus.success();
		}
	}

	public static class ChangeDirectory implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			switch (args.size()) {
				case 0:
					err.send(Record.of("error", Values.ofText("missing path argument")));
					return ExitStatus.error();
				case 1:
					Path arg = Paths.get(args.get(0));
					final Path newCwd;
					if (arg.isAbsolute()) {
						newCwd = arg;
					} else {
						newCwd = Paths.get(state.getCwd().toString(), args.get(0));
					}
					if (Files.isDirectory(newCwd)) {
						state.setCwd(newCwd);
						return ExitStatus.success();
					} else {
						err.send(Record.of("error", Values.ofText("not a directory")));
						return ExitStatus.error();
					}
				default:
					err.send(Record.of("error", Values.ofText("expecting one path argument")));
					return ExitStatus.error();
			}
		}
	}

	public static class Lines implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			switch (args.size()) {
				case 1:
					Path path = Paths.get(args.get(0));
					if (!path.isAbsolute()) {
						path = Paths.get(state.getCwd().toString(), args.get(0));
					}
					if (Files.isRegularFile(path)) {
						readLines(path, out, err);
						return ExitStatus.success();
					} else {
						err.send(Record.of("error", Values.ofText("not readable file")));
						return ExitStatus.error();
					}
				default:
					err.send(Record.of("error", Values.ofText("expecting one path argument")));
					return ExitStatus.error();
			}
		}

		private void readLines(Path path, Channel out, Channel err) {
			try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
				lines.forEach(line -> out.send(Record.of("line", Values.ofText(line))));
			} catch (IOException e) {
				err.send(Record.of("exception", Values.ofText(e.getMessage())));
			}
		}
	}

	public static class Find implements Command, StateAware {
		private static final Logger LOGGER = LoggerFactory.getLogger(Find.class);
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			Path arg = Path.of(args.get(0));
			Path start;
			if (arg.isAbsolute()) {
				start = arg;
			} else {
				start = state.getCwd().resolve(arg).normalize();
			}
			LOGGER.debug("find: directory '{}'", start);
			try (Stream<Path> stream = Files.walk(start)) {
				stream.forEach(path -> {
					Record result = Record.of("name", Values.ofLocalPath(path.toAbsolutePath()));
					out.send(result);
				});
				return ExitStatus.success();
			} catch (NoSuchFileException e) {
				err.send(Record.of("error", Values.ofText("path does not exist: " + e.getFile())));
				return ExitStatus.error();
			} catch (IOException e) {
				err.send(Record.of("error", Values.ofText(e.getMessage())));
				return ExitStatus.error();
			}
		}
	}
}
