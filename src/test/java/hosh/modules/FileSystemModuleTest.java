/*
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
package hosh.modules;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import hosh.doc.Bug;
import hosh.modules.FileSystemModule.ChangeDirectory;
import hosh.modules.FileSystemModule.Copy;
import hosh.modules.FileSystemModule.CurrentWorkingDirectory;
import hosh.modules.FileSystemModule.Find;
import hosh.modules.FileSystemModule.Hardlink;
import hosh.modules.FileSystemModule.Lines;
import hosh.modules.FileSystemModule.ListFiles;
import hosh.modules.FileSystemModule.Move;
import hosh.modules.FileSystemModule.Partitions;
import hosh.modules.FileSystemModule.Probe;
import hosh.modules.FileSystemModule.Remove;
import hosh.modules.FileSystemModule.Resolve;
import hosh.modules.FileSystemModule.Symlink;
import hosh.modules.FileSystemModule.WithLock;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.testsupport.RecordMatcher;
import hosh.testsupport.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

public class FileSystemModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ListFilesTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private ListFiles sut;

		@Test
		public void errorTwoOrMoreArgs() {
			ExitStatus exitStatus = sut.run(List.of("dir1", "dir2"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected at most 1 argument")));
		}

		@Test
		public void zeroArgsWithEmptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFolder("dir").mkdir();
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(
					RecordMatcher.of(
							Keys.PATH, Values.ofPath(Paths.get("dir")),
							Keys.SIZE, Values.none()));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("file")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneSymlink() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("file").toPath();
			Files.createSymbolicLink(Paths.get(state.getCwd().toString(), "link"), file);
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("file")),
					Keys.SIZE, Values.ofSize(0)));
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("link")),
					Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgWithRelativeFile() throws IOException {
			Path file = temporaryFolder.newFile().toPath();
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(file.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + file.toAbsolutePath().toString())));
		}

		@Test
		public void oneArgWithEmptyDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFolder().toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgWithNonEmptyDirectory() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(newFolder.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(ArgumentMatchers.any());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgReferringToCwd() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(newFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgAbsoluteFile() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Path path = Files.createFile(new File(newFolder, "aaa").toPath()).toAbsolutePath();
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(path.toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + path)));
		}

		@Test
		public void oneArgAbsoluteDir() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(newFolder.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void listDot() throws IOException {
			temporaryFolder.newFile("aaa");
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void listDotDot() throws IOException {
			File cwd = temporaryFolder.newFolder("aaa");
			given(state.getCwd()).willReturn(cwd.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(".."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(
					RecordMatcher.of(
							Keys.PATH, Values.ofPath(Path.of("aaa")),
							Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@DisabledOnOs(OS.WINDOWS) // File.setReadable() fails on windows
		@Bug(description = "check handling of java.nio.file.AccessDeniedException", issue = "https://github.com/dfa1/hosh/issues/74")
		@Test
		public void accessDenied() {
			File cwd = temporaryFolder.toFile();
			assert cwd.exists();
			assert cwd.setReadable(false, true);
			assert cwd.setExecutable(false, true);
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("access denied: " + cwd.toString())));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ChangeDirectoryTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private ChangeDirectory sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument (directory)")));
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd", "asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument (directory)")));
		}

		@Test
		public void oneDirectoryRelativeArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of("dir"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(state).should().setCwd(newFolder.toPath().toAbsolutePath());
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectoryAbsoluteArgument() throws IOException {
			File newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of(newFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(state).should().setCwd(newFolder.toPath());
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneFileArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CurrentWorkingDirectoryTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private CurrentWorkingDirectory sut;

		@Test
		public void noArgs() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(temporaryFolder.toPath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting no arguments")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class LinesTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Lines sut;

		@Test
		public void emptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void nonEmptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile, StandardCharsets.UTF_8)) {
				writer.write("a 1\n");
				writer.write("b 2\n");
			}
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("b 2")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void nonEmptyFileInCwd() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile, StandardCharsets.UTF_8)) {
				writer.write("a 1\n");
			}
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void directory() {
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not readable file")));
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting one path argument")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CopyTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Copy sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
		}

		@Test
		public void copyRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File source = temporaryFolder.newFile("source.txt");
			File target = temporaryFolder.newFile("target.txt");
			assert target.delete();
			ExitStatus exitStatus = sut.run(List.of(source.getName(), target.getName()), in, out, err);
			assertThat(source.exists()).isTrue();
			assertThat(target.exists()).isTrue();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void copyAbsoluteToAbsolute() throws IOException {
			File source = temporaryFolder.newFile("source.txt").getAbsoluteFile();
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			assert target.delete();
			ExitStatus exitStatus = sut.run(List.of(source.getAbsolutePath(), target.getAbsolutePath()), in, out, err);
			assertThat(source.exists()).isTrue();
			assertThat(target.exists()).isTrue();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class MoveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Move sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
		}

		@Test
		public void moveRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File source = temporaryFolder.newFile("source.txt");
			File target = temporaryFolder.newFile("target.txt");
			assert target.delete();
			ExitStatus exitStatus = sut.run(List.of(source.getName(), target.getName()), in, out, err);
			assertThat(source.exists()).isFalse();
			assertThat(target.exists()).isTrue();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void moveAbsoluteToAbsolute() throws IOException {
			File source = temporaryFolder.newFile("source.txt").getAbsoluteFile();
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			assert target.delete();
			ExitStatus exitStatus = sut.run(List.of(source.getAbsolutePath(), target.getAbsolutePath()), in, out, err);
			assertThat(source.exists()).isFalse();
			assertThat(target.exists()).isTrue();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class RemoveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Remove sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: rm target")));
		}

		@Test
		public void removeRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			ExitStatus exitStatus = sut.run(List.of(target.getName()), in, out, err);
			assertThat(target.exists()).isFalse();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void removeAbsolute() throws IOException {
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			ExitStatus exitStatus = sut.run(List.of(target.getAbsolutePath()), in, out, err);
			assertThat(target.exists()).isFalse();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class PartitionsTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Partitions sut;

		@Test
		public void listAllPartitions() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should(Mockito.atLeastOnce()).send(Mockito.any());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: partitions")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class FindTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Find sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void relativePath() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath())));
			then(err).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
		}

		@Test
		public void nonExistentRelativePath() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			assert newFile.delete();
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isError();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("path does not exist: " + newFile)));
			then(out).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
		}

		@Test
		public void absolutePath() throws IOException {
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath())));
			then(err).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
		}

		@Test
		public void nonExistentAbsolutePath() throws IOException {
			File newFile = temporaryFolder.newFile("file.txt");
			assert newFile.delete();
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isError();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("path does not exist: " + newFile)));
			then(out).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
		}

		// on Windows a special permission is needed to create symlinks, see
		// https://stackoverflow.com/a/24353758
		@DisabledOnOs(OS.WINDOWS)
		@Test
		public void resolveSymlinks() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFolder = temporaryFolder.newFolder("folder");
			File symlink = new File(temporaryFolder.toFile(), "symlink");
			Files.createSymbolicLink(symlink.toPath(), newFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("symlink"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(err).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFolder.toPath())));
			then(in).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ProbeTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Probe sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: probe file")));
		}

		@Test
		public void probeKnownRelativeFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.of("contenttype"), Values.ofText("text/plain")));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void probeUnknownRelativeFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.hosh");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("content type cannot be determined")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SymlinkTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Symlink sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: symlink source target")));
		}

		@Test
		public void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName(), "link"), in, out, err);
			Path link = temporaryFolder.toPath().resolve("link");
			assertThat(link).isSymbolicLink();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class HardlinkTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Hardlink sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: hardlink source target")));
		}

		@Test
		public void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName(), "link"), in, out, err);
			Path link = temporaryFolder.toPath().resolve("link");
			assertThat(link).exists();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ResolveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Resolve sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: resolve file")));
		}

		@Test
		public void regularRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("file.txt"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void regularAbsolute() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void symlink() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			Files.createSymbolicLink(Path.of(temporaryFolder.toFile().getAbsolutePath(), "link"), newFile.toPath());
			ExitStatus exitStatus = sut.run(List.of("link"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class WithLockTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private WithLock sut;

		@Test
		public void noArgs() {
			assertThatThrownBy(() -> sut.before(List.of(), in, out, err))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("expecting file name");
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void wrongFile() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			assertThatThrownBy(() -> sut.before(List.of("../missing_directory/file.txt"), in, out, err))
					.isInstanceOf(UncheckedIOException.class);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@DisabledOnOs(OS.WINDOWS) // cannot delete an opened lock file
		@Test
		public void lock() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File lockFile = temporaryFolder.newFile("file.txt");
			RandomAccessFile resource = sut.before(List.of("file.txt"), in, out, err);
			assertThat(resource).isNotNull();
			// under same JVM tryLock throws exception
			assertThatThrownBy(() -> resource.getChannel().tryLock())
					.isInstanceOf(OverlappingFileLockException.class);
			sut.after(resource, in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			assertThat(lockFile).doesNotExist();
		}
	}
}
