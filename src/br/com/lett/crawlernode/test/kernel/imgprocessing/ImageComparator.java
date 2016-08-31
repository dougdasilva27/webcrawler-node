package br.com.lett.crawlernode.test.kernel.imgprocessing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;

import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.model.fit.RANSAC;

import org.openimaj.util.pair.Pair;

public class ImageComparator {
	
	private int numberOfMatchesTreshold;
	private DoGSIFTEngine engine;
	private String referenceImagePath;
	private MBFImage referenceImage;
	private LocalFeatureList<Keypoint> referenceKeypoints;
	private double rate;
	
	public ImageComparator() {
		engine = new DoGSIFTEngine();
		this.numberOfMatchesTreshold = 150;
	}
	
	public void setThreshold(int threshold) {
		this.numberOfMatchesTreshold = threshold;
	}
	
	public int getThreshold() {
		return this.numberOfMatchesTreshold;
	}
	
	public void setReference(String referenceImagePath) {
		this.referenceImagePath = referenceImagePath;
		try {
			this.referenceImage = ImageUtilities.readMBF(new File(referenceImagePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.referenceKeypoints = engine.findFeatures(referenceImage.flatten());
	}
	
	public void setReference(ImageFeatures referenceImageFeatures) {
		this.referenceImagePath = null;
		this.referenceImage = null;
		this.referenceKeypoints = referenceImageFeatures.getFeatures();
	}
	
	public ImageComparationResult compareWithReference(ImageFeatures imageFeatures) {
		ImageComparationResult result = new ImageComparationResult();
		
		if(imageFeatures != null) {

			// Get the features
			LocalFeatureList<Keypoint> queryKeypoints = imageFeatures.getFeatures();

			// Filter the matches based a geometric model
			RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(5.0, 1500, new RANSAC.PercentageInliersStoppingCondition(0.5));

			// Match the descriptors of the query image with those from the reference image
			LocalFeatureMatcher<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);
			matcher.setModelFeatures(queryKeypoints);
			matcher.findMatches(referenceKeypoints);
			List<Pair<Keypoint>> matches = matcher.getMatches();
			
			// Create result data
			result.setNumberOfMatches( matches.size() );
			result.setRate(computeRate( matches.size()) );
			result.setTotalNumberOfMatches(referenceKeypoints.size());
			if(matches.size() > this.numberOfMatchesTreshold) {
				result.setPassed();
			}

		}
		else {
			result.setNumberOfMatches(0);
			result.setTotalNumberOfMatches(0);
		}
		
		return result;
		
	}
	
	private double computeRate(int matches) {
		if (referenceKeypoints.size() > 0) {
			return (double)matches / (double)referenceKeypoints.size();
		}
		return 0;
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
	
	/**
	 * Extract the SIFT features from an image
	 * @param queryImage The image to extract features and keypoints from
	 * @return A feature list
	 */
	public LocalFeatureList<Keypoint> extractFeatures(MBFImage queryImage) {
		return this.engine.findFeatures(queryImage.flatten());
	}

}
