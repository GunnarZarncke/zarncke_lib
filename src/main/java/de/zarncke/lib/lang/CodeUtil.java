package de.zarncke.lib.lang;

import java.lang.reflect.Member;


public final class CodeUtil {
	private CodeUtil() {
		// helper
	}

	public static Piece named(final String name) {
		return new Piece() {
			@Override
			public String getName() {
				return name;
			}
		};
	}

	public static Piece of(final Class<?> clazz) {
		return new Piece() {
			@Override
			public String getName() {
				return clazz.getName();
			}
		};
	}

	public static Piece of(final Package pack) {
		return new Piece() {
			@Override
			public String getName() {
				return pack.getName();
			}
		};
	}

	public static Piece of(final Member member) {
		return new Piece() {
			@Override
			public String getName() {
				return member.getName();
			}
		};
	}
}
