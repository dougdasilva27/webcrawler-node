package br.com.lett.crawlernode.processor;

import org.jsoup.Jsoup;

public class DigitalContentAnalyserUtils {

	// (^|\s|['`+,.!?()])
	// the ^ in this case means start of string
	// the $ means end of string
	// \s is blank spaces
	private static final String DESCRIPTION_KEYWORD_REGEX_START_STRING_GROUP = "(^|[\\s'`+;:,.!?()])"; // start string
	private static final String DESCRIPTION_KEYWORD_REGEX_END_STRING_GROUP = "($|[\\s'`+;:,.!?()])"; // end string

	private static final String DESCRIPTION_KEYWORD_REGEX_BOUNDARY_GROUP = "(\\b)";

	private static final String DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACE_ALL_GROUP = "([-\\[\\]{}()*+?.,\\^$|#])";
	private static final String DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACEMENT = "\\\\$1";

	private DigitalContentAnalyserUtils() {
		super();
	}

	/**
	 * Sanitizes the original description text content.
	 * Besides the unsupported characters deletions we also
	 * convert all the content to lower case.
	 * 
	 * @param originalDescription
	 * @return the final sanitized content
	 */
	public static String sanitizeOriginalDescription(String originalDescription) {
		String sanitizedContent = originalDescription;

		sanitizedContent = sanitizedContent
				.replaceAll("\\u00a0", " ")
				.replaceAll("\\u2007", " ")
				.replaceAll("\\u202F", " ")
				.replaceAll("\\u3000", " ")
				.replaceAll("\\u1680", " ")
				.replaceAll("\\u180e", " ")
				.replaceAll("\\u200a", " ")
				.replaceAll("\\xA0", " ")
				.replaceAll("\\u205f", " ");	

		// create replacement for double quotes
		sanitizedContent = sanitizedContent
				.replaceAll("\\u201d", "\"") 	// ”
				.replaceAll("\\u201e", "\"") 	// „
				.replaceAll("\\u201c", "\""); 	// “

		sanitizedContent =  Jsoup.parse(sanitizedContent).text();

		return sanitizedContent;
	}

	/**
	 * Creates a regular expression to match inside a description text.
	 * We always must escape the input keyword String to include it in the
	 * final regular expression.
	 * Before the actual escaping, we always trim the string and convert it to lower case.
	 * 
	 * replace pattern: ([-\\[\\]{}()*+?.,\\^$|#])
	 * replacement: \\\\$1
	 * 
	 * The $1 is interpreted by the parser as being the found pattern itself. For example,
	 * For example, in the string "fim de frase.", the '.' character will be replaced by
	 * "\.";
	 * 
	 * To be completely case insensitive, without having to transform the content to lower case
	 * we also perform a replacement creating a class of characters in the cases were we can have
	 * accents.
	 * e.g: samir -> s[aàáâãäå]m[iìíîï]r
	 * The above must go only into the regular expression. And must only be APPLIED AFTER the escaping replacement.
	 * 
	 * @param keyword
	 * @return the regular expression String
	 */
	public static String createDescriptionKeywordRegex(String keyword) {
		String escapedKeyword = keyword
				.trim()
				.toLowerCase()
				.replaceAll(DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACE_ALL_GROUP, DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACEMENT)
				.replaceAll("[aàáâãäå]", "[aàáâãäå]")
				.replaceAll("[eèéêë]", "[eèéêë]")
				.replaceAll("[iìíîï]", "[iìíîï]")
				.replaceAll("[oòóôõö]", "[oòóôõö]")
				.replaceAll("[uùúûü]", "[uùúûü]")
				.replaceAll("[cCçÇ]", "[cCçÇ]");
				
		return 
				DESCRIPTION_KEYWORD_REGEX_BOUNDARY_GROUP + 
				"(" + escapedKeyword + ")" + 
				DESCRIPTION_KEYWORD_REGEX_BOUNDARY_GROUP;
	}

}
