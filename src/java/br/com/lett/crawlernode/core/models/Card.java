package br.com.lett.crawlernode.core.models;

public enum Card {
	
	VISA,
	MASTERCARD,
	MAESTRO,
	AMEX,
	DINERS,
	CREDICARD,
	ELO,
	HIPERCARD,
	HIPER,
	AURA,
	BNDES,
	DISCOVER,
	SHOP_CARD,
	HSCARD,
	CABAL,
	NO_CARD;
	
	@Override
	public String toString() {
		switch (this) {
			case VISA: return "visa";
			case MASTERCARD: return "mastercard";
			case MAESTRO: return "maestro";
			case AMEX: return "amex";
			case DINERS: return "diners";
			case CREDICARD: return "credicard";
			case ELO: return "elo";
			case HIPERCARD: return "hipercard";
			case HIPER: return "hiper";
			case AURA: return "aura";
			case BNDES: return "bndes";
			case DISCOVER: return "discover";
			case SHOP_CARD: return "shop_card";
			case HSCARD: return "hscard";
			case CABAL: return "cabal";
			default: return "not_a_card";
		}
	}

}
