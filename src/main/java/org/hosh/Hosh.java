package org.hosh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.CommandFactory;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.ConsoleChannel;
import org.hosh.runtime.Interpreter;
import org.hosh.runtime.LineReaderIterator;
import org.hosh.runtime.SimpleCommandRegistry;
import org.hosh.runtime.Version;
import org.hosh.spi.Channel;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.SimpleChannel;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main class */
public class Hosh {

	// TODO: configure logger to log under hidden home directory
	private static final Logger logger = LoggerFactory.getLogger(Hosh.class);

	public static void main(String[] args) throws Exception {
		Terminal terminal = TerminalBuilder
				.builder()
				.system(true)
				.build();
		String prompt = new AttributedStringBuilder()
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("hosh> ")
				.style(AttributedStyle.DEFAULT)
				.toAnsi(terminal);
		State state = new State();
		state.setPrompt(prompt);
		LineReader lineReader = LineReaderBuilder
				.builder()
				.appName("hosh")
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000")
				.completer(new CommandCompleter(state))
				.terminal(terminal)
				.build();
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		LineReaderIterator read = new LineReaderIterator(state, lineReader);
		CommandFactory commandFactory = new CommandFactory(state, terminal);
		Compiler compiler = new Compiler(state, commandFactory);

		if (args.length == 0) {
			Channel out = new ConsoleChannel(terminal, AttributedStyle.WHITE);
			Channel err = new ConsoleChannel(terminal, AttributedStyle.RED);
			Interpreter interpreter = new Interpreter(out, err);
			welcome(out);
			repl(read, compiler, interpreter, err);
		} else {
			Channel out = new SimpleChannel(System.out);
			Channel err = new SimpleChannel(System.err);
			Interpreter interpreter = new Interpreter(out, err);
			script(args[0], compiler, interpreter, err);
		}

	}

	private static void script(String path, Compiler compiler, Interpreter interpreter, Channel err)
			throws IOException {
		try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
			String script = lines.collect(Collectors.joining("\n"));
			Program program = compiler.compile(script);
			interpreter.eval(program);
			System.exit(0);
		} catch (IOException e) {
			err.send(Record.of("message", Values.ofText("unable to load: " + path)));
			logger.debug("caught exception for load: " + path, e);
			System.exit(1);
		} catch (Exception e) {
			logger.debug("caught exception", e);
			err.send(Record.of("message", Values.ofText(e.getMessage())));
			System.exit(1);
		}
	}

	private static void repl(LineReaderIterator read, Compiler compiler, Interpreter interpreter, Channel err) {
		while (read.hasNext()) {
			String line = read.next();
			try {
				Program program = compiler.compile(line);
				interpreter.eval(program);
			} catch (RuntimeException e) {
				logger.debug("caught exception for input: " + line, e);
				err.send(Record.of("message", Values.ofText(e.getMessage())));
			}
		}
		System.exit(0);
	}

	private static void welcome(Channel out) throws IOException {
		out.send(Record.of("message", Values.ofText("hosh v" + Version.readVersion())));
		out.send(Record.of("message", Values.ofText("Running on Java " + System.getProperty("java.version"))));
	}

}