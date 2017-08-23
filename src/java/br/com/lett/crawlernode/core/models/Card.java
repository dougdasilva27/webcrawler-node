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
	HSCARD,
	CABAL,
	SENFF,
	CREDISHOP,
	NARANJA,
	NATIVA,
	JCB,
	MULTICASH,
	MULTIEMPRESARIAL,
	MULTICHEQUE,
	MULTIBENEFICIOS,
	SOROCRED,
	
	SHOP_CARD,
	UNKNOWN_CARD;
	
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
			case SENFF: return "senff";
			case CREDISHOP: return "credishop";
			case NARANJA: return "naranja";
			case NATIVA: return "nativa";
			case MULTICASH: return "multicash";
			case JCB: return "jcb";
			case MULTIEMPRESARIAL: return "multiempresarial";
			case MULTICHEQUE: return "multicheque";
			case MULTIBENEFICIOS: return "multibeneficios";
			case SOROCRED: return "sorocred";
			default: return "unkown_card";
		}
	}

}
