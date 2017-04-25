package br.com.lett.crawlernode.core.session;

public enum TestType {
	
	INSIGHTS,
	IMAGE,
	SEED,
	DISCOVER,
	RATING,
	UNKNOWN;
	
	@Override
	public String toString() {
		switch (this) {
			case INSIGHTS: return "insights";
			case IMAGE: return "image";
			case SEED: return "seed";
			case DISCOVER: return "discover";
			case RATING: return "rating";
			default: return "unknown";
		}
	}

}
