package org.hosh.script;

import hosh.BootstrapBuiltins;
import hosh.PathInitializer;
import hosh.runtime.CommandResolver;
import hosh.runtime.CommandResolvers;
import hosh.runtime.Compiler;
import hosh.runtime.Compiler.Program;
import hosh.runtime.ConsoleChannel;
import hosh.runtime.DisabledHistory;
import hosh.runtime.Injector;
import hosh.runtime.Interpreter;
import hosh.spi.Ansi;
import hosh.spi.ExitStatus;
import hosh.spi.OutputChannel;
import hosh.spi.State;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;

import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class HoshScriptContext implements ScriptContext {

	private final Compiler compiler;

	private final Interpreter interpreter;

	private final State state;

	private final OutputChannel out;

	private final OutputChannel err;

	public HoshScriptContext() throws IOException {
		Terminal terminal = TerminalBuilder.builder().exec(false).jna(true).build();
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
		Program program = compiler.compile(script);
		return interpreter.eval(program, out, err);
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
