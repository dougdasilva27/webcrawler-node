package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class DesiredCapabilitiesBuilder {

	private String userAgent;
	private String executablePath;
	private LettProxy lettProxy;
	private Proxy proxy;
	private List<String> clientArgs;
	private String browserName;

	public static DesiredCapabilitiesBuilder create() {
		return new DesiredCapabilitiesBuilder();
	}

	protected DesiredCapabilitiesBuilder() {
		super();
	}

	public DesiredCapabilitiesBuilder setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

	public DesiredCapabilitiesBuilder setExecutablePathProperty(String executablePath) {
		this.executablePath = executablePath;
		return this;
	}

	public DesiredCapabilitiesBuilder setLettProxy(LettProxy lettProxy) {
		this.lettProxy = lettProxy;
		return this;
	}
	
	public DesiredCapabilitiesBuilder setProxy(Proxy proxy) {
		this.proxy = proxy;
		return this;
	}

	public DesiredCapabilitiesBuilder setBrowserType(String browserName) {
		this.browserName = browserName;
		return this;
	}

	public DesiredCapabilities build() {
		DesiredCapabilities desiredCapabilities = DesiredCapabilities.chrome();

		desiredCapabilities.setPlatform(Platform.ANY);
		desiredCapabilities.setVersion("ANY");
		desiredCapabilities.setBrowserName(browserName);
		
		if (proxy != null) desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);

		return desiredCapabilities;
	}

//	private List<String> createClientArgs() {
//		List<String> clientArgs = new ArrayList<String>();
//
//		clientArgs.add("--web-security=false");
//		clientArgs.add("--ignore-ssl-errors=true");
//		clientArgs.add("--ssl-protocol=any");		// necessary to fetch https urls
//		clientArgs.add("--load-images=false");		// don't download images when fetching pages
//
//		return clientArgs;
//	}

}
