package br.com.lett.crawlernode.core.models;

import java.util.Map;

import org.json.JSONObject;

/**
 * 
 * This class is a holder for the different crawled prices.
 * 
 * e.g:
 * 
 * bank_ticket : {
 * 	1: 985
 * }
 * card : {
 * 	1 : 1000
 * 	2 : 1010
 * 	3 : 1500
 * }
 * 
 * @author Samir Leao
 *
 */
public class Prices {

	public static final String BANK_TICKET_FIELD_NAME = "bank_ticket";
	public static final String CARD_FIELD_NAME = "card";
	
	public static final String VISA = "visa";

	private JSONObject prices;

	public Prices() {
		this.prices = new JSONObject();
		this.prices.put(BANK_TICKET_FIELD_NAME, new JSONObject().put("1", JSONObject.NULL));
		this.prices.put(CARD_FIELD_NAME, new JSONObject());
	}

	/**
	 * 
	 * @param bankTicketPrice
	 */
	public void insertBankTicket(Float bankTicketPrice) {
		JSONObject bankTicket = new JSONObject();
		if (bankTicketPrice == null) {
			bankTicket.put("1", JSONObject.NULL);
		} else {
			bankTicket.put("1", bankTicketPrice);
		}
		this.prices.put(BANK_TICKET_FIELD_NAME, bankTicket);
	}

	/**
	 * 
	 * @return
	 */
	public Float getBankTicketPrice() {
		JSONObject bankTicket = this.prices.getJSONObject(BANK_TICKET_FIELD_NAME);
		if (bankTicket.has("1")) {
			Object value = bankTicket.get("1");
			if (value != JSONObject.NULL) {
				Double valueDouble = bankTicket.getDouble("1");
				return valueDouble.floatValue();
			}
		}
		return null;
	}

	/**
	 * 
	 * @param cardBrand
	 * @param installmentPriceMap
	 */
	public void insertCardInstallment(String cardBrand, Map<Integer, Float> installmentPriceMap) {
		if (installmentPriceMap.size() > 0) {
			JSONObject currentCardObject = this.prices.getJSONObject(CARD_FIELD_NAME);
			
			// create the new card json object
			JSONObject installmentPrices = new JSONObject();
			for (Integer installmentNumber : installmentPriceMap.keySet()) {
				installmentPrices.put(installmentNumber.toString(), installmentPriceMap.get(installmentNumber));
			}
			currentCardObject.put(cardBrand, installmentPrices);
			
			// update the card price options on prices
			this.prices.put(CARD_FIELD_NAME, currentCardObject);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public JSONObject getRawCardPaymentOptions() {
		return this.prices.getJSONObject(CARD_FIELD_NAME);
	}
	
	/**
	 * 
	 * @param cardBrand
	 * @return
	 */
	public JSONObject getRawCardPaymentOptions(String cardBrand) {
		if (this.prices.getJSONObject(CARD_FIELD_NAME).has(cardBrand)) {
			return this.prices.getJSONObject(CARD_FIELD_NAME).getJSONObject(cardBrand);
		}
		return null;
	}

	public JSONObject getPricesJson() {
		return this.prices;
	}
	
	public void setPricesJson(JSONObject prices) {
		this.prices = prices;
	}

	/**
	 * The string representation of the json object.
	 */
	@Override
	public String toString() {
		return this.prices.toString();
	}

}
