package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;

public class FeatureExtractor {
	
	private DoGSIFTEngine engine;
	
	public FeatureExtractor() {
		engine = new DoGSIFTEngine();
	}	
	
	/**
	 * Extract the SIFT features from an image
	 * @param queryImage The image to extract features and keypoints from
	 * @return A feature list
	 */
	public LocalFeatureList<Keypoint> extractFeatures(String path) {

		if (path != null && !path.isEmpty()) {

			// Load the image
			MBFImage image = loadImage(path);

			// Get the features
			return this.engine.findFeatures(image.flatten());

		}
		
		return null;
	}
	
	public LocalFeatureList<Keypoint> extractFeatures(BufferedImage image) {

		if (image != null) {

			// Load the image
			MBFImage mbfImage = loadImage(image);

			// Get the features
			return this.engine.findFeatures(mbfImage.flatten());

		}
		
		return null;
	}
	
	/**
	 * Load an image from the path
	 * @param imagePath The path to the image file
	 * @return A MBFImage ready to be processed by openIMAJ
	 */
	public MBFImage loadImage(String imagePath) {
		MBFImage image = null;
		try {
			image = ImageUtilities.readMBF(new File(imagePath));
			return image;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
	}
	
	/**
	 * Load an image from a BufferedImage
	 * @param bufferedImage The buffered image
	 * @return A MBFImage ready to be processed by openIMAJ
	 */
	public MBFImage loadImage(BufferedImage bufferedImage) {
		return ImageUtilities.createMBFImage(bufferedImage, false);
	}

}
