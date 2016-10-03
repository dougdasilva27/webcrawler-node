package br.com.lett.crawlernode.core.imgprocessing;

import java.io.File;

public class ImageDownloadResult {
	
	private File imageFile;
	
	private String md5;
	
	public ImageDownloadResult() {
		super();
	}

	public File getImageFile() {
		return imageFile;
	}

	public void setImageFile(File imageFile) {
		this.imageFile = imageFile;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}
	
	

}
