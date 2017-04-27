package br.com.lett.crawlernode.processor.controller;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;
import br.com.lett.crawlernode.processor.base.Extractor;
import br.com.lett.crawlernode.processor.base.IdentificationLists;
import br.com.lett.crawlernode.processor.base.Queries;
import br.com.lett.crawlernode.processor.base.ReplacementMaps;
import br.com.lett.crawlernode.processor.digitalcontent.Pic;
import br.com.lett.crawlernode.processor.extractors.ExtractorFlorianopolisAngeloni;
import br.com.lett.crawlernode.processor.models.BrandModel;
import br.com.lett.crawlernode.processor.models.ClassModel;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;


/**
 * Classe responsável por processar produtos da tabela crawler a fim de transformá-los em
 * produtos legíves para serem encaminhados para a tabela processed
 * 
 * @author Doug
 */
public class ResultManager {

	private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

	private DatabaseManager db;

	// substitution maps
	private Map<String, String> unitsReplaceMap;
	private Map<String, String> recipientsReplaceMap;

	// identification lists
	private List<String> recipientsList;
	private List<String> unitsList;
	private List<ClassModel> classModelList;
	private List<BrandModel> brandModelList;

	// log messages on/off
	private boolean logActivated;

	private DateFormat isoDateFormat;
	private Map<Integer, String> cityNameInfo;
	private Map<Integer, String> marketNameInfo;

	private ArrayList<Integer> marketid;

	/**
	 * Called on crawler main method.
	 * 
	 * @param activateLogging
	 * @param mongo
	 * @param db
	 * @throws NullPointerException
	 */
	public ResultManager(
			boolean activateLogging, 
			DatabaseManager db
			) throws NullPointerException {

		this.db = db;

		this.init(activateLogging);
	}

