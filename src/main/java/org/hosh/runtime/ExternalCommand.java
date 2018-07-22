package org.hosh.runtime;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalCommand implements Command, StateAware {

	private static final Logger logger = LoggerFactory.getLogger(ExternalCommand.class);
	private final Path command;
	private State state;

	public ExternalCommand(Path command) {
		this.command = command;
	}

	@Override
	public void run(List<String> args, Channel out, Channel err) {
		List<String> processArgs = new ArrayList<>();
		processArgs.add(command.toAbsolutePath().toString());
		processArgs.addAll(args);
		logger.info("executing command {}", processArgs);
		try {
			Process process= new ProcessBuilder(processArgs.toArray(new String[0]))
				.directory(state.getCwd().toFile())
				.redirectError(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT)
				.start();
			int exitCode = process.waitFor();
			out.send(Record.of("message", Values.ofText("exit code: " + exitCode)));
		} catch (IOException | InterruptedException e) {
			err.send(Record.of("error", Values.ofText(e.getMessage())));
			logger.error("got", e);
		}
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

}