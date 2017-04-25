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

import com.google.gson.Gson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.imgprocessing.FeatureExtractor;
import br.com.lett.crawlernode.core.imgprocessing.ImageDownloadResult;
import br.com.lett.crawlernode.core.imgprocessing.ImageFeatures;
import br.com.lett.crawlernode.core.imgprocessing.ImageConverter;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;

import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import comunication.MongoDB;

public class ImageCrawler extends Task {

	private static final Logger logger = LoggerFactory.getLogger(ImageCrawler.class);

	private final int IMAGE_CHECKING_TRY = 5;

	private FeatureExtractor imageFeatureExtractor;

	public ImageCrawler(Session session) {
		this.session = session;
		this.imageFeatureExtractor = new FeatureExtractor();
	}

	@Override
	public void processTask() {

		try {
			ImageDownloadResult simpleDownloadResult = simpleDownload();

			if (simpleDownloadResult.imageFile == null) {
				Logging.printLogError(logger, session, "Failed to download image....returning from image processTask()....");
				session.registerError(new SessionError(SessionError.BUSINESS_LOGIC, "Download image failed."));
				return;
			}

			// get metadata from the image on Amazon
			Logging.printLogDebug(logger, session, "Fetching image object metadata on Amazon: " + ((ImageCrawlerSession)session).getImageKeyOnBucket());
			ObjectMetadata metadata = S3Service.fetchObjectMetadata(session, ((ImageCrawlerSession)session).getImageKeyOnBucket());
			
			if ( metadata == null ) { // we doesn't have any image under this path in S3 yet
				Logging.printLogDebug(logger, session, "This image isn't on Amazon yet.");

				if (simpleDownloadResult.imageFile != null && simpleDownloadResult.md5 != null ) {
					update(simpleDownloadResult);
				}
			}

			// we already have an image under this path in S3
			else {

				String amazonMd5 = metadata.getUserMetaDataOf(S3Service.MD5_HEX_METADATA_FIELD);

				Logging.printLogDebug(logger, session, "Looking for change on image # " + ((ImageCrawlerSession)session).getImageNumber() + "... (first check)");
				if (amazonMd5 == null) {
					Logging.printLogDebug(logger, session, "Amazon MD5 doesn't exists yet.");
				} else {
					Logging.printLogDebug(logger, session, "Amazon MD5: " + amazonMd5);
				}
				Logging.printLogDebug(logger, session, "Local MD5: " + simpleDownloadResult.md5);

				// let's see if the md5 has changed
				if ( amazonMd5 == null || isDifferent(amazonMd5, simpleDownloadResult.md5) ) {
					Logging.printLogDebug(logger, session, "The new md5 doesn't exists on Amazon yet, or it's different from the previous md5.");

					ImageDownloadResult finalDownloadResult;

					if (amazonMd5 != null) {
						finalDownloadResult = trucoDownload(amazonMd5, simpleDownloadResult.md5);
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
			session.setTaskStatus(Task.STATUS_FAILED);
		}
		else { // only remove the task from queue if it was flawless
			Logging.printLogDebug(logger, session, "Task completed.");
			session.setTaskStatus(Task.STATUS_COMPLETED);
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
		BufferedImage bufferedImage = createImage(imageDownloadResult.imageFile);

		// convert image to jpg if necessary
		Logging.printLogDebug(logger, session, "Rescaling the image...");
		convertImage(bufferedImage, imageDownloadResult.imageFile);

		// upload to Amazon
		Logging.printLogWarn(logger, session, "Uploading image to Amazon...only if the computed md5 isn't null");
		if (imageDownloadResult.md5 != null) {
			ObjectMetadata newObjectMetadata = new ObjectMetadata();
			newObjectMetadata.addUserMetadata(S3Service.MD5_HEX_METADATA_FIELD, imageDownloadResult.md5);
			S3Service.uploadImage(session, newObjectMetadata);
		}
	}

	private ImageDownloadResult simpleDownload() throws IOException {
		ImageDownloadResult result = new ImageDownloadResult();
		File imageFile = downloadImage();
		String md5 = CommonMethods.computeMD5(imageFile);

		result.imageFile = imageFile;
		result.md5 = md5;

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
				if ( result.md5 != null && result.md5.equals(pastIterationMd5) ) {
					Logging.printLogDebug(logger, session, "Fiz um download novo e constatei que era igual a imagem que eu tinha da iteração anterior");
					return result;
				}

				// Comparar o md5 do download atual com o da Amazon -- se chegou aqui, significa que o download
				// atual não foi igual ao da iteração anterior
				if ( amazonMd5.equals(result.md5) ) {
					Logging.printLogDebug(logger, session, "Imagem do download atual, que é a iteração " + iteration + " é igual a da Amazon.");
					return result;
				}

				// Se chegou até aqui significa que o novo download não foi igual ao download da iteração anterior e que também
				// não foi igual ao da Amazon. Vamos atualizar o pastIterationMd5 com o download atual e partir para a próxima iteração
				Logging.printLogDebug(logger, session, "O novo download não foi igual ao download da iteração anterior.");
				Logging.printLogDebug(logger, session, "Vou partir para a próxima iteração.");

				pastIterationMd5 = result.md5;

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
	private void convertImage(BufferedImage bufferedImage, File imageFile) throws FileNotFoundException, IOException {
		if (bufferedImage == null) {
			Logging.printLogError(logger, session, "Image downloaded is null...returning...");
			return;
		}

		ImageConverter.convertToJPG(session, bufferedImage, imageFile);
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
		MongoDB imagesDatabase = Main.dbManager.connectionImages;
		try {
			FindIterable<Document> iterable = imagesDatabase.runFind(Filters.eq("md5", md5), "ImageFeatures");
			Document document = iterable.first();

			if(document == null) {

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
				if (imagesDatabase != null) {
					Logging.printLogDebug(logger, session, "Storing image descriptors on Mongo...");
					imagesDatabase.insertOne(doc, "ImageFeatures");
				}

			} else {
				Logging.printLogDebug(logger, session, "md5 desta imagem já está no mongo");
			}
		} catch (Exception e) {
			Logging.printLogDebug(logger, session, CommonMethods.getStackTraceString(e));
			SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e));
			session.registerError(error);
		}

		Logging.printLogDebug(logger, session, "Descritores inseridos com sucesso.");
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