	/**
	 * Result manager initialization.
	 * 
	 * @param activateLogging
	 * @throws NullPointerException
	 */
	private void init(boolean activateLogging) throws NullPointerException {
		this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		this.logActivated = activateLogging;

		// create market information
		this.createMarketInfo();

		// initialize substitution maps
		this.unitsReplaceMap = new LinkedHashMap<>();
		this.recipientsReplaceMap = new LinkedHashMap<>();

		// initialize identification maps
		this.recipientsList = new ArrayList<>();
		this.unitsList = new ArrayList<>();
		this.classModelList = new ArrayList<>();
		this.brandModelList = new ArrayList<>();

		// create model for brand manipulation for identification and substitution
		try {
			ResultSet rs = db.connectionPostgreSQL.runSqlConsult(Queries.queryForLettBrandProducts);
			while(rs.next()) {

				// verify if the brand should be ignored
				// e.g: papel higiênico leve 3 pague 2 PAMPERS
				// LEVE is also a brand, but we want PAMPERS
				// so LEVE is not included on our brand list
				if(!rs.getBoolean("ignored")) {		
					BrandModel aux = null;

					// see if this brand already exists on the brand list
					for(BrandModel bm: brandModelList) {
						if(bm.getBrand().equals(rs.getString("denomination"))) {
							aux = bm;
							break;
						}
					}

					// if it doesn't exists yet, we create a new Brand Model and add it on the brandsModelList
					if(aux == null) {
						aux = new BrandModel(rs.getString("denomination"), rs.getString("lett_supplier"));
						aux.putOnList(rs.getString("mistake"));
						brandModelList.add(aux);
					}

					// if an occurrence exists, only add this error on the map
					else {
						aux.putOnList(rs.getString("mistake"));
					}
				}
			}

			// close the result set
			rs.close();

		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// completion or correction of brands through brands list
		for(BrandModel bm : brandModelList) {

			// automation 1: brands is no blank spaces
			// eg: "jack daniels"
			//
			// automatically add with brandsReplaceMap and its combinations:
			// jackdaniels (wrong) -> jack daniels (correct)
			if(bm.getBrand().contains(" ")) {
				bm.putOnList(bm.getBrand().replace(" ", ""));

				Pattern space = Pattern.compile(" ");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}

			// automation 2: brands that contains hifen (-).
			// e.g: "arco-íris"
			//
			// automatically add with brandsReplaceMap and its combinations:
			// "arco íris" (wrong) -> "arco-íris" (correct)
			// "arcoíris" (wrong)  -> "arco-íris" (correct)
			if(bm.getBrand().contains("-")) {
				bm.putOnList(bm.getBrand().replace("-", ""));
				bm.putOnList(bm.getBrand().replace("-", " "));

				Pattern space = Pattern.compile("-");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + " " + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}
		}

		// create unit identification lists
		this.unitsList = IdentificationLists.unitsList;

		// create unit substitution lists
		this.unitsReplaceMap = ReplacementMaps.unitsReplaceMap;

		// create recipient identification lists
		this.recipientsList = IdentificationLists.recipientsList;

		// create recipient substitution lists
		this.recipientsReplaceMap = ReplacementMaps.recipientsReplaceMap;

		// create manipulation models for lett classes
		try{
			ResultSet rs = this.db.connectionPostgreSQL.runSqlConsult(Queries.queryForLettClassProducts);
			while (rs.next()) {

				ClassModel aux = null;

				// see if it already exists on class list
				for(ClassModel lcm: classModelList) {
					if(lcm.getLettName().equals(rs.getString("denomination"))) {
						aux = lcm;
						break;
					}
				}

				// if it not exists, creates a new class model and add it to the list
				if (aux == null) {
					aux = new ClassModel(rs.getString("denomination"));
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
					classModelList.add(aux);
				} 

				// if an occurrence exists, only add this error on the map
				else {
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
				}
			}

			// close the result set
			rs.close();

		}	catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// complete or apply correction upon classes
		for (ClassModel cm : classModelList){
			Pattern space = Pattern.compile(" ");

			// automation 1: brands is no blank spaces
			// eg: "jack daniels"
			//
			// automatically add with brandsReplaceMap and its combinations:
			// jackdaniels (wrong) -> jack daniels (correct)
			if (cm.getLettName().contains(" ")) {
				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", ""))) {
					cm.putOnMap(cm.getLettName().replace(" ", ""), "");
				}
				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", "-"))) {
					cm.putOnMap(cm.getLettName().replace(" ", "-"), "");
				}

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}

			// automation 2: brands that contains hifen (-).
			// e.g: "arco-íris"
			//
			// automatically add with brandsReplaceMap and its combinations:
			// "arco íris" (wrong) -> "arco-íris" (correct)
			// "arcoíris" (wrong)  -> "arco-íris" (correct)
			if (cm.getLettName().contains("-")) {
				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", ""))) {
					cm.putOnMap(cm.getLettName().replace("-", ""), "");
				}
				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", " "))) {
					cm.putOnMap(cm.getLettName().replace("-", " "), "");
				}

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}
		}
		Logging.printLogDebug(logger, "success!");

	}

	/**
	 * Extract informations from all originals fields on ProcessedModel and then
	 * saves the data gattered from the ProcessedModel to be returned.
	 * 
	 * @author Fabricio
	 * @param cm Recebe valores do Crawler e os transfere para o ProcessModel
	 * @return pm Retorna processModel com valores do Crawler 
	 */
	public ProcessedModel processProduct(ProcessedModel pm, Session session) {	
		Logging.printLogDebug(logger, session, "Processing product in ResultManager...");

		// preventing extra field to be null
		if (pm.getExtra() == null) {
			pm.setExtra("");
		}

		Extractor extractor = new ExtractorFlorianopolisAngeloni();
		extractor.setAttrs(logActivated, 
				brandModelList,
				unitsReplaceMap, 
				recipientsReplaceMap,
				recipientsList,
				unitsList,
				classModelList);

		// extract processed model fields
		pm = extractor.extract(pm);

		// update digital content
		updateDigitalContent(pm, session);

		if (logActivated) {
			Logging.printLogDebug(logger, "\n---> Final result:");
		}

		if (logActivated) {
			Logging.printLogDebug(logger, pm.toString());
		}

		return pm;
	}

	/**
	 * Evaluates the processed product main and secondary images.
	 * Main image evaluation algorithm:
	 * <br>Fetch the md5 of the most recent downloaded main image by the image crawler
	 * <br>If the md5 is null, we set the image status to no_image
	 * <br>If the md5 exists, we get the previous verified md5 from the digital content and compare to the new one
	 * <br>If they are equals, there is nothing to be done
	 * <br>If they are different, we update the md5 field of the digital content and set the image status to no_image
	 * 
	 * @param pm
	 * @param session
	 */
	private void updateDigitalContent(ProcessedModel pm, Session session) {  
		Logging.printLogDebug(logger, session, "Updating digital content...");

		// if the processed model doesn't have a digital content
		// we must create an empty one, to be populated
		if(pm.getDigitalContent() == null) { 
			pm.setDigitalContent(new JSONObject()); 
		}

		// get reference digital content
		JSONObject lettDigitalContent = fetchReferenceDigitalContent(pm.getLettId(), session);

		// analysing images
		// count pics
		// evaluate primary image
		// evaluate secondary images
		JSONObject processedModelDigitalContentPic = new JSONObject();
		try {
			JSONObject processedModelDigitalContent = pm.getDigitalContent();
			if (processedModelDigitalContent != null && processedModelDigitalContent.has("pic")) {
				processedModelDigitalContentPic = pm.getDigitalContent().getJSONObject("pic");
			}
		} catch (Exception e) { 
			Logging.printLogDebug(logger, session, CommonMethods.getStackTraceString(e));
		}

		// count images
		processedModelDigitalContentPic.put("count", DigitalContentAnalyser.imageCount(pm));

		// evaluate primary image
		JSONObject processedModelDigitalContentPicPrimary;
		if ( processedModelDigitalContentPic.has("primary") ) {
			processedModelDigitalContentPicPrimary = processedModelDigitalContentPic.getJSONObject("primary");
		} else {
			processedModelDigitalContentPicPrimary = new JSONObject();
		}

		// assembling path to primary image stored on Amazon S3
		// this image is the last downloaded image in the image crawler
		String primaryImageAmazonKey = new StringBuilder()
											.append("market/")
											.append("product-image/")
											.append(pm.getId())
											.append("/1.jpg")
											.toString();

		// fetch md5 of the image
		String primaryMd5 = S3Service.fetchImageMd5(session, primaryImageAmazonKey); // the supposed new image

		Logging.printLogDebug(logger, session, "Last downloaded primary image md5: " + primaryMd5);

		String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

		if (primaryMd5 == null) { // if md5 is null, clear and add set as no_image
			Logging.printLogDebug(logger, session, "Amazon md5 of the last downloaded image is null...seting status to no_image...");
			processedModelDigitalContentPicPrimary = new JSONObject();
			processedModelDigitalContentPicPrimary.put("status", Pic.NO_IMAGE);
			processedModelDigitalContentPicPrimary.put("verified_by", "crawler_" + nowISO);

		} else { // see if the md5 has changed comparing to the last verified md5
			String previousMd5 = processedModelDigitalContentPicPrimary.has("md5") ? processedModelDigitalContentPicPrimary.getString("md5") : null;

			Logging.printLogDebug(logger, session, "Previous verified md5: " + previousMd5);

			if ( !primaryMd5.equals(previousMd5) ) {
				Logging.printLogDebug(logger, session, "Previous md5 is different from the new one...updating and seting as not_verified...");

				File primaryImage = S3Service.fetchImageFromAmazon(session, primaryImageAmazonKey);

				// get dimensions from image
				processedModelDigitalContentPicPrimary.put("dimensions", DigitalContentAnalyser.imageDimensions(primaryImage));

				// old similarity value
				// was calculate using the naive similarity finder
				// set to 0 because this algorithm was removed
				processedModelDigitalContentPicPrimary.put("similarity", 0);

				// TODO compute similarity of the new image using the SIFT algorithm
				processedModelDigitalContentPicPrimary.put("similarity_sift", new JSONObject());

				// setting fields of the new primary image
				processedModelDigitalContentPicPrimary.put("md5", primaryMd5); // updated md5
				processedModelDigitalContentPicPrimary.put("status", Pic.NOT_VERIFIED);
				processedModelDigitalContentPicPrimary.put("verified_by", "crawler_" + nowISO);

				// delete local images
				if(primaryImage != null) {
					primaryImage.delete();
				}
			} else {
				Logging.printLogDebug(logger, session, "New image md5 is the same as the previous verified one. Nothing to be done.");
			}
		}

		processedModelDigitalContentPic.put("primary", processedModelDigitalContentPicPrimary);

		// computing pic secondary
		Pic.setPicSecondary(lettDigitalContent, processedModelDigitalContentPic);

		// set pic on digital content
		pm.getDigitalContent().put("pic", processedModelDigitalContentPic);
	}

	/**
	 * Fetch market informations.
	 */
	private void createMarketInfo() {
		this.cityNameInfo = new HashMap<>();
		this.marketNameInfo = new HashMap<>();
		this.marketid = new ArrayList<>();

		try {

			ResultSet rs = this.db.connectionPostgreSQL.runSqlConsult("SELECT * FROM market");

			while(rs.next()) {
				cityNameInfo.put(rs.getInt("id"), rs.getString("city"));
				marketNameInfo.put(rs.getInt("id"), rs.getString("name"));
				marketid.add(rs.getInt("id"));
			}
		} catch (SQLException e) {
			Logging.printLogError(logger, "Error fetching market info on postgres!");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * Fetch the digital content from lett table.
	 * 
	 * @param lettId
	 * @param session
	 * @return the json object containing the reference digital content or an empty json object
	 */
	private JSONObject fetchReferenceDigitalContent(Long lettId, Session session) {
		try {
			ResultSet rs = this.db.connectionPostgreSQL.runSqlConsult("SELECT digital_content FROM lett WHERE id = " + lettId);
			while(rs.next()) {
				String referenceDigitalContent = rs.getString("digital_content");
				if (referenceDigitalContent != null && !referenceDigitalContent.isEmpty()) {
					return new JSONObject(rs.getString("digital_content"));
				}
			}

		} catch (Exception e) { 
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}

		return new JSONObject();
	}


	public List<Integer> getMarketid() {
		return this.marketid;
	}

	public Map<Integer, String> getCityNameInfo() {
		return this.cityNameInfo;
	}

	public Map<Integer, String> getMarketNameInfo() {
		return this.marketNameInfo;
	}

}
