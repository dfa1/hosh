package org.hosh;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimpleCommandRegistry implements CommandRegistry {

    private final Map<String, Class<? extends Command>> commandsByName = new HashMap<>();
    private final CommandFactory commandFactory;

    public SimpleCommandRegistry(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public void registerCommand(String name, Class<? extends Command> command) {
        commandsByName.put(name, command);
    }

    @Override
    public void unregisterCommand(String name) {
        commandsByName.remove(name);
    }

    @Override
    public Optional<Command> search(String name) {
        Class<? extends Command> commandClass = commandsByName.get(name);
        return Optional.ofNullable(commandClass).map(commandFactory::create);
    }
}
