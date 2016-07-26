package br.com.lett.crawlernode.imgprocessing;

import java.io.Serializable;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.feature.local.keypoints.Keypoint;

public class ImageFeatures implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private LocalFeatureList<Keypoint> features = new MemoryLocalFeatureList<Keypoint>();
	private String md5;
	
	public ImageFeatures(LocalFeatureList<Keypoint> features, String md5) {
		this.features = features;
		this.md5 = md5;
	}
	
	public LocalFeatureList<Keypoint> getFeatures() {
		return this.features;
	}
	
	public String getMd5() {
		return this.md5;
	}

	public void setFeatures(LocalFeatureList<Keypoint> features) {
		this.features = features;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	@Override
	public String toString() {
		return "ImageFeatures [features=" + features.size() + ", md5=" + md5 + "]";
	}
	
}
