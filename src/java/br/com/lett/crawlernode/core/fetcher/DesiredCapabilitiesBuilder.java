package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.Platform;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class DesiredCapabilitiesBuilder {

	private String userAgent;
	private String executablePath;
	private LettProxy lettProxy;
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

	public DesiredCapabilitiesBuilder setBrowserType(String browserName) {
		this.browserName = browserName;
		return this;
	}

	public DesiredCapabilities build() {
		DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();

		desiredCapabilities.setPlatform(Platform.ANY);
		desiredCapabilities.setVersion("ANY");
		desiredCapabilities.setBrowserName(browserName);

		this.clientArgs = createClientArgs();

		if (browserName.equals(BrowserType.PHANTOMJS)) {
			if (this.userAgent != null) {
				desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "User-Agent", this.userAgent);
			}

			if (this.clientArgs != null && this.clientArgs.size() > 0) {
				desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, this.clientArgs);
			}

			if (this.executablePath != null) {
				desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, this.executablePath);
			}

			//capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Authorization", proxy.getUser() + ":" + proxy.getPass());
		}

		return desiredCapabilities;
	}

	private List<String> createClientArgs() {
		List<String> clientArgs = new ArrayList<String>();

		clientArgs.add("--web-security=false");
		clientArgs.add("--ignore-ssl-errors=true");
		clientArgs.add("--load-images=false");

		return clientArgs;
	}

}
