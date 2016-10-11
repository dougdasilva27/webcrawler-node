package br.com.lett.crawlernode.core.fetcher;

public enum ProxyService {
	
	buy,
	bonanza,
	storm,
	no_proxy,
	charity,
	azure;
	
	@Override
	public String toString() {
		switch (this) {
			case buy: return buy.name();
			case bonanza: return bonanza.name();
			case storm: return storm.name();
			case no_proxy: return no_proxy.name();
			case charity: return charity.name();
			case azure: return azure.name();
			default: return no_proxy.name();
		}
	}
	
}
