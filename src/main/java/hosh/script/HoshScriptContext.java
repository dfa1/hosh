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
package hosh.script;

import hosh.BootstrapBuiltins;
import hosh.PathInitializer;
import hosh.doc.Todo;
import hosh.runtime.CommandCompleter;
import hosh.runtime.CommandResolver;
import hosh.runtime.CommandResolvers;
import hosh.runtime.Compiler;
import hosh.runtime.Compiler.Program;
import hosh.runtime.ConsoleChannel;
import hosh.runtime.DisabledHistory;
import hosh.runtime.FileSystemCompleter;
import hosh.runtime.Injector;
import hosh.runtime.Interpreter;
import hosh.runtime.VariableExpansionCompleter;
import hosh.runtime.VersionLoader;
import hosh.spi.Ansi;
import hosh.spi.ExitStatus;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptContext;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class HoshScriptContext implements ScriptContext {

	private final Logger logger = LoggerFactory.forEnclosingClass();

	private final Terminal terminal;

	private final Compiler compiler;

	private final Interpreter interpreter;

	private final State state;

	private final OutputChannel out;

	private final OutputChannel err;

	@Todo(description = "find a way to disable history")
	public HoshScriptContext() throws IOException {
		String version = VersionLoader.loadVersion();
		logger.info(() -> String.format("starting hosh %s", version));
		terminal = TerminalBuilder.builder().exec(false).jna(true).build();
		state = new State();
		state.setCwd(Paths.get("."));
		state.getVariables().putAll(System.getenv());
		state.setPath(new PathInitializer().initializePath(System.getenv("PATH")));
		new BootstrapBuiltins().registerAllBuiltins(state);
		out = new ConsoleChannel(terminal.writer(), Ansi.Style.NONE);
		err = new ConsoleChannel(terminal.writer(), Ansi.Style.NONE);
		Injector injector = new Injector();
		injector.setHistory(new DisabledHistory());
		injector.setLineReader(LineReaderBuilder.builder().terminal(terminal).build());
		injector.setTerminal(terminal);
		injector.setState(state);
		CommandResolver commandResolver = CommandResolvers.builtinsThenExternal(state, injector);
		compiler = new Compiler(commandResolver);
		interpreter = new Interpreter(state);
	}

	public ExitStatus eval(String script) {
		try {
			Program program = compiler.compile(script);
			return interpreter.eval(program, out, err);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "caught exception", e);
			err.send(Records.singleton(Keys.ERROR, Values.ofText(Objects.toString(e.getMessage(), "(no message)"))));
			return ExitStatus.error();
		}
	}

	public LineReader createLineReader() {
		return LineReaderBuilder
				.builder()
				.appName("hosh")
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.completer(new AggregateCompleter(
						new CommandCompleter(state),
						new FileSystemCompleter(state),
						new VariableExpansionCompleter(state)))
				.terminal(terminal)
				.build();
	}

	public boolean isExit() {
		return state.isExit();
	}

	@Override
	public void setBindings(Bindings bindings, int scope) {
	}

	@Override
	public Bindings getBindings(int scope) {
		return null;
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
	}

	@Override
	public Object getAttribute(String name, int scope) {
		return null;
	}

	@Override
	public Object removeAttribute(String name, int scope) {
		return null;
	}

	@Override
	public Object getAttribute(String name) {
		return null;
	}

	@Override
	public int getAttributesScope(String name) {
		return 0;
	}

	@Override
	public Writer getWriter() {
		return Writer.nullWriter();
	}

	@Override
	public Writer getErrorWriter() {
		return Writer.nullWriter();
	}

	@Override
	public void setWriter(Writer writer) {
	}

	@Override
	public void setErrorWriter(Writer writer) {
	}

	@Override
	public Reader getReader() {
		return Reader.nullReader();
	}

	@Override
	public void setReader(Reader reader) {
	}

	@Override
	public List<Integer> getScopes() {
		return Collections.emptyList();
	}
}
