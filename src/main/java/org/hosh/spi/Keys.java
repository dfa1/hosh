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
package org.hosh.spi;

import java.util.Objects;

import org.hosh.doc.Todo;

/**
 * Standard keys used over and over through all commands. Usually such keys
 * convey some semantic information
 * and some defaults regarding textual representation in terminal.
 *
 * Built-in commands can use "private" keys when appropriate.
 *
 * NB: these could be good candidates for constant pool (JEP 303)
 */
public class Keys {
	/**
	 * Human readable name of something (e.g. name of an env variable).
	 */
	public static final Key NAME = Keys.of("name");
	/**
	 * Human readable value of something (e.g. value of env variable).
	 */
	public static final Key VALUE = Keys.of("value");
	/**
	 * An error message.
	 */
	public static final Key ERROR = Keys.of("error");
	/**
	 * An informative message.
	 */
	public static final Key MESSAGE = Keys.of("message");
	/**
	 * Denotes an external input (e.g. output of a native command). Usually contains
	 * an unstructured text value.
	 */
	public static final Key LINE = Keys.of("line");
	/**
	 * Denotes a local path to system.
	 */
	public static final Key PATH = Keys.of("path");
	/**
	 * Denotes a value measured in bytes.
	 */
	public static final Key SIZE = Keys.of("size");
	/**
	 * A numeric value representing a count.
	 */
	public static final Key COUNT = Keys.of("count");
	/**
	 * A numeric value representing an index in a set of values
	 * (e.g. 'cmd | enumerate').
	 */
	public static final Key INDEX = Keys.of("index");
	/**
	 * Denotes a random generated value.
	 */
	public static final Key RAND = Keys.of("rand");

	private Keys() {
	}

	public static Key of(String key) {
		return new StringKey(key);
	}

	@Todo(description = "should be also non-empty and a single word in order to be used as config key")
	private static class StringKey implements Key {
		private final String name;

		public StringKey(String name) {
			if (name == null) {
				throw new IllegalArgumentException("name is null");
			}
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				StringKey that = (StringKey) obj;
				return this.name.equals(that.name);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public String toString() {
			return String.format("Key[%s]", name);
		}
	}
}
