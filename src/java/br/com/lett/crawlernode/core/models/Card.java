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
	SHOP_CARD;
	
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
			case SHOP_CARD: return "shop_card";
			default: return "not_a_card";
		}
	}

}
