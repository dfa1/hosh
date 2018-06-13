package org.hosh.spi;

/** Modules uses this to register command classes */
public interface CommandRegistry {

	void registerCommand(String name, Class<? extends Command> command);

}
