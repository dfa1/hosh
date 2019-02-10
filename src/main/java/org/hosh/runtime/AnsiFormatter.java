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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class AnsiFormatter extends Formatter {
	private final DateTimeFormatter dateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Override
	public String format(LogRecord record) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			LocalDateTime zdt = LocalDateTime.ofInstant(record.getInstant(), ZoneOffset.UTC);
			Ansi.Style style = colorize(record.getLevel());
			pw.append(dateTime.format(zdt));
			pw.append(' ');
			pw.append('[');
			style.enable(pw);
			pw.append(record.getLevel().toString());
			style.disable(pw);
			pw.append(']');
			pw.append(' ');
			pw.append('[');
			pw.append(Thread.currentThread().getName()); // looks like logging happens in the same thread of the 'Logger.log' method call
			pw.append(']');
			pw.append(' ');
			pw.append('-');
			pw.append(' ');
			style.enable(pw);
			pw.append(record.getMessage());
			style.disable(pw);
			pw.println();
			if (record.getThrown() != null) {
				record.getThrown().printStackTrace(pw);
			}
			return sw.toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Ansi.Style colorize(Level level) {
		if (Level.SEVERE.equals(level)) {
			return Ansi.Style.FG_RED;
		}
		if (Level.WARNING.equals(level)) {
			return Ansi.Style.FG_YELLOW;
		}
		return Ansi.Style.NONE;
	}
}