package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class DesiredCapabilitiesBuilder {
	
	private static final String DEFAULT_BROWSER = "chrome";
	private static final String DEFAULT_PROXY = "191.235.90.114:3333";

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
		
		if (browserName != null) {
			desiredCapabilities.setBrowserName(browserName);
		} else {
			desiredCapabilities.setBrowserName(DEFAULT_BROWSER);
		}
		
		if (proxy != null) {
			desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);
		} else {
			Proxy defaultProxy = new Proxy();
			defaultProxy.setHttpProxy(DEFAULT_PROXY);
			defaultProxy.setSslProxy(DEFAULT_PROXY);
			desiredCapabilities.setCapability(CapabilityType.PROXY, defaultProxy);
		}
		
		ChromeOptions chromeOptions = new ChromeOptions();
		
		if (userAgent != null) {
			List<String> chromeArgs = new ArrayList<String>();
			chromeArgs.add("--user-agent=" + userAgent);
//			chromeArgs.add("--allow-insecure-localhost");
//			chromeArgs.add("--ssl-version-max=tls1.3");
//			chromeArgs.add("--ssl-version-min=tls1");
//			chromeArgs.add("--ignore-certificate-errors=true");
//			chromeArgs.add("--ignore-urlfetcher-cert-requests=true");
			
			chromeOptions.addArguments(chromeArgs);
			
			desiredCapabilities.setCapability("chromeOptions", chromeOptions);
		}
		
		desiredCapabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

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
