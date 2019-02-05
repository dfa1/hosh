/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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

import java.io.IOError;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class LineReaderIterator implements Iterator<String> {
	private final State state;
	private final LineReader lineReader;
	private String nextLine;

	public LineReaderIterator(State state, LineReader lineReader) {
		this.state = state;
		this.lineReader = lineReader;
	}

	@Override
	public boolean hasNext() {
		if (nextLine == null) {
			try {
				nextLine = lineReader.readLine(computePrompt());
			} catch (EndOfFileException e) {
				nextLine = null;
				return false;
			} catch (UserInterruptException e) {
				nextLine = "";
				return true;
			} catch (IOError e) {
				// this happens when the user hits ctrl-c before Jline sets the signal handler
				if (e.getCause() instanceof java.io.InterruptedIOException) {
					nextLine = "";
					return true;
				} else {
					throw e;
				}
			}
		}
		return true;
	}

	@Override
	public String next() {
		if (nextLine == null) {
			throw new NoSuchElementException();
		} else {
			String line = nextLine;
			nextLine = null;
			return line;
		}
	}

	private String computePrompt() {
		return new AttributedStringBuilder()
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("hosh:")
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
				.append(String.valueOf(state.getId()))
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("> ")
				.style(AttributedStyle.DEFAULT)
				.toAnsi(lineReader.getTerminal());
	}
}
