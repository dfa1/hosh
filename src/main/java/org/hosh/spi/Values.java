package org.hosh.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Built-in values to be used in Records.
 *
 * NB: actual value types are not exposed by purpose
 */
public class Values {
	private Values() {
	}

	public static Value ofText(String text) {
		return new Text(text);
	}

	public enum Unit {
		B, KB, MB, GB, TB
	}

	// indexed units table
	private static final Unit[] UNITS = { Unit.KB, Unit.MB, Unit.GB, Unit.TB };
	/**
	 * One kibibyte (1024 bytes), this is in contrast to the SI system (1000 bytes)
	 */
	public static final int KiB = 1024;

	/**
	 * Select the appropriate unit for measuring bytes.
	 */
	public static Value ofHumanizedSize(long bytes) {
		if (bytes < KiB) {
			return new Size(BigDecimal.valueOf(bytes), Unit.B);
		}
		int exp = (int) (Math.log(bytes) / Math.log(KiB));
		Unit unit = UNITS[exp - 1];
		BigDecimal value = BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(Math.pow(KiB, exp)), 1, RoundingMode.HALF_UP);
		return new Size(value, unit);
	}

	/**
	 * Paths, without any special attributes.
	 */
	public static Value ofLocalPath(Path path) {
		return new LocalPath(path);
	}

	/**
	 * Generic text, make sure that text hasn't any number or date formatted without
	 * the current locale.
	 */
	static final class Text implements Value {
		private final String value;

		public Text(String value) {
			this.value = value;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(value);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Text[%s]", value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Text) {
				Text that = (Text) obj;
				return Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	/**
	 * Used to represent a size of a file, etc.
	 */
	static final class Size implements Value {
		private final BigDecimal value;
		private final Unit unit;

		public Size(BigDecimal value, Unit unit) {
			if (value.compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("negative size");
			}
			this.value = value;
			this.unit = unit;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			NumberFormat instance = NumberFormat.getInstance(locale);
			try {
				appendable.append(instance.format(value) + unit.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Size[%s%s]", value, unit);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Size) {
				Size that = (Size) obj;
				return Objects.equals(this.value, that.value) && Objects.equals(this.unit, that.unit);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, unit);
		}
	}

	static final class LocalPath implements Value {
		private final Path path;

		public LocalPath(Path path) {
			this.path = path;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(path.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("LocalPath[%s]", path);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LocalPath) {
				LocalPath that = (LocalPath) obj;
				return Objects.equals(this.path, that.path);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}
	}
}
