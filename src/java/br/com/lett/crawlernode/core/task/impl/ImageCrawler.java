package br.com.lett.crawlernode.core.task.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.bson.Document;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import com.google.gson.Gson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.imgprocessing.FeatureExtractor;
import br.com.lett.crawlernode.core.imgprocessing.ImageDownloadResult;
import br.com.lett.crawlernode.core.imgprocessing.ImageFeatures;
import br.com.lett.crawlernode.core.imgprocessing.ImageRescaler;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;

import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawler extends Task {

	private static final Logger logger = LoggerFactory.getLogger(ImageCrawler.class);

	private final int IMAGE_CHECKING_TRY = 5;

	protected Session session;

	private FeatureExtractor imageFeatureExtractor;

	public ImageCrawler(Session session) {
		this.session = session;
		this.imageFeatureExtractor = new FeatureExtractor();
	}

	@Override
	public void processTask() {

		try {

			ImageDownloadResult simpleDownloadResult = simpleDownload();

			// get metadata from the image on Amazon
			Logging.printLogDebug(logger, session, "Fetching image object metadata on Amazon: " + ((ImageCrawlerSession)session).getOriginalName());
			ObjectMetadata metadata = S3Service.fetchObjectMetadata(session, ((ImageCrawlerSession)session).getOriginalName());

			// if this image isn't on Amazon yet
			if ( metadata == null ) {
				Logging.printLogDebug(logger, session, "This image isn't on Amazon yet.");

				if (simpleDownloadResult.getImageFile() != null && simpleDownloadResult.getMd5() != null ) {

					update(simpleDownloadResult);
				}
			}

			// the image is already on the Amazon
			else {

				// get the md5 of the image on Amazon
				S3Object s3Object = S3Service.fetchS3Object(session, ((ImageCrawlerSession)session).getMd5AmazonPath());
				String amazonMd5 = null;
				if (s3Object != null) {
					amazonMd5 = S3Service.getAmazonImageFileMd5(s3Object);
				}

				Logging.printLogDebug(logger, session, "Looking for change on image # " + ((ImageCrawlerSession)session).getNumber() + "... (first check)");
				if (amazonMd5 == null) {
					Logging.printLogDebug(logger, session, "Amazon MD5 doesn't exists yet.");
				} else {
					Logging.printLogDebug(logger, session, "Amazon MD5: " + amazonMd5);
				}
				Logging.printLogDebug(logger, session, "Local MD5: " + simpleDownloadResult.getMd5());

				// let's see if the md5 has changed
				if ( amazonMd5 == null || isDifferent(amazonMd5, simpleDownloadResult.getMd5()) ) {
					Logging.printLogDebug(logger, session, "The new md5 doesn't exists on Amazon yet, or it's different from the previous md5.");

					ImageDownloadResult finalDownloadResult;

					if (amazonMd5 != null) {
						finalDownloadResult = trucoDownload(amazonMd5, simpleDownloadResult.getMd5());
					} else {
						finalDownloadResult = simpleDownloadResult;
					}

					update(finalDownloadResult);
				}
				else if (Main.executionParameters.mustForceImageUpdate()) {
					Logging.printLogDebug(logger, session, "The image md5 is already on Amazon, but i want to force the update.");

					update(simpleDownloadResult);
				}
				else {
					Logging.printLogDebug(logger, session, "The image md5 is already on Amazon.");
				}
			}

		} catch (Exception e) {
			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}		

	}

	@Override
	public void onStart() {
		Logging.printLogDebug(logger, session, "START");
	}

	@Override
	public void onFinish() {
		List<SessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");


		if (!errors.isEmpty()) {
			Logging.printLogError(logger, session, "Task failed!");

			// print all errors of type exceptions
			for (SessionError error : errors) {
				if (error.getType().equals(SessionError.EXCEPTION)) {
					Logging.printLogError(logger, session, error.getErrorContent());
				}
			}
		}
		else {

			// only remove the task from queue if it was flawless
			Logging.printLogDebug(logger, session, "Task completed.");
			Logging.printLogDebug(logger, session, "Deleting task: " + session.getOriginalURL() + " ...");

		}

		// clear the session
		session.clearSession();

		Logging.printLogDebug(logger, session, "END");
	}

	/**
	 * This method takes the following steps:
	 * 1) Create all rescaled versions of the image
	 * 2) Upload all the content to the Amazon bucket
	 * 3) Stores all the image metadata on Mongo
	 * 
	 * @param imageDownloadResult
	 * @throws IOException
	 */
	private void update(ImageDownloadResult imageDownloadResult) throws IOException {

		// create a buffered image from the downloaded image
		Logging.printLogDebug(logger, session, "Creating a buffered image...");
		BufferedImage bufferedImage = createImage(imageDownloadResult.getImageFile());

		// apply rescaling on the image
		Logging.printLogDebug(logger, session, "Rescaling the image...");
		rescale(bufferedImage, imageDownloadResult.getImageFile());

		// upload to Amazon
		Logging.printLogWarn(logger, session, "Uploading image to Amazon...only the md5 isn't null");
		if (imageDownloadResult.getMd5() != null) {
			S3Service.uploadImageToAmazon(session, imageDownloadResult.getMd5());
		}

		// store image metadata, including descriptors and hash
		// using the md5 of the local original file, to maintain as the original code
		// for now is commented...looking for a better way to deal with image features TODO

		//storeImageMetaData( bufferedImage, CommonMethods.computeMD5(new File(((ImageCrawlerSession)session).getLocalOriginalFileDir())) );
	}

	private ImageDownloadResult simpleDownload() throws IOException {
		ImageDownloadResult result = new ImageDownloadResult();
		File imageFile = downloadImage();
		String md5 = CommonMethods.computeMD5(imageFile);

		result.setImageFile(imageFile);
		result.setMd5(md5);

		return result;
	}

	private ImageDownloadResult trucoDownload(String amazonMd5, String currentMd5) throws IOException {
		String pastIterationMd5 = currentMd5;
		int iteration = 1;
		ImageDownloadResult result = null;

		Logging.printLogDebug(logger, session, "Initiating truco download...");

		while (iteration <= IMAGE_CHECKING_TRY) {
			try {
				Logging.printLogDebug(logger, session, "Iteration " + iteration + "...");

				// Fazer um novo download da imagem
				result = simpleDownload();

				// Comparar com a da iteração anterior, se der igual, retorno
				if ( result.getMd5() != null && result.getMd5().equals(pastIterationMd5) ) {
					Logging.printLogDebug(logger, session, "Fiz um download novo e constatei que era igual a imagem que eu tinha da iteração anterior");
					return result;
				}

				// Comparar o md5 do download atual com o da Amazon -- se chegou aqui, significa que o download
				// atual não foi igual ao da iteração anterior
				if ( amazonMd5.equals(result.getMd5()) ) {
					Logging.printLogDebug(logger, session, "Imagem do download atual, que é a iteração " + iteration + " é igual a da Amazon.");
					return result;
				}

				// Se chegou até aqui significa que o novo download não foi igual ao download da iteração anterior e que também
				// não foi igual ao da Amazon. Vamos atualizar o pastIterationMd5 com o download atual e partir para a próxima iteração
				Logging.printLogDebug(logger, session, "O novo download não foi igual ao download da iteração anterior.");
				Logging.printLogDebug(logger, session, "Vou partir para a próxima iteração.");

				pastIterationMd5 = result.getMd5();

				iteration++;

			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
		}

		Logging.printLogDebug(logger, session, "Fiz todas as tentativas e conclui que a imagem realmente mudou.");

		return result;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	private File downloadImage() throws IOException {
		Logging.printLogDebug(logger, session, "Downloading image from market...");
		return DataFetcher.fetchImage(session);
	}

	/**
	 * 
	 * @param imageFile
	 * @return
	 * @throws IOException
	 */
	private BufferedImage createImage(File imageFile) throws IOException {
		if (imageFile == null) {
			Logging.printLogDebug(logger, session, "Image file is null!");
			return null;
		}

		return ImageIO.read(imageFile);
	}

	/**
	 * 
	 * @param bufferedImage
	 * @param imageFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void rescale(BufferedImage bufferedImage, File imageFile) throws FileNotFoundException, IOException {
		if (bufferedImage == null) {
			Logging.printLogError(logger, session, "Image downloaded is null...returning...");
			return;
		}

		ImageRescaler.rescale(session, bufferedImage, imageFile);
	}

	/**
	 * 
	 * @param image
	 * @return
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void storeImageMetaData(BufferedImage image, String md5) throws NullPointerException, FileNotFoundException, IOException {
		if (image == null || md5 == null) {
			Logging.printLogError(logger, session, "Image or md5 is null...returning...");
			return;
		}

		Logging.printLogDebug(logger, session, "Loking for image features on Mongo...");

		// Só extraio as features e insiro no mongo, se elas já não estiverem presentes
		MongoDatabase database = Main.dbManager.mongoMongoImages;
		MongoCollection imageFeaturesCollection = database.getCollection("ImageFeatures");
		FindIterable<Document> iterable = imageFeaturesCollection.find(Filters.eq("md5", md5));
		Document document = iterable.first();

		if(document == null) {
			try {
				Logging.printLogDebug(logger, session, "Image features are not on Mongo yet.");
				Logging.printLogDebug(logger, session, "Extraindo descritores da imagem...");

				// extract sift descriptors from image
				LocalFeatureList<Keypoint> features = this.imageFeatureExtractor.extractFeatures(image);

				// create serializable container class to store the image features
				ImageFeatures imageFeatures = new ImageFeatures(features, md5);

				// convert the object to json
				Gson gson = new Gson();
				Document doc = Document.parse(gson.toJson(imageFeatures));

				// store on mongo
				if (database != null && imageFeaturesCollection != null) {
					Logging.printLogDebug(logger, session, "Storing image descriptors on Mongo...");
					imageFeaturesCollection.insertOne(doc);	 
				}

			} catch (Exception e) {
				Logging.printLogDebug(logger, session, CommonMethods.getStackTraceString(e));
				SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e));
				session.registerError(error);
			}
			Logging.printLogDebug(logger, session, "Descritores inseridos com sucesso.");
		} else {
			Logging.printLogDebug(logger, session, "md5 desta imagem já está no mongo");
		}
	}

	/**
	 * 
	 * @param metadata
	 * @param md5
	 * @return
	 */
	private boolean isDifferent(String amazonMd5, String md5) {

		// Se nunca teve imagem na amazon, retorna true se o md5 da local não for nulo também
		if(amazonMd5 == null) {
			return md5 != null;
		} 

		// Se tem imagem na amazon, retorna true se o md5 da local for diferente ou nulo
		else {
			return !amazonMd5.equals(md5);
		}

	}

}
