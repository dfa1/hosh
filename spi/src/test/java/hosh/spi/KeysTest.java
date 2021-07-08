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
package hosh.spi;

import hosh.spi.Keys.StringKey;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeysTest {

	@Test
	void nullKey() {
		assertThatThrownBy(() -> Keys.of(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("name must be not null");
	}

	@Test
	void emptyKey() {
		assertThatThrownBy(() -> Keys.of(""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("name must be not blank");
	}

	@Test
	void asString() {
		assertThat(Keys.of("name"))
			.hasToString("Key['name']");
	}

	@Disabled("EqualsVerifier not compatible with java 16 yet")
	@Test
	void equalContract() {
		EqualsVerifier.configure()
			.forClass(StringKey.class)
			.verify();
	}

	@Test
	void compareTo() {
		Key a = Keys.of("name");
		Key b = Keys.of("b");
		assertThat(a).isEqualByComparingTo(a);
		assertThat(a).isNotEqualByComparingTo(b);
	}
}
