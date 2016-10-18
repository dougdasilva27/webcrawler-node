package br.com.lett.crawlernode.core.models;

import java.util.Map;

import org.json.JSONObject;

public class Prices {

	public static final String BANK_TICKET_FIELD_NAME = "bank_ticket";
	public static final String CARD_FIELD_NAME = "card";

	private JSONObject prices;

	public Prices() {
		this.prices = new JSONObject();
	}

	/**
	 * Boleto bancario.
	 * If price is null, the field will not exist.
	 * 
	 * e.g
	 * 
	 * bank_ticket : {
	 * 	1 : 985
	 * }
	 * 
	 * @param bankTicketPrice
	 */
	public void insertBankTicket(Float bankTicketPrice) {
		JSONObject bankTicket = new JSONObject();
		bankTicket.put("1", bankTicketPrice);
		this.prices.put(BANK_TICKET_FIELD_NAME, bankTicket);
	}

	/**
	 * 
	 * @return
	 */
	public Float getBankTicketPrice() {
		if (this.prices.has(BANK_TICKET_FIELD_NAME)) {
			JSONObject bankTicket = this.prices.getJSONObject(BANK_TICKET_FIELD_NAME);
			Double v = bankTicket.getDouble("1");
			return v.floatValue();
		}
		return null;
	}

	/**
	 * Parcelado.
	 * 
	 * e.g
	 * 
	 * card : {
	 * 	1 : 1000
	 * 	2 : 1010
	 * 	3 : 1500
	 * }
	 * 
	 * @param installmentPriceMap
	 */
	public void insertCardInstallment(Map<Integer, Float> installmentPriceMap) {
		if (installmentPriceMap.size() > 0) {
			JSONObject installmentPrices = new JSONObject();
			for (Integer installmentNumber : installmentPriceMap.keySet()) {
				installmentPrices.put(installmentNumber.toString(), installmentPriceMap.get(installmentNumber));
			}
			this.prices.put(CARD_FIELD_NAME, installmentPrices);
		}
	}

	public JSONObject getPricesJson() {
		return this.prices;
	}

	@Override
	public String toString() {
		return this.prices.toString();
	}

}
