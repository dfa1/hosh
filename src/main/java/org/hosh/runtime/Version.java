package org.hosh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

	private Version() {
	}

	// TODO: rename this class to VersionLoader, Version should be a value object
	public static String readVersion() throws IOException {
		try (InputStream is = Version.class.getResourceAsStream("/git.properties")) {
			Properties properties = new Properties();
			properties.load(is);
			return String.format("%s (%s)", properties.getProperty("git.build.version"),
					properties.getProperty("git.commit.id.abbrev"));
		}
	}

}
