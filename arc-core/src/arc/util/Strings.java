package arc.util;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class Strings {
	public static final Charset utf8 = Charset.forName("UTF-8");
	public static final Charset ascii = Charset.forName("US-ASCII");

	private static final byte[] hexArray = "0123456789ABCDEF".getBytes(ascii);
	private static StringBuilder tmp1 = new StringBuilder(), tmp2 = new StringBuilder();
	private static Pattern
			filenamePattern = Pattern.compile("[\0/\"<>|:*?\\\\]"),
			unsafeFilenamePattern = Pattern.compile("[\0/\"'<>|:*!?\\\\]"),
			reservedFilenamePattern = Pattern.compile("(CON|AUX|PRN|NUL|(COM[0-9])|(LPT[0-9]))((\\..*$)|$)", Pattern.CASE_INSENSITIVE);

	/** @return sha256 hash of the given string 给定字符串的 sha256 哈希 */
	public static byte[] sha256(String str) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(str.getBytes(utf8));
		} catch (NoSuchAlgorithmException e) {
			throw new ArcRuntimeException(e);
		}
	}

	//https://stackoverflow.com/a/3758880
	public static String formatByteCount(long bytes) {
		if (-1000 < bytes && bytes < 1000) return bytes + " B";

		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	//https://stackoverflow.com/a/9855338
	public static String bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars, utf8);
	}

	public static String getFileExtension(String path) {
		int dotIndex = path.lastIndexOf('.');
		return dotIndex == -1 ? "" : path.substring(dotIndex + 1);
	}

	public static String getFileName(String path) {
		int index = path.lastIndexOf('/');
		return index < 0 ? path : path.substring(index + 1);
	}

	public static String getFileNameWithoutExtension(String path) {
		String name = getFileName(path);
		int dotIndex = name.lastIndexOf('.');
		return dotIndex == -1 ? name : name.substring(0, dotIndex);
	}

	/** @return whether the name matches the query; case-insensitive. Always returns true if query is empty. 名称是否匹配查询;不区分大小写。查询为空时始终返回 true。 */
	public static boolean matches(String query, String name) {
		return query == null || query.isEmpty() || (name != null && name.toLowerCase().contains(query.toLowerCase()));
	}

	public static int count(CharSequence s, char c) {
		int total = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) total++;
		}
		return total;
	}

	public static String repeat(String str, int count) {
		if (count <= 0) return "";
		tmp2.setLength(0);
		tmp2.ensureCapacity(str.length() * count);
		for (int i = 0; i < count; i++) {
			tmp2.append(str);
		}
		return tmp2.toString();
	}

	public static String truncate(String s, int length) {
		return s.length() <= length ? s : s.substring(0, length);
	}

	public static String truncate(String s, int length, String ellipsis) {
		return s.length() <= length ? s : s.substring(0, length) + ellipsis;
	}

	public static Ar<Throwable> getCauses(Throwable e) {
		Ar<Throwable> arr = new Ar<>();
		while (e != null) {
			arr.add(e);
			e = e.getCause();
		}
		return arr;
	}

	public static String getSimpleMessage(Throwable e) {
		Throwable fcause = getFinalCause(e);
		return fcause.getMessage() == null ? fcause.getClass().getSimpleName() : fcause.getClass().getSimpleName() + ": " + fcause.getMessage();
	}

	public static String getSimpleMessages(Throwable e) {
		StringBuilder builder = new StringBuilder();
		while (e != null) {
			if (e.getMessage() != null) {
				builder.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
			} else {
				builder.append(e.getClass().getSimpleName());
			}
			e = e.getCause();
			if (e != null) {
				builder.append(" -> ");
			}
		}
		return builder.toString();
	}

	public static String getFinalMessage(Throwable e) {
		String message = e.getMessage();
		while (e.getCause() != null) {
			e = e.getCause();
			if (e.getMessage() != null) {
				message = e.getMessage();
			}
		}
		return message;
	}

	public static Throwable getFinalCause(Throwable e) {
		while (e.getCause() != null) {
			e = e.getCause();
		}
		return e;
	}

	public static String getStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/** @return a neat error message of a throwable, with stack trace. 返回 throwable 的整洁错误消息,包含堆栈跟踪。 */
	public static String neatError(Throwable e) {
		return neatError(e, true);
	}

	/** @return a neat error message of a throwable, with stack trace. 返回 throwable 的整洁错误消息,包含堆栈跟踪。 */
	public static String neatError(Throwable e, boolean stacktrace) {
		StringBuilder build = new StringBuilder();

		while (e != null) {
			String name = e.getClass().toString().substring("class ".length()).replace("Exception", "");
			if (name.indexOf('.') != -1) {
				name = name.substring(name.lastIndexOf('.') + 1);
			}

			build.append("> ").append(name);
			if (e.getMessage() != null) {
				build.append(": ");
				build.append("'").append(e.getMessage()).append("'");
			}

			if (stacktrace) {
				for (StackTraceElement s : e.getStackTrace()) {
					if (s.getClassName().contains("MethodAccessor") || s.getClassName().substring(s.getClassName().lastIndexOf(".") + 1).equals("Method"))
						continue;
					build.append("\n");

					String className = s.getClassName();
					build.append(className.substring(className.lastIndexOf(".") + 1)).append(".").append(s.getMethodName()).append(": ").append(s.getLineNumber());
				}
			}

			build.append("\n");

			e = e.getCause();
		}


		return build.toString();
	}

	public static String stripColors(CharSequence str) {
		StringBuilder out = new StringBuilder(str.length());

		int i = 0;
		while (i < str.length()) {
			char c = str.charAt(i);

			// Possible color tag.
			// 可能是颜色标签。
			if (c == '[') {
				int length = parseColorMarkup(str, i + 1, str.length());
				if (length >= 0) {
					i += length + 2;
				} else {
					out.append(c);
					//escaped string
					// 转义后的字符串
					i++;
				}
			} else {
				out.append(c);
				i++;
			}
		}

		return out.toString();
	}

	public static String stripGlyphs(CharSequence str) {
		StringBuilder out = new StringBuilder(str.length());

		for (int i = 0; i < str.length(); i++) {
			int c = str.charAt(i);
			if (c >= 0xE000 && c <= 0xF8FF) continue;
			out.append((char) c);
		}

		return out.toString();
	}

	private static int parseColorMarkup(CharSequence str, int start, int end) {
		if (start >= end) return -1; // String ended with "[".
		// 字符串以 "[" 结尾。
		switch (str.charAt(start)) {
			case '#':
				// Parse hex color RRGGBBAA where AA is optional and defaults to 0xFF if less than 6 chars are used.
				// 解析十六进制颜色 RRGGBBAA,其中 AA 可选,若使用的字符少于 6 个则默认为 0xFF。
				for (int i = start + 1; i < end; i++) {
					char ch = str.charAt(i);
					if (ch == ']') {
						if (i < start + 2 || i > start + 9) break; // Illegal number of hex digits.
						// 十六进制位数非法。
						return i - start;
					}
					if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F')) {
						break; // Unexpected character in hex color.
						// 十六进制颜色中出现意外字符。
					}
				}
				return -1;
			case '[': // "[[" is an escaped left square bracket.
				// "[[" 是转义的左方括号。
				return -2;
			case ']': // "[]" is a "pop" color tag.
				// "[]" 是 "pop" 颜色标签。
				//pop the color stack here if needed
				// 如有需要,在此弹出颜色栈
				return 0;
		}
		// Parse named color.
		// 解析命名颜色。
		for (int i = start + 1; i < end; i++) {
			char ch = str.charAt(i);
			if (ch != ']') continue;
			Color namedColor = Colors.get(str.subSequence(start, i).toString());
			if (namedColor == null) return -1; // Unknown color name.
			// 未知的颜色名称。
			//namedColor is the result color here
			// 此处 namedColor 即结果颜色
			return i - start;
		}
		return -1; // Unclosed color tag.
		// 未闭合的颜色标签。
	}

	public static int count(String str, String substring) {
		int lastIndex = 0;
		int count = 0;

		while (lastIndex != -1) {

			lastIndex = str.indexOf(substring, lastIndex);

			if (lastIndex != -1) {
				count++;
				lastIndex += substring.length();
			}
		}
		return count;
	}

	/**
	 * Replaces non-safe filename characters with '_'. Handles reserved window file names.
	 * 将文件名中的不安全字符替换为 '_'。会处理 Windows 保留文件名。
	 */
	public static String sanitizeFilename(String str) {
		if (str.equals(".")) {
			return "_";
		} else if (str.equals("..")) {
			return "__";
		} else if (reservedFilenamePattern.matcher(str).matches()) {
			//turn things like con.msch -> _con.msch, which is no longer reserved
			// 把 con.msch 之类的名称变成 _con.msch,从而不再是保留名
			str = "_" + str;
		}
		return filenamePattern.matcher(str).replaceAll("_");
	}

	public static boolean isSafeFilename(String name) {
		return !name.equals(".") && !name.equals("..") && !reservedFilenamePattern.matcher(name).matches() && !unsafeFilenamePattern.matcher(name).find();
	}

	public static String encode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException why) {
			//why the HECK does this even throw an exception
			// 这到底为什么会抛异常
			throw new RuntimeException(why);
		}
	}

	public static String format(String text, Object... args) {
		if (args.length > 0) {
			StringBuilder out = new StringBuilder(text.length() + args.length * 2);
			int argi = 0;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '@' && argi < args.length) {
					out.append(stringify(args[argi++]));
				} else {
					out.append(c);
				}
			}

			return out.toString();
		}

		return text;
	}

	static String stringify(Object o) {
		if (o instanceof Object[]) return Arrays.deepToString((Object[]) o);
		else if (o instanceof int[]) return Arrays.toString((int[]) o);
		else if (o instanceof long[]) return Arrays.toString((long[]) o);
		else if (o instanceof float[]) return Arrays.toString((float[]) o);
		else if (o instanceof double[]) return Arrays.toString((double[]) o);
		else if (o instanceof char[]) return Arrays.toString((char[]) o);
		else if (o instanceof short[]) return Arrays.toString((short[]) o);
		else if (o instanceof byte[]) return Arrays.toString((byte[]) o);
		else if (o instanceof boolean[]) return Arrays.toString((boolean[]) o);
		return String.valueOf(o);
	}

	public static String join(String separator, String... strings) {
		StringBuilder builder = new StringBuilder();
		for (String s : strings) {
			builder.append(s);
			builder.append(separator);
		}
		builder.setLength(builder.length() - separator.length());
		return builder.toString();
	}

	public static String join(String separator, Iterable<String> strings) {
		StringBuilder builder = new StringBuilder();
		for (String s : strings) {
			builder.append(s);
			builder.append(separator);
		}
		builder.setLength(builder.length() - separator.length());
		return builder.toString();
	}

	/**
	 * Returns the levenshtein distance between two strings.
	 * 返回两个字符串之间的 Levenshtein 距离。
	 */
	public static int levenshtein(String x, String y) {
		int[][] dp = new int[x.length() + 1][y.length() + 1];

		for (int i = 0; i <= x.length(); i++) {
			for (int j = 0; j <= y.length(); j++) {
				if (i == 0) {
					dp[i][j] = j;
				} else if (j == 0) {
					dp[i][j] = i;
				} else {
					dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1),
									dp[i - 1][j] + 1),
							dp[i][j - 1] + 1);
				}
			}
		}

		return dp[x.length()][y.length()];
	}

	/**
	 * Returns the case-independent biased levenshtein distance between two strings.
	 * 返回两个字符串之间不区分大小写的带偏 Levenshtein 距离。
	 */
	public static float biasedLevenshtein(String x, String y) {
		x = x.toLowerCase(Locale.ROOT);
		y = y.toLowerCase(Locale.ROOT);

		int[][] dp = new int[x.length() + 1][y.length() + 1];

		for (int i = 0; i <= x.length(); i++) {
			for (int j = 0; j <= y.length(); j++) {
				if (i == 0) {
					dp[i][j] = j;
				} else if (j == 0) {
					dp[i][j] = i;
				} else {
					dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1]
											+ (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1),
									dp[i - 1][j] + 1),
							dp[i][j - 1] + 1);
				}
			}
		}

		float output = dp[x.length()][y.length()];
		if (y.startsWith(x) || x.startsWith(y)) {
			return output / 3f;
		}
		return (y.contains(x) || x.contains(y)) ? output / 1.5f : output;
	}

	public static String animated(float time, int length, float scale, String replacement) {
		return new String(new char[Math.abs((int) (time / scale) % length)]).replace("\0", replacement);
	}

	public static String kebabToCamel(String s) {
		StringBuilder result = new StringBuilder(s.length());

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != '_' && c != '-') {
				if (i != 0 && (s.charAt(i - 1) == '_' || s.charAt(i - 1) == '-')) {
					result.append(Character.toUpperCase(c));
				} else {
					result.append(c);
				}
			}
		}

		return result.toString();
	}

	public static String camelToKebab(String s) {
		StringBuilder result = new StringBuilder(s.length() + 1);

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i > 0 && Character.isUpperCase(s.charAt(i))) {
				result.append('-');
			}

			result.append(Character.toLowerCase(c));

		}

		return result.toString();
	}

	/**
	 * Converts a snake_case or kebab-case string to Upper Case.
	 * <p>
	 * 将 snake_case 或 kebab-case 字符串转换为首字母大写形式。
	 * 例如:"test_string" -> "Test String"
	 * For example: "test_string" -> "Test String"
	 */
	public static String capitalize(String s) {
		StringBuilder result = new StringBuilder(s.length());

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '_' || c == '-') {
				result.append(" ");
			} else if (i == 0 || s.charAt(i - 1) == '_' || s.charAt(i - 1) == '-') {
				result.append(Character.toUpperCase(c));
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	/**
	 * Adds spaces to a camel/pascal case string.
	 * 在 camel/pascal 命名风格的字符串中添加空格。
	 */
	public static String insertSpaces(String s) {
		StringBuilder result = new StringBuilder(s.length() + 1);

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (i > 0 && Character.isUpperCase(c)) {
				result.append(' ');
			}

			result.append(c);
		}

		return result.toString();
	}

	/**
	 * Converts a Space Separated string to camelCase.
	 * <p>
	 * 将空格分隔的字符串转换为 camelCase。
	 * 例如:"Camel Case" -> "camelCase"
	 * For example: "Camel Case" -> "camelCase"
	 */
	public static String camelize(String s) {
		StringBuilder result = new StringBuilder(s.length());

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0) {
				result.append(Character.toLowerCase(c));
			} else if (c != ' ') {
				result.append(c);
			}

		}

		return result.toString();
	}

	public static boolean canParseInt(String s) {
		return parseInt(s) != Integer.MIN_VALUE;
	}

	public static boolean canParsePositiveInt(String s) {
		int p = parseInt(s);
		return p >= 0;
	}

	public static int parseInt(String s, int defaultValue) {
		return parseInt(s, 10, defaultValue);
	}

	public static int parseInt(String s, int radix, int defaultValue) {
		return parseInt(s, radix, defaultValue, 0, s.length());
	}

	public static int parseInt(String s, int radix, int defaultValue, int start, int end) {
		boolean negative = false;
		int i = start, len = end - start, limit = -2147483647;
		if (len <= 0) {
			return defaultValue;
		} else {
			char firstChar = s.charAt(i);
			if (firstChar < '0') {
				if (firstChar == '-') {
					negative = true;
					limit = -2147483648;
				} else if (firstChar != '+') {
					return defaultValue;
				}

				if (len == 1) return defaultValue;

				++i;
			}

			int limitForMaxRadix = (-Integer.MAX_VALUE) / 36;
			int limitBeforeMul = limitForMaxRadix;

			int digit, result = 0;
			while (i < end) {
				digit = Character.digit(s.charAt(i++), radix);
				if (digit < 0) return defaultValue;
				if (result < limitBeforeMul) {
					if (limitBeforeMul == limitForMaxRadix) {
						limitBeforeMul = limit / radix;

						if (result < limitBeforeMul) {
							return defaultValue;
						}
					} else {
						return defaultValue;
					}
				}

				result *= radix;
				if (result < limit + digit) {
					return defaultValue;
				}

				result -= digit;
			}

			return negative ? result : -result;
		}
	}

	public static long parseLong(String s, long defaultValue) {
		return parseLong(s, 10, defaultValue);
	}

	public static long parseLong(String s, int radix, long defaultValue) {
		return parseLong(s, radix, 0, s.length(), defaultValue);
	}

	public static long parseLong(String s, int radix, int start, int end, long defaultValue) {
		boolean negative = false;
		int i = start, len = end - start;
		long limit = -9223372036854775807L;
		if (len <= 0) {
			return defaultValue;
		} else {
			char firstChar = s.charAt(i);
			if (firstChar < '0') {
				if (firstChar == '-') {
					negative = true;
					limit = -9223372036854775808L;
				} else if (firstChar != '+') {
					return defaultValue;
				}

				if (len == 1) return defaultValue;

				++i;
			}

			long multmin = limit / radix;
			long result;
			int digit;
			for (result = 0L; i < end; result -= digit) {
				digit = Character.digit(s.charAt(i++), radix);
				if (digit < 0 || result < multmin) {
					return defaultValue;
				}

				result *= radix;
				if (result < limit + (long) digit) {
					return defaultValue;
				}
			}

			return negative ? result : -result;
		}
	}

	/**
	 * Faster double parser that doesn't throw exceptions.
	 * 不抛出异常的更快 double 解析器。
	 */
	public static double parseDouble(String value, double defaultValue) {
		int len = value.length();
		if (len == 0) return defaultValue;

		int sign = 1;
		int start = 0, end = len;
		char last = value.charAt(len - 1), first = value.charAt(0);
		if (last == 'F' || last == 'f' || last == '.') {
			end--;
		}
		if (first == '+') {
			start = 1;
		}
		if (first == '-') {
			start = 1;
			sign = -1;
		}
		if (start >= end) return defaultValue;

		int dot = -1, e = -1;
		int dotCount = 0, eCount = 0;
		for (int i = start; i < end; i++) {
			char c = value.charAt(i);
			if (c == '.') {
				dot = i;
				dotCount++;
			}
			if (c == 'e' || c == 'E') {
				e = i;
				eCount++;
			}
		}
		if (dotCount > 1 || eCount > 1) return defaultValue;
		if (dot != -1 && e != -1 && dot > e) return defaultValue;

		int mantissaEnd = (e != -1) ? e : end;

		long exponent = 0;
		if (e != -1) {
			if (e + 1 >= end) return defaultValue;
			exponent = parseLong(value, 10, e + 1, end, Long.MIN_VALUE);
			if (exponent == Long.MIN_VALUE) return defaultValue;
		}

		if (dot != -1 && dot < end) {
			//negation as first character
			// 首字符为负号
			long whole = start == dot ? 0 : parseLong(value, 10, start, dot, Long.MIN_VALUE);
			if (whole == Long.MIN_VALUE || whole < 0) return defaultValue;

			int decDigits = mantissaEnd - (dot + 1);
			if (decDigits == 0) {
				return whole * Math.pow(10, exponent) * sign;
			}

			//a long holds 18 decimal digits safely, and a double can't represent more precision than that anyway
			// long 可以安全地保存 18 位十进制数字,而 double 无论如何也无法表示比这更高的精度
			int used = Math.min(decDigits, 18);
			long dec = parseLong(value, 10, dot + 1, dot + 1 + used, Long.MIN_VALUE);
			if (dec < 0) return defaultValue;

			//truncated digits still have to be valid digits
			// 被截断的数字仍必须是有效数字
			for (int i = dot + 1 + used; i < mantissaEnd; i++) {
				char c = value.charAt(i);
				if (c < '0' || c > '9') return defaultValue;
			}

			double pow = Math.pow(10, used);
			long p = (long) pow;
			double mantissa;
			if (whole <= (Long.MAX_VALUE - dec) / p) {
				//fits in a long, keeps the original (more accurate) path
				// 能放进 long,走原来的(更精确)路径
				mantissa = (whole * p + dec) / pow;
			} else {
				mantissa = whole + dec / pow;
			}
			return mantissa * Math.pow(10, exponent) * sign;
		}

		//check scientific notation
		// 检查科学计数法
		if (e != -1) {
			long whole = parseLong(value, 10, start, e, Long.MIN_VALUE);
			if (whole == Long.MIN_VALUE) return defaultValue;
			return whole * Math.pow(10, exponent) * sign;
		}

		//parse as standard integer
		// 按标准整数解析
		long out = parseLong(value, 10, start, end, Long.MIN_VALUE);
		return out == Long.MIN_VALUE ? defaultValue : out * sign;
	}

	/**
	 * Returns Integer.MIN_VALUE if parsing failed.
	 * 解析失败时返回 Integer.MIN_VALUE。
	 */
	public static int parseInt(String s) {
		return parseInt(s, Integer.MIN_VALUE);
	}

	public static boolean canParseFloat(String s) {
		return parseFloat(s, Float.NEGATIVE_INFINITY) != Float.NEGATIVE_INFINITY;
	}

	public static boolean canParsePositiveFloat(String s) {
		return parseFloat(s) >= 0f;
	}

	/**
	 * Returns Float.NEGATIVE_INFINITY if parsing failed.
	 * 解析失败时返回 Float.NEGATIVE_INFINITY。
	 */
	public static float parseFloat(String s) {
		return parseFloat(s, Float.NEGATIVE_INFINITY);
	}

	/**
	 * Faster float parser that doesn't throw exceptions.
	 * 不抛出异常的更快 float 解析器。
	 */
	public static float parseFloat(String value, float defaultValue) {
		return (float) parseDouble(value, defaultValue);
	}

	/**
	 * Returns a new, blank color if parsing failed.
	 * 解析失败时返回一个新的空白颜色。
	 */
	public static Color parseColor(String s) {
		return parseColor(s, new Color());
	}

	public static Color parseColor(String s, Color defaultValue) {
		Color col = Colors.get(s);
		if (col == null) col = parseColorOrNull(new Color(), s);
		if (col == null) return defaultValue;
		return col;
	}

	public static @Nullable Color parseColorOrNull(Color color, String hex) {
		if (hex == null || hex.isEmpty()) return null;

		int offset = hex.charAt(0) == '#' ? 1 : 0;

		int len = hex.length() - offset;
		if (len != 6 && len != 8) return null;

		int r = parseHex(hex, offset, offset + 2);
		int g = parseHex(hex, offset + 2, offset + 4);
		int b = parseHex(hex, offset + 4, offset + 6);
		int a = len != 8 ? 255 : parseHex(hex, offset + 6, offset + 8);

		if (r < 0 || g < 0 || b < 0 || a < 0) return null;

		return color.set(r / 255f, g / 255f, b / 255f, a / 255f);
	}

	private static int parseHex(String string, int from, int to) {
		int total = 0;
		for (int i = from; i < to; i++) {
			char c = string.charAt(i);
			int digit = Character.digit(c, 16);
			if (digit < 0) return -1;
			total += digit * (i == from ? 16 : 1);
		}
		return total;
	}

	public static String autoFixed(float value, int max) {

		//truncate extra digits past the max
		// 截断超出上限的多余数字
		value = (float) Mathf.floor(value * Mathf.pow(10, max) + 0.001f) / Mathf.pow(10, max);

		int precision =
				Math.abs(Mathf.floor(value) - value) < 0.0001f ? 0 :
						Math.abs(Mathf.floor(value * 10) - value * 10) < 0.0001f ? 1 :
								Math.abs(Mathf.floor(value * 100) - value * 100) < 0.0001f ? 2 :
										Math.abs(Mathf.floor(value * 1000) - value * 1000) < 0.0001f ? 3 :
												4;

		return fixed(value, Math.min(max, precision));
	}

	public static String fixed(float d, int decimalPlaces) {
		return fixedBuilder(d, decimalPlaces).toString();
	}

	/** 左补空格到指定宽度（等宽字体下数值对齐用 */
	public static String padLeft(String s, int width) {
		return padLeft(s, ' ', width);
	}

	/** 左补指定字符到指定宽度；超长原样返回 */
	public static String padLeft(String s, char c, int width) {
		if (s.length() >= width) return s;
		tmp2.setLength(0);
		for (int i = s.length(); i < width; i++) {
			tmp2.append(c);
		}
		return tmp1.append(s).toString();
	}

	public static StringBuilder fixedBuilder(float d, int decimalPlaces) {
		if (decimalPlaces < 0 || decimalPlaces > 8) {
			throw new IllegalArgumentException("Unsupported number of " + "decimal places: " + decimalPlaces);
		}
		boolean negative = d < 0;
		d = Math.abs(d);
		StringBuilder dec = tmp2;
		dec.setLength(0);
		dec.append((int) (float) (d * Math.pow(10, decimalPlaces) + 0.0001f));

		int len = dec.length();
		int decimalPosition = len - decimalPlaces;
		StringBuilder result = tmp1;
		result.setLength(0);
		if (negative) result.append('-');
		if (decimalPlaces == 0) {
			if (negative) dec.insert(0, '-');
			return dec;
		} else if (decimalPosition > 0) {
			// Insert a dot in the right place
			// 在正确的位置插入小数点
			result.append(dec, 0, decimalPosition);
			result.append(".");
			result.append(dec, decimalPosition, dec.length());
		} else {
			result.append("0.");
			// Insert leading zeroes into the decimal part
			// 在小数部分插入前导零
			while (decimalPosition++ < 0) {
				result.append("0");
			}
			result.append(dec);
		}
		return result;
	}

	public static String formatMillis(long val) {
		StringBuilder buf = new StringBuilder(20);
		String sgn = "";

		if (val < 0) sgn = "-";
		val = Math.abs(val);

		append(buf, sgn, 0, (val / 3600000));
		val %= 3600000;
		append(buf, ":", 2, (val / 60000));
		val %= 60000;
		append(buf, ":", 2, (val / 1000));
		return buf.toString();
	}

	private static void append(StringBuilder tgt, String pfx, int dgt, long val) {
		tgt.append(pfx);
		if (dgt > 1) {
			int pad = (dgt - 1);
			for (long xa = val; xa > 9 && pad > 0; xa /= 10) pad--;
			for (int xa = 0; xa < pad; xa++) tgt.append('0');
		}
		tgt.append(val);
	}

	/**
	 * Replaces all instances of {@code find} with {@code replace}.
	 * 将所有 {@code find} 替换为 {@code replace}。
	 */
	public static StringBuilder replace(StringBuilder builder, String find, String replace) {
		int findLength = find.length(), replaceLength = replace.length();
		int index = 0;
		while (true) {
			index = builder.indexOf(find, index);
			if (index == -1) break;
			builder.replace(index, index + findLength, replace);
			index += replaceLength;
		}
		return builder;
	}

	/**
	 * Replaces all instances of {@code find} with {@code replace}.
	 * 将所有 {@code find} 替换为 {@code replace}。
	 */
	public static StringBuilder replace(StringBuilder builder, char find, String replace) {
		int replaceLength = replace.length();
		int index = 0;
		while (true) {
			while (true) {
				if (index == builder.length()) return builder;
				if (builder.charAt(index) == find) break;
				index++;
			}
			builder.replace(index, index + 1, replace);
			index += replaceLength;
		}
	}

	private static boolean isDigitsOnly(String part) {
		for (int i = 0; i < part.length(); i++) {
			if (!Character.isDigit(part.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Strips leading/trailing non-numeric, non-dot characters, normalizes a loose version string (e.g. "v1", "2.0", "alpha 2.0.0 release") into an array.
	 * This can handle semver, but is adapted for a maximum of 4 components, since people do that for some reason.
	 * <p>
	 * 去除开头和结尾的非数字、非点字符,将宽松的版本字符串(如 "v1"、"2.0"、"alpha 2.0.0 release")规范化为数组。可以处理 semver,但最多适配 4 个组成部分,因为总有人这么用。
	 *
	 * @return the parsed {major, minor, patch, build} array, or null upon failure. 解析出的 {major, minor, patch, build} 数组,失败时为 null。
	 */
	public static @Nullable int[] sanitizeVersion(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		//strip leading chars until first digit
		// 去除开头字符直到第一个数字
		int start = 0;
		while (start < trimmed.length() && !Character.isDigit(trimmed.charAt(start))) {
			start++;
		}
		//strip trailing chars after last digit
		// 去除最后一个数字之后的结尾字符
		int end = trimmed.length() - 1;
		while (end >= 0 && !Character.isDigit(trimmed.charAt(end))) {
			end--;
		}
		if (start > end) {
			return null; //no digits at all
			// 完全没有数字
		}
		String core = trimmed.substring(start, end + 1);
		String[] parts = core.split("\\.", -1);
		if (parts.length < 1 || parts.length > 4) {
			return null;
		}
		int[] nums = new int[4]; //major, minor, patch, build
		// major、minor、patch、build
		for (int i = 0; i < 4; i++) {
			if (i < parts.length) {
				String part = parts[i];
				if (part.isEmpty() || !isDigitsOnly(part)) {
					return null;
				}
				nums[i] = Strings.parseInt(part);
				if (nums[i] == Integer.MIN_VALUE) return null;
			}
		}
		return nums;
	}

	/** @return true if semver {@param version} > {@param target}. If either parameter is not a valid (or sanitizable) semver string, just returns (version != target). 若 semver {@param version} > {@param target} 则返回 true。若任一参数不是有效(或可规范化)的 semver 字符串,则直接返回 (version != target)。 */
	public static boolean checkNewerSemver(String version, String target) {
		if (version == null || target == null) return false;

		int[] versionNums = sanitizeVersion(version);
		int[] targetNums = sanitizeVersion(target);
		if (versionNums == null || targetNums == null) {
			return !version.equals(target);
		}
		for (int i = 0; i < 4; i++) {
			if (versionNums[i] != targetNums[i]) {
				return versionNums[i] > targetNums[i];
			}
		}
		return false;
	}

	public static byte[] deflate(String str) {
		try {
			Deflater deflater = new Deflater();
			deflater.setInput(str.getBytes(utf8));
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				out.write(buffer, 0, count);
			}
			deflater.end();
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String undeflate(byte[] bytes) {
		try {
			Inflater inflater = new Inflater();
			inflater.setInput(bytes);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				out.write(buffer, 0, count);
			}
			inflater.end();
			return new String(out.toByteArray(), utf8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
