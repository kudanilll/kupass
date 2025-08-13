package com.nielcode.kupass.utils;

public class Config {

	public static final String FILES_PREFIX = "kupass_backup_";
	public static final String EMPTY_STRING = "";

	public static class Language {
		public static final String DEFAULT = "default";
		public static final String ENGLISH = "en";
		public static final String INDONESIA = "in";
		// Contributor: You can add your language here.

		public static class Code {
			public static final int DEFAULT = 0;
			public static final int ENGLISH = 1;
			public static final int INDONESIA = 2;
			// Contributor: You can add your language code here.
		}
	}

	public static class Theme {
		public static final String SYSTEM = "default";
		public static final String LIGHT = "light";
		public static final String DARK = "dark";

		public static class Code {
			public static final int SYSTEM = 0;
			public static final int LIGHT = 1;
			public static final int DARK = 2;
		}
	}

	public static class FileType {
		public static final int CSV = 0;
		public static final int JSON = 1;
		public static final int TEXT = 2;
	}
}
