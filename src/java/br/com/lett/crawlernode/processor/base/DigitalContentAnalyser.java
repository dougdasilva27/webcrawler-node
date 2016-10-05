package br.com.lett.crawlernode.processor.base;

import java.awt.image.BufferedImage;

import java.io.File;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.Normalizer;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import org.jsoup.Jsoup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;

import br.com.lett.crawlernode.core.imgprocessing.ImageComparationResult;
import br.com.lett.crawlernode.core.imgprocessing.ImageComparator;
import br.com.lett.crawlernode.core.imgprocessing.ImageFeatures;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.Logging;


/**
 * Classe contém funções de análise de Conteúdo Digital
 * @author fabricio
 *
 */
public class DigitalContentAnalyser {

	private static final Logger logger = LoggerFactory.getLogger(DigitalContentAnalyser.class);

	private static final int 	REFERENCE_SIMILARITY_SIFT_THRESHOLD = 160;
	private static final double SIBLING_SIMILARITY_SIFT_THRESHOLD = 0.30;
	private static final int 	SELF_SIMILARITY_SIFT_THRESHOLD = 400;
	private static final double SELF_SIMILARITY_SIFT_RATE_THRESHOLD = 0.65;

	public static final String RULE_SATISFIED = "satisfied";

	public static final String RULE_TYPE_KEYWORDS_MIN = "keywords_min";
	public static final String RULE_TYPE_KEYWORDS_EXACT = "keywords_exact";
	public static final String RULE_TYPE_KEYWORDS_ALL = "keywords_all";
	public static final String RULE_TYPE_KEYWORDS_NONE = "keywords_none";

