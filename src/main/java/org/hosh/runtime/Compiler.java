package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.ArgContext;
import org.hosh.antlr4.HoshParser.InvocationContext;
import org.hosh.antlr4.HoshParser.PipelineContext;
import org.hosh.antlr4.HoshParser.StmtContext;
import org.hosh.antlr4.HoshParser.WrappedContext;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;

public class Compiler {
	private final CommandResolver commandResolver;

	public Compiler(CommandResolver commandResolver) {
		this.commandResolver = commandResolver;
	}

	public Program compile(String input) {
		Parser parser = new Parser();
		HoshParser.ProgramContext programContext = parser.parse(input);
		Program program = new Program();
		List<Statement> statements = new ArrayList<>();
		for (StmtContext stmtContext : programContext.stmt()) {
			Statement statement = compileStatement(stmtContext);
			statements.add(statement);
		}
		program.setStatements(statements);
		return program;
	}

	private Statement compileStatement(StmtContext ctx) {
		if (ctx.single() != null) {
			return compileInvocation(ctx.single().invocation());
		}
		if (ctx.wrapped() != null) {
			return compileWrappedCommand(ctx.wrapped());
		}
		if (ctx.pipeline() != null) {
			return compilePipeline(ctx.pipeline());
		}
		throw new InternalBug();
	}

	private Statement compilePipeline(PipelineContext ctx) {
		Statement producer = compileInvocation(ctx.invocation());
		Statement consumer = compileStatement(ctx.stmt());
		producer.setNext(consumer);
		return producer;
	}

	private Statement compileInvocation(InvocationContext ctx) {
		Token token = ctx.ID().getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError(String.format("line %d: '%s' unknown command", token.getLine(), commandName));
		}
		List<String> commandArgs = compileArguments(ctx);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		return statement;
	}

	private Statement compileWrappedCommand(WrappedContext ctx) {
		Token token = ctx.invocation().ID().getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError(String.format("line %d: '%s' unknown command wrapper", token.getLine(), commandName));
		}
		if (!(command instanceof CommandWrapper)) {
			throw new CompileError(String.format("line %d: '%s' is not a command wrapper", token.getLine(), commandName));
		}
		CommandWrapper<?> commandWrapper = (CommandWrapper<?>) command;
		Statement nestedStatement = compileStatement(ctx.stmt());
		List<String> commandArgs = compileArguments(ctx.invocation());
		Statement statement = new Statement();
		statement.setCommand(new DefaultCommandWrapper<>(nestedStatement, commandWrapper));
		statement.setArguments(commandArgs);
		return statement;
	}

	private List<String> compileArguments(InvocationContext ctx) {
		return ctx
				.arg()
				.stream()
				.map(this::compileArgument)
				.collect(Collectors.toList());
	}

	private String compileArgument(ArgContext ctx) {
		if (ctx.VARIABLE() != null) {
			Token token = ctx.VARIABLE().getSymbol();
			return token.getText();
		}
		if (ctx.ID() != null) {
			Token token = ctx.ID().getSymbol();
			return token.getText();
		}
		if (ctx.STRING() != null) {
			Token token = ctx.STRING().getSymbol();
			return dropQuotes(token);
		}
		throw new InternalBug();
	}

	// "some text" -> some text
	private String dropQuotes(Token token) {
		String text = token.getText();
		return text.substring(1, text.length() - 1);
	}

	public static class Program {
		private List<Statement> statements;

		public void setStatements(List<Statement> statements) {
			this.statements = statements;
		}

		public List<Statement> getStatements() {
			return statements;
		}

		@Override
		public String toString() {
			return String.format("Program[%s]", statements);
		}
	}

	public static class Statement {
		private Command command;
		private List<String> arguments;
		private Statement next; // not null if part of a pipeline

		public void setCommand(Command command) {
			this.command = command;
		}

		public Command getCommand() {
			return command;
		}

		public List<String> getArguments() {
			return arguments;
		}

		public void setArguments(List<String> arguments) {
			this.arguments = arguments;
		}

		public void setNext(Statement next) {
			this.next = next;
		}

		public Statement getNext() {
			return next;
		}

		@Override
		public String toString() {
			return String.format("Statement[class=%s,arguments=%s,pipe=%s]", command.getClass().getCanonicalName(), arguments, next);
		}
	}

	public static class CompileError extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public CompileError(String message) {
			super(message);
		}
	}

	public static class InternalBug extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
