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

import hosh.runtime.Compiler.Statement;
import hosh.spi.Command;
import hosh.spi.CommandDecorator;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;

import java.util.List;

// generated by compiler for 'cmd { ... }'
class DefaultCommandDecorator implements Command, InterpreterAware {

    private final Statement nested;

    private final CommandDecorator commandDecorator;

    private Interpreter interpreter;

    public DefaultCommandDecorator(Statement nested, CommandDecorator commandDecorator) {
        this.nested = nested;
        this.commandDecorator = commandDecorator;
    }

    @Override
    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
        // CommandNested is just a way to hide Interpreter and other internals to the modules
        commandDecorator.setCommandNested(() -> interpreter.eval(nested, in, out, err));
        return commandDecorator.run(args, in, out, err);
    }

    @Override
    public String toString() {
        return String.format("DefaultCommandDecorator[nested=%s,commandDecorator=%s]", nested, commandDecorator);
    }
}
