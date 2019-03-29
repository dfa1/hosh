/**
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
package org.hosh.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hosh.doc.Todo;
import org.hosh.spi.Ansi;
import org.hosh.spi.State;

public class Prompt {

	private final State state;

	public Prompt(State state) {
		this.state = state;
	}

	@Todo(description = "this should be user-configurable")
	public String compute() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("hosh:");
		Ansi.Style.FG_CYAN.enable(pw);
		pw.append(String.valueOf(state.getId()));
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("> ");
		Ansi.Style.RESET.enable(pw);
		return sw.toString();
	}
}
