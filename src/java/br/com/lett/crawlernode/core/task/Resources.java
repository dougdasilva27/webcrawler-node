package br.com.lett.crawlernode.core.task;

import java.io.File;

public class Resources {
	
	private File webdriverExtension;
	
	public Resources() {
		super();
	}
	
	public void setWebdriverExtension(File webdriverExtension) {
		this.webdriverExtension = webdriverExtension;
	}
	
	public File getWebdriverExtension() {
		return webdriverExtension;
	}

}
