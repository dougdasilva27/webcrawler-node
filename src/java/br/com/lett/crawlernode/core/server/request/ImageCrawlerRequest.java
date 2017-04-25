package br.com.lett.crawlernode.core.server.request;

public class ImageCrawlerRequest extends Request {
	
	private String imageType;
	private Integer imageNumber;
	
	public ImageCrawlerRequest() {
		super();
	}
	
	public String getImageType() {
		return imageType;
	}
	
	public void setImageType(String imageType) {
		this.imageType = imageType;
	}
	
	public Integer getImageNumber() {
		return imageNumber;
	}
	
	public void setImageNumber(Integer imageNumber) {
		this.imageNumber = imageNumber;
	}

}
