package br.com.lett.crawlernode.core.task;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.json.JSONObject;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.imgprocessing.FeatureExtractor;
import br.com.lett.crawlernode.core.imgprocessing.ImageFeatures;
import br.com.lett.crawlernode.core.imgprocessing.ImageRescaler;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.CrawlerSessionError;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ImageCrawler.class);

	private final int IMAGE_CHECKING_TRY = 5;

	protected CrawlerSession session;

	private FeatureExtractor imageFeatureExtractor;

	public ImageCrawler(CrawlerSession session) {
		this.session = session;
		this.imageFeatureExtractor = new FeatureExtractor();
	}

	@Override
	public void run() {

		try {
			
			BufferedImage bufferedImage = null;
			boolean mustUploadToAmazon = false;
			JSONObject checkChangeResult = null;

			// download the image and compute it's md5
			File imageFile = downloadImage();
			String md5 = CommonMethods.computeMD5(imageFile);

			// get metada from the image on Amazon
			Logging.printLogDebug(logger, session, "Fetching image object metadata on Amazon: " + ((ImageCrawlerSession)session).getOriginalName());
			ObjectMetadata metadata = S3Service.fetchObjectMetadata(session, ((ImageCrawlerSession)session).getOriginalName());

			// looking for image change
			boolean changedFirstCheck = false;
			boolean changedSecondCheck = false;

			Logging.printLogDebug(logger, session, "Looking for change on image # " + ((ImageCrawlerSession)session).getNumber() + "... (first check)");
			Logging.printLogDebug(logger, session, "Amazon MD5: " + (metadata == null ? null : metadata.getETag()));
			Logging.printLogDebug(logger, session, "Local MD5: " + md5);
			
			changedFirstCheck = isDifferent(metadata, md5);

			if(changedFirstCheck) {
				Logging.printLogDebug(logger, session, "Imagem mudou na first check, vou conferir fazendo a second check...");

				// re-download image and compute it's md5
				imageFile = downloadImage();
				md5 = CommonMethods.computeMD5(imageFile);

				Logging.printLogDebug(logger, session, "Looking for change on image # " + ((ImageCrawlerSession)session).getNumber() + "... (second check)");
				Logging.printLogDebug(logger, session, "Amazon MD5: " + (metadata == null ? null : metadata.getETag()));
				Logging.printLogDebug(logger, session, "Local MD5: " + md5);
				
				// look for change
				changedSecondCheck = isDifferent(metadata, md5);

			} else {
				Logging.printLogDebug(logger, session, "Image didn't changed. (md5: " + md5 + ")");
			}

			if(changedFirstCheck && !changedSecondCheck) {
				Logging.printLogDebug(logger, session, "Achei que a imagem tinha mudado, mas ao baixar de novo para conferir, vi que a imagem não mudou!");
			}

			// Se passou pelo first e second check, antes de dizer se realmente mudou, vou fazer uma checagem mais minuciosa
			if(changedFirstCheck && changedSecondCheck) {
				Logging.printLogDebug(logger, session, "Imagem não passou no teste do first e second check. Checarei mais algumas vezes até obter duas imagens iguais");

				// detailed checking
				checkChangeResult = checkChange(metadata, md5);

				if (checkChangeResult.getBoolean("changed")) {
					Logging.printLogWarn(logger, session, "Image changed! Must upload to Amazon...");
					mustUploadToAmazon = true;
				}
			}
			
			// create a buffered image from the downloaded image
			Logging.printLogDebug(logger, session, "Creating a buffered image...");
			bufferedImage = createImage(imageFile);

			// apply rescaling on the image
			Logging.printLogDebug(logger, session, "Rescaling the image...");
			rescale(bufferedImage, imageFile);
			
			// upload to Amazon
			if (mustUploadToAmazon) {
				Logging.printLogWarn(logger, session, "Uploading image to Amazon...");
				S3Service.uploadImageToAmazon(session, checkChangeResult.getString("md5"));
				
				// store image metadata, including descriptors and hash
				storeImageMetaData(bufferedImage, checkChangeResult.getString("md5"));
			} else {
				storeImageMetaData(bufferedImage, md5);
			}

		} catch (Exception e) {
			session.registerError( new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}		

	}

	/**
	 * 
	 * @return
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private File downloadImage() throws NullPointerException, FileNotFoundException, IOException {
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
				if(database != null) {
					if(imageFeaturesCollection != null) {
						Logging.printLogDebug(logger, session, "Storing image descriptors on Mongo...");
						imageFeaturesCollection.insertOne(doc);	 
					}
				}

			} catch (Exception e) {
				Logging.printLogDebug(logger, session, CommonMethods.getStackTraceString(e));
				CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTraceString(e));
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
	private boolean isDifferent(ObjectMetadata metadata, String md5) {

		// Se nunca teve imagem na amazon, retorna true se o md5 da local não for nulo também
		if(metadata == null || metadata.getETag() == null) {
			return md5 != null;
		} 

		// Se tem imagem na amazon, retorna true se o md5 da local for diferente ou nulo
		else {
			return !metadata.getETag().equals(md5);
		}

	}

	/**
	 * 
	 * @param metadata
	 * @param md5SecondDownload
	 * @return
	 */
	private JSONObject checkChange(ObjectMetadata metadata, String md5SecondDownload) {
		
		JSONObject result = new JSONObject();

		/*
		 * Neste ponto temos o md5 da Amazon e o md5 do segundo download.
		 * Vamos seguir fazendo um download por vez em um laço e comparando o novo download
		 * com o md5 da Amazon e com o md5 que tínhamos da iteração anterior. Ao encontrar algo igual em qualquer
		 * um dos casos, nos retornamos. Se foi igual ao md5 da Amazon em algum momento, retornamos false (pois são
		 * iguais, e a função está olhando se foram diferentes). Se for igual
		 * ao md5 que tínhamos da iteração anterior, retornamos true.
		 */
		String pastIterationMd5 = md5SecondDownload;
		int iteration = 1;
		while (iteration <= IMAGE_CHECKING_TRY) {
			try {

				Logging.printLogDebug(logger, session, "Iteração " + iteration + "...");

				// Fazer um novo download da imagem
				File imageFile = downloadImage();
				String imageNow = CommonMethods.computeMD5(imageFile);

				// Comparar com a da iteração anterior, se der igual, retorno true
				if ( imageNow != null && imageNow.equals(pastIterationMd5) ) {
					Logging.printLogDebug(logger, session, "Fiz um download novo e constatei que era igual a imagem que eu tinha da iteração anterior");
					
					result.append("changed", true);
					result.append("md5", imageNow);
					
					return result;
				}

				// Comparar o md5 do download atual com o da Amazon -- se chegou aqui, significa que o download
				// atual não foi igual ao da iteração anterior
				if ( !isDifferent(metadata, imageNow) ) {
					Logging.printLogDebug(logger, session, "Imagem do download atual, que é a iteração " + iteration + " é igual a da Amazon.");
					
					result.append("changed", false);
					result.append("md5", imageNow);
					
					return result;
				}

				// Se chegou até aqui significa que o novo download não foi igual ao download da iteração anterior e que também
				// não foi igual ao da Amazon. Vamos atualizar o pastIterationMd5 com o download atual e partir para a próxima iteração
				Logging.printLogDebug(logger, session, "O novo download não foi igual ao download da iteração anterior.");
				Logging.printLogDebug(logger, session, "Vou partir para a próxima iteração.");

				pastIterationMd5 = imageNow;

				iteration++;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Logging.printLogDebug(logger, session, "Fiz todas as tentativas e conclui que a imagem realmente mudou.");
		
		result.append("changed", true);
		result.append("md5", pastIterationMd5);

		return result;
	}

}
