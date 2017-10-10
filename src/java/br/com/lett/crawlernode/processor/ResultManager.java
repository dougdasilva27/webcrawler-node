package br.com.lett.crawlernode.processor;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;

import models.Processed;


public class ResultManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResultManager.class);

	private DatabaseManager db;

	// log messages on/off
	private boolean logActivated;

	private DateFormat isoDateFormat;
	private Map<Integer, String> cityNameInfo;
	private Map<Integer, String> marketNameInfo;
	private List<Integer> marketid;

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

		logActivated = activateLogging;
		createMarketInfo();
	}

	/**
	 * Extract informations from all originals fields on ProcessedModel and then
	 * saves the data gattered from the ProcessedModel to be returned.
	 * 
	 * @author Fabricio
	 * @param cm Recebe valores do Crawler e os transfere para o ProcessModel
	 * @return pm Retorna processModel com valores do Crawler 
	 */
	public Processed processProduct(Processed pm, Session session) {	
		Logging.printLogDebug(LOGGER, session, "Processing product in ResultManager...");

		// preventing extra field to be null
		if (pm.getExtra() == null) {
			pm.setExtra("");
		}

		// update digital content
		updateDigitalContent(pm, session);

		if (logActivated) {
			Logging.printLogDebug(LOGGER, "\n---> Final result:");
		}

		if (logActivated) {
			Logging.printLogDebug(LOGGER, pm.toString());
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
	private void updateDigitalContent(Processed pm, Session session) {  
		Logging.printLogDebug(LOGGER, session, "Updating digital content...");

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
			Logging.printLogDebug(LOGGER, session, CommonMethods.getStackTraceString(e));
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
										.append("market")
										.append("/")
										.append("product-image")
										.append("/")
										.append(pm.getId())
										.append("/")
										.append(1)
										.append(".jpg")
										.toString();

		// fetch md5 of the image
		String primaryMd5 = S3Service.fetchOriginalImageMd5(session, primaryImageAmazonKey); // the supposed new image

		Logging.printLogDebug(LOGGER, session, "Last downloaded primary image md5: " + primaryMd5);

		String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");
		
		if ( primaryMd5 == null ) { // if md5 is null, means that there is no image in Amazon, let's see the previous status
			Logging.printLogDebug(LOGGER, session, "Amazon md5 of the last downloaded image is null");
			
			String previousStatus = processedModelDigitalContentPicPrimary.has("status") ? processedModelDigitalContentPicPrimary.getString("status") : null;
			
			Logging.printLogDebug(LOGGER, session, "Previous image status is " + previousStatus);
			
			if ( !Pic.NO_IMAGE.equals(previousStatus) ) { // if the previous verified status is different from no-image, clear and set as not-verified
				Logging.printLogDebug(LOGGER, session, "Previous image status is different from " + Pic.NO_IMAGE + "...let's clear and set to not-verified");
				
				processedModelDigitalContentPicPrimary = new JSONObject();
				processedModelDigitalContentPicPrimary.put("status", Pic.NOT_VERIFIED);
				processedModelDigitalContentPicPrimary.put("verified_by", "crawler_" + nowISO);
			}

		} else { // see if the md5 has changed comparing to the last verified md5
			String previousMd5 = processedModelDigitalContentPicPrimary.has("md5") ? processedModelDigitalContentPicPrimary.getString("md5") : null;

			Logging.printLogDebug(LOGGER, session, "Previous verified md5: " + previousMd5);

			if ( !primaryMd5.equals(previousMd5) ) {
				Logging.printLogDebug(LOGGER, session, "Previous md5 is different from the new one...updating and seting as not_verified...");

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
				Logging.printLogDebug(LOGGER, session, "New image md5 is the same as the previous verified one. Nothing to be done.");
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
			Logging.printLogError(LOGGER, "Error fetching market info on postgres!");
			Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
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
			Logging.printLogError(LOGGER, session, CommonMethods.getStackTraceString(e));
		}

		return new JSONObject();
	}

}
