package br.com.lett.crawlernode.test.processor.base;

import java.util.ArrayList;

import org.bson.Document;

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

import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.imgprocessing.ImageFeatures;

public class DigitalContentAnalyserUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(DigitalContentAnalyserUtils.class);
	
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

			LocalFeatureList<Keypoint> features = new MemoryLocalFeatureList<Keypoint>();
			
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

}
