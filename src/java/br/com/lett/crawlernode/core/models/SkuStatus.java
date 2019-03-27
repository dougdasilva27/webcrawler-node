package br.com.lett.crawlernode.core.models;

public enum SkuStatus {
	
	AVAILABLE("available"),
	UNAVAILABLE("unavailable"),
	MARKETPLACE_ONLY("only_marketplace"),
	VOID("void");
	
	private String s;
	
	private SkuStatus(String s) {
		this.s = s;
	}
	
	@Override
	public String toString() {
		return this.s;
	}

}