	/**
	 * 
	 * @param pm
	 * @return
	 */
	public static int imageCount(ProcessedModel pm) {
		Integer picCount = 0;

		if(pm.getPic() != null && !pm.getPic().isEmpty()) {
			picCount++;
		}

		try {
			JSONArray secondaryPics = new JSONArray(pm.getSecondary_pics());

			return picCount = picCount + secondaryPics.length();
		} catch (Exception e) { 
			return picCount;	
		}

	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public static JSONObject imageDimensions(File image) {

		try {

			BufferedImage bimg = ImageIO.read(image);

			JSONObject pic_dimensions = new JSONObject();
			pic_dimensions.put("width", bimg.getWidth());
			pic_dimensions.put("height", bimg.getHeight());

			return pic_dimensions;

		} catch (Exception e) {
			return null;
		}

	}


	public static JSONObject validateRule(String content, JSONObject rule) {

		JSONObject results = new JSONObject();

		if(content == null) content = "";

		try {

			String sanitizedContent = sanitizeBeforeValidateRule(content);

			Integer condition = null;
			try {
				condition = rule.getInt("condition");
			} catch (Exception e) { 
				// ignored
			}

			// Se a regra é de Keywords, vamos contá-las
			if(rule.getString("type").startsWith("keywords_")) {

				JSONArray desiredKeywords = rule.getJSONArray("value");
				JSONArray keywordsFound = new JSONArray();
				JSONArray keywordsNotFound = new JSONArray();

				// Preparando os objetos para realizar as buscas para buscarmos
				JSONArray desired_keywords_sanitized = new JSONArray();

				for(int i = 0; i < desiredKeywords.length(); i++) {
					desired_keywords_sanitized.put(sanitizeBeforeValidateRule(desiredKeywords.get(i).toString()));
				}

				// Iniciando as buscas
				for(int i = 0; i < desired_keywords_sanitized.length(); i++) {
					if(sanitizedContent.contains(desired_keywords_sanitized.get(i).toString() )) {
						keywordsFound.put(desiredKeywords.get(i).toString());
					} else {
						keywordsNotFound.put(desiredKeywords.get(i).toString());
					}
				}

				// Agora damos o resultado, baseado no tipo da regra de keywords
				if(rule.getString("type").equals(RULE_TYPE_KEYWORDS_MIN)) {
					results.put(RULE_SATISFIED, keywordsFound.length() >= condition);
				} 
				else if(rule.getString("type").equals(RULE_TYPE_KEYWORDS_EXACT)) {
					results.put(RULE_SATISFIED, keywordsFound.length() == condition);
				} 
				else if(rule.getString("type").equals(RULE_TYPE_KEYWORDS_ALL)) {
					results.put(RULE_SATISFIED, keywordsFound.length() == desired_keywords_sanitized.length());
				} 
				else if(rule.getString("type").equals(RULE_TYPE_KEYWORDS_NONE)) {
					results.put(RULE_SATISFIED, keywordsFound.length() == 0);
				}

				results.put("keywords_found", keywordsFound);
				results.put("keywords_not_found", keywordsNotFound);

			} else if(rule.getString("type").startsWith("words_")) {

				results.put(RULE_SATISFIED, sanitizedContent.trim().split(" ").length >= condition);
				results.put("words_count", sanitizedContent.trim().split(" ").length);

			}

			return results;

		} catch (Exception e) {

			Logging.printLogError(logger, "An error occurred when computing a description rule: ");
			Logging.printLogError(logger, rule.toString());			

			return results;
		}

	}

	private static String sanitizeBeforeValidateRule(String content) {

		String sanitizedContent = content;

		// Essas palavras ou caracteres devem ser substituídas em qualquer situação, não apenas na lista negra
		sanitizedContent = sanitizedContent.replace("'", "");
		sanitizedContent = sanitizedContent.replace("`", "");
		sanitizedContent = sanitizedContent.replace("+", " ");
		sanitizedContent = sanitizedContent.replace(",", " ");
		sanitizedContent = sanitizedContent.replace(".", " ");
		sanitizedContent = sanitizedContent.replace("!", " ");
		sanitizedContent = sanitizedContent.replace("?", " ");
		sanitizedContent = sanitizedContent.replaceAll("\\u00a0"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u2007"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u202F"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u3000"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u1680"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u180e"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u200a"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\xA0"," ");
		sanitizedContent = sanitizedContent.replaceAll("\\u205f"," ");

		// Básico - Remover caixa alta e espaços vazios no começo e fim
		sanitizedContent = sanitizedContent.toLowerCase();
		sanitizedContent = sanitizedContent.trim();

		// Remoção de espaços duplos
		while (sanitizedContent.contains("  ")) {
			sanitizedContent = sanitizedContent.replace("  ", " ");
		}

		sanitizedContent =  " " + Jsoup.parse(Normalizer.normalize(sanitizedContent, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")).text() + " ";

		sanitizedContent = sanitizedContent.replace("/", " ");

		return sanitizedContent;
	}

	/**
	 * Classify an image analysing the result of each similarity stage.
	 * 
	 * Classification pipeline
	 * Self Comparation --> Sibling Comparation --> Reference Comparation --> Resultado final
	 * 
	 * @param similaritySiftResult
	 * @return
	 */
	public static JSONObject classifyImage(JSONObject similaritySiftResult) {

		JSONObject finalResult = null;

		// Reference
		JSONObject analysis1 = similaritySiftResult.getJSONObject("analysis1");

		// stage1 - Sibling, stage2 - Self
		JSONObject analysis2 = similaritySiftResult.getJSONObject("analysis2");
		JSONObject stage1 = analysis2.getJSONObject("stage1");
		JSONObject stage2 = analysis2.getJSONObject("stage2");

		if( stage2.has("passed") && stage2.getBoolean("passed") ) {
			finalResult = new JSONObject();
			finalResult.put("passed", true);
			finalResult.put("pipeline_comparation_type_passed", stage2.getString("comparation_type"));
			finalResult.put("would_set_status", stage2.getString("would_set_status"));

		} else if( stage1.has("passed") && stage1.getBoolean("passed") ) {

			finalResult = new JSONObject();
			finalResult.put("passed", true);
			finalResult.put("pipeline_comparation_type_passed", stage1.getString("comparation_type"));
			finalResult.put("would_set_status", stage1.getString("would_set_status"));

		} else if( analysis1.has("passed") && analysis1.getBoolean("passed") ) {

			finalResult = new JSONObject();
			finalResult.put("passed", true);
			finalResult.put("pipeline_comparation_type_passed", analysis1.getString("comparation_type"));
			finalResult.put("would_set_status", analysis1.getString("would_set_status"));

		} else { // não passou em nada

			finalResult = new JSONObject();
			finalResult.put("passed", false);
			finalResult.put("pipeline_comparation_type_passed", "none");
			finalResult.put("would_set_status", "none");
		}

		return finalResult;
	}

	public static JSONObject similaritySIFT(MongoDatabase mongo, DatabaseManager db, String currentMd5, Long lettId, String referenceMd5) {
		JSONObject similaritySift = new JSONObject();

		JSONObject analysis1 = DigitalContentAnalyser.compareReference(mongo, currentMd5, referenceMd5);
		JSONObject analysis2 = DigitalContentAnalyser.compareSiblings(mongo, db, lettId, currentMd5);

		similaritySift.put("analysis1", analysis1);
		similaritySift.put("analysis2", analysis2);

		// Rodar o pipeline de classificação
		JSONObject classification = classifyImage(similaritySift);

		similaritySift.put("veredict", classification);

		return similaritySift;
	}

	public static JSONObject compareReference(MongoDatabase mongo, String md5, String referenceMd5) {
		JSONObject referenceComparation = new JSONObject();

		ImageComparationResult imageComparationResult = compareSIFT(mongo, md5, referenceMd5);

		referenceComparation.put("comparation_type", "Reference");
		referenceComparation.put("threshold_used", REFERENCE_SIMILARITY_SIFT_THRESHOLD);
		if(imageComparationResult.getNumberOfMatches() >= REFERENCE_SIMILARITY_SIFT_THRESHOLD) {
			referenceComparation.put("passed", true);
			referenceComparation.put("matches", imageComparationResult.getNumberOfMatches());
			referenceComparation.put("would_set_status_to", "match");
		} else {
			referenceComparation.put("passed", false);
			referenceComparation.put("matches", imageComparationResult.getNumberOfMatches());
			referenceComparation.put("would_set_status_to", "not-verified");
		}

		return referenceComparation;
	}

	/**
	 * Comparar a nova imagem do produto com os irmãos dele. Os irmãos são os produtos que possuem o mesmo lettId.
	 * @param mongoBackendPanel Instância do banco do mongo para buscar buscar as features a partir do md5 da imagem
	 * @param dbm Instância do DBManager para buscar o md5 de cada imagem
	 * @param lettId lettId do produto atual, usado para buscar os irmãos no Postgres
	 * @param md5 md5 da imagem que acabou de ser baixada
	 * @return
	 */
	public static JSONObject compareSiblings(MongoDatabase mongo, DatabaseManager db, Long lettId, String md5) {
		JSONObject siblingsComparations = new JSONObject();
		JSONObject selfComparation = new JSONObject();
		JSONObject result = new JSONObject();
		JSONObject analysis2 = new JSONObject();
		double chosenNumberOfMathces = 0;
		String chosenStatus = null;

		// procurar os irmãos deste processed (os produtos que possuem o mesmo let_id)
		// será feita uma comparação de imagens com todos os irmãos dele
		try {
			String sqlConsult = "SELECT digital_content, id FROM processed WHERE digital_content IS NOT NULL AND lett_id IS NOT NULL AND lett_id = " + lettId;
			ResultSet resultSet = db.runSqlConsult(sqlConsult);

			// para cada produto com o mesmo lett_id do processed atual (inclusive ele próprio)
			while(resultSet.next()) {

				// Pegar o digital content do produto
				JSONObject digitalContent = new JSONObject(resultSet.getString("digital_content"));

				// Pegar o pic
				JSONObject picObject = digitalContent.getJSONObject("pic");

				// Caso tenha o pic, olhar o status do irmão para ver se é ou não é not-verified
				if(picObject != null) {
					JSONObject primary = picObject.getJSONObject("primary");

					if(primary != null) {
						if( primary.has("status") && !primary.getString("status").equals("not-verified") ) { // Se o status for diferente de not-verified, então eu comparo a imagem

							// Pegar o md5
							String siblingMd5 = null;
							if(primary.has("md5")) siblingMd5 = primary.getString("md5");									

							if( siblingMd5 != null &&  !siblingMd5.equals(md5) ) { //excluir o processed atual, porque já olhei a similaridade com ele
								ImageComparationResult imageComparationResult = DigitalContentAnalyser.compareSIFT(mongo, md5, siblingMd5);

								double candidate = 0;
								if (imageComparationResult.getTotalNumberOfMatches() > 0) candidate = imageComparationResult.getRate();

								// Colocar as informações do resultado da análise com este irmão
								JSONObject siblingComparationResult = new JSONObject();
								siblingComparationResult.put("id", resultSet.getString("id"));
								siblingComparationResult.put("matches", imageComparationResult.getNumberOfMatches());
								siblingComparationResult.put("total", imageComparationResult.getTotalNumberOfMatches());
								siblingComparationResult.put("rate", candidate);
								siblingsComparations.append("comparation_result", siblingComparationResult);

								if(candidate > chosenNumberOfMathces) {
									chosenNumberOfMathces = candidate;
									chosenStatus = primary.getString("status");
								}
							}

							// comparar a imagem que eu acabai de baixar do produto com a imagem do mesmo produto que está no banco,
							// que seria uma versão antiga da imagem. Com isso vamos impor um limiar um pouco maior para ver se é a mesma
							// imagem, porém com pouca modificação, o que pode ser uma pequena mudança de resolução, por exemplo.
							else if( siblingMd5 != null ) {
								ImageComparationResult imageComparationResult = DigitalContentAnalyser.compareSIFT(mongo, md5, siblingMd5);
								selfComparation.put("comparation_type", "Self");
								selfComparation.put("threshold_used", SELF_SIMILARITY_SIFT_RATE_THRESHOLD);
								if(imageComparationResult.getRate() >= SELF_SIMILARITY_SIFT_RATE_THRESHOLD) {
									selfComparation.put("passed", true);
									selfComparation.put("matches", imageComparationResult.getNumberOfMatches());
									selfComparation.put("total", imageComparationResult.getTotalNumberOfMatches());
									selfComparation.put("rate", imageComparationResult.getRate());
									selfComparation.put("would_set_status", primary.getString("status"));
									selfComparation.put("sugestion", "A imagem nao precisa ser reavaliada");
								} else {
									selfComparation.put("passed", false);
									selfComparation.put("matches", imageComparationResult.getNumberOfMatches());
									selfComparation.put("total", imageComparationResult.getTotalNumberOfMatches());
									selfComparation.put("rate", imageComparationResult.getRate());
									selfComparation.put("would_set_status", "not-verified");
									selfComparation.put("sugestion", "A imagem nao precisa ser reavaliada");
								}

							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		result.put("comparation_type", "Sibling");
		result.put("threshold_used", SIBLING_SIMILARITY_SIFT_THRESHOLD);
		if(chosenNumberOfMathces >= SIBLING_SIMILARITY_SIFT_THRESHOLD) {
			result.put("passed", true);
		} else {
			result.put("passed", false);
		}

		JSONObject bestSiblingComparation = new JSONObject();
		bestSiblingComparation.put("matches", chosenNumberOfMathces);

		result.put("best_sibling_comparation_result", bestSiblingComparation);
		result.put("all_siblings_comparations", siblingsComparations);
		result.put("would_set_status", chosenStatus);

		analysis2.put("stage1", result);
		analysis2.put("stage2", selfComparation);

		return analysis2;
	}

	/**
	 * Faz a comparação entre duas imagens, fazendo um matching de keypoints com descritores SIFT.
	 * @param mongoBackendPanel
	 * @param md5
	 * @param md5Desired
	 * @return Classe de resultado contendo dados de resultado da comparação
	 */
	public static ImageComparationResult compareSIFT(MongoDatabase mongo, String md5, String md5Desired) {
		ImageComparator imageComparator = new ImageComparator();
		ImageComparationResult result = null;

		if(md5 != null && md5Desired != null) {

			// Buscar no Mongo a imagem cujo md5 é igual ao passado de parâmetro
			ImageFeatures imageFeatures = DigitalContentAnalyserUtils.searchImageFeatures(mongo, md5);
			ImageFeatures imageFeaturesDesired = DigitalContentAnalyserUtils.searchImageFeatures(mongo, md5Desired);

			if(imageFeatures != null && imageFeaturesDesired != null) {

				/*
				 * Setar as features target no comparador. Poderia modificar o método
				 * para receber direto as duas imagens de parâmetro, porém de início
				 * foi feito de forma a setar a imagem target, pois rodávamos o comparador
				 * em várias pastas de produtos diferentes, comparando várias imagens com
				 * uma única imagem target. Então para evitar extrair descritores da imagem
				 * target a cada rodada, foi arquitetado dessa forma.
				 */
				imageComparator.setReference(imageFeatures);

				// Rodar a análise
				result = imageComparator.compareWithReference(imageFeaturesDesired);
			}

		}

		if(result == null) {
			result = new ImageComparationResult();
			result.setNumberOfMatches(0);
		}

		return result;
	}	

}
