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
package org.hosh.fitness;

import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JUnitFitnessTest {

	@Test
	public void enforcePresenceOfTestAnnotation() {
		try (ScanResult scanResult = new ClassGraph().whitelistPackages("org.hosh").scan()) {
			scanResult
					.getAllClasses()
					.loadClasses()
					.stream()
					.filter(c -> c.getName().endsWith("Test") || c.getName().endsWith("IT"))
					.flatMap(c -> Stream.of(c.getDeclaredMethods()))
					.filter(m -> Modifier.isPublic(m.getModifiers()))
					.filter(m -> !Modifier.isStatic(m.getModifiers()))
					.filter(m -> !m.isAnnotationPresent(Before.class))
					.filter(m -> !m.isAnnotationPresent(After.class))
					.filter(m -> !m.isAnnotationPresent(Test.class))
					.forEach(m -> {
						fail("method should be annotated with @Test: " + m);
					});
		}
	}
}