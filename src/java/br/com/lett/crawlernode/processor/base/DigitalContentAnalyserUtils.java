package br.com.lett.crawlernode.processor.base;

import java.util.ArrayList;

import org.bson.Document;
import org.jsoup.Jsoup;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.feature.local.keypoints.Keypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import br.com.lett.crawlernode.core.imgprocessing.ImageFeatures;
import br.com.lett.crawlernode.util.Logging;

public class DigitalContentAnalyserUtils {

	private static final Logger logger = LoggerFactory.getLogger(DigitalContentAnalyserUtils.class);

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
	 * Busca no banco do Mongo a imagem cujo md5 é igual ao passado de parâmetro
	 * @param mongoBackendPanel
	 * @param md5
	 * @return
	 */
	public static ImageFeatures searchImageFeatures(MongoDatabase mongo, String md5) {
		MongoCollection<Document> imageFeaturesCollection =  mongo.getCollection("ImageFeatures");
		FindIterable<Document> iterable = imageFeaturesCollection.find(Filters.eq("md5", md5));
		Document document = iterable.first();

		if(document == null) {
			return null;
		} else {

		}

		try {

			Gson gson = new Gson();

			ArrayList<Document> featuresDocuments = (ArrayList<Document>) document.get("features");

			LocalFeatureList<Keypoint> features = new MemoryLocalFeatureList<>();

			for(Document d: featuresDocuments) {
				features.add(gson.fromJson(d.toJson(), Keypoint.class));
			}

			ImageFeatures imageFeatures = new ImageFeatures(features, document.getString("md5"));

			return imageFeatures;
		} catch (Exception e) {
			Logging.printLogError(logger, "Error searching image feature on Mongo.");
			Logging.printLogError(logger, e.getMessage());
			return null;
		}

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

		sanitizedContent =  Jsoup.parse(sanitizedContent).text().toLowerCase();

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
				.replaceAll(DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACE_ALL_GROUP, DESCRIPTION_KEYWORD_ESCAPING_REGEX_REPLACEMENT);
				//.replaceAll("[aàáâãäå]", "[aàáâãäå]")
				//.replaceAll("[eèéêë]", "[eèéêë]")
				//.replaceAll("[iìíîï]", "[iìíîï]")
				//.replaceAll("[oòóôõö]", "[oòóôõö]")
				//.replaceAll("[uùúûü]", "[uùúûü]")
				//.replaceAll("[cCçÇ]", "[cCçÇ]");
				
		return 
				DESCRIPTION_KEYWORD_REGEX_BOUNDARY_GROUP + 
				"(" + escapedKeyword + ")" + 
				DESCRIPTION_KEYWORD_REGEX_BOUNDARY_GROUP;
	}

}
