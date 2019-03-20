package br.com.lett.crawlernode.core.fetcher;

public enum FetchMode {
	
	STATIC,
	WEBDRIVER,
	SMART,
	UNKOWN_FETCHER;
	
	@Override
	public String toString() {
		switch (this) {
			case STATIC: return STATIC.name();
			case WEBDRIVER: return WEBDRIVER.name();
			case SMART: return SMART.name();
			default: return UNKOWN_FETCHER.name();
		}
	}

}
