package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.Resources;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;

public class TestHtmlBuilder {

	private static final Logger logger = LoggerFactory.getLogger(TestHtmlBuilder.class);

	private static final String PRODUCT_URL = "url";
	private static final String INTERNAL_ID = "internalId";
	private static final String INTERNAL_PID = "internalPid";
	private static final String NAME = "name";
	private static final String PRICE = "price";
	private static final String STOCK = "stock";
	private static final String AVAILABLE = "available";
	private static final String DESCRIPTION = "description";
	private static final String CAT1 = "category1";
	private static final String CAT2 = "category2";
	private static final String CAT3 = "category3";
	private static final String PRIMARY_IMAGE = "primaryImage";
	private static final String SECONDARY_IMAGES = "secondaryImages";
	private static final String MARKETPLACE = "marketplace";
	private static final String PRICES = "prices";
	private static final String BANK_TICKET = "bank_ticket";
	private static final String FROM = "from";

	public static String buildProductHtml(Product p, String pathWrite, Session session) {
		JSONObject productJson = new JSONObject(p.toJson());

		MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		File file = new File(Resources.getResource("productTemplate.html").getFile());

		Mustache mustache = null;
		try {
			mustache = mustacheFactory.compile(
					new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")),
					file.getName());
		} catch (FileNotFoundException e) {
			Logging.printLogError(logger, CommonMethods.getStackTrace(e));
		}

		if (mustache != null) {
			// Map to replace all var to product informations in html
			Map<String, Object> scopes = new HashMap<>();

			if (p.getUrl() != null) scopes.put(PRODUCT_URL, p.getUrl());
			putInternalId(productJson, scopes);
			putInternalPid(productJson, scopes);
			putName(productJson, scopes);
			putPrice(productJson, scopes);
			putAvailable(productJson, scopes);
			putStock(productJson, scopes);
			putDescription(productJson, scopes);
			putCat1(productJson, scopes);
			putCat2(productJson, scopes);
			putCat3(productJson, scopes);
			putPrimaryImage(productJson, scopes);
			putSecondaryImages(productJson, scopes);
			putMarketplaces(productJson, scopes);
			putPrices(productJson, scopes);

			// Execute replace in html
			StringWriter writer = new StringWriter();
			mustache.execute(writer, scopes);

			try (PrintWriter out = new PrintWriter(
					pathWrite + session.getMarket().getName() + "-" + scopes.get(INTERNAL_ID) + ".html")) {
				out.println(writer.toString());
			} catch (FileNotFoundException e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}

			return writer.toString();
		}

		return null;
	}

	private static void putInternalId(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(INTERNAL_ID) && !productJson.isNull(INTERNAL_ID)) {
			scopes.put(INTERNAL_ID, productJson.getString(INTERNAL_ID));
		}
	}

	private static void putInternalPid(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(INTERNAL_PID) && !productJson.isNull(INTERNAL_PID)) {
			scopes.put(INTERNAL_PID, productJson.getString(INTERNAL_PID));
		}
	}

	private static void putName(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(NAME) && !productJson.isNull(NAME)) {
			scopes.put(NAME, productJson.getString(NAME));
		}
	}

	private static void putPrice(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(PRICE) && !productJson.isNull(PRICE)) {
			scopes.put(PRICE, productJson.get(PRICE));
		}
	}

	private static void putAvailable(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(AVAILABLE) && !productJson.isNull(AVAILABLE)) {
			scopes.put(AVAILABLE, productJson.get(AVAILABLE));
		}
	}

	private static void putStock(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(STOCK) && !productJson.isNull(STOCK)) {
			scopes.put(STOCK, productJson.get(STOCK));
		}
	}

	private static void putDescription(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(DESCRIPTION) && !productJson.isNull(DESCRIPTION)) {
			scopes.put(DESCRIPTION, productJson.getString(DESCRIPTION));
		}
	}

	private static void putCat1(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(CAT1) && !productJson.isNull(CAT1)) {
			scopes.put(CAT1, productJson.getString(CAT1));
		}
	}

	private static void putCat2(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(CAT2) && !productJson.isNull(CAT2)) {
			scopes.put(CAT2, productJson.getString(CAT2));
		}
	}

	private static void putCat3(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(CAT3) && !productJson.isNull(CAT3)) {
			scopes.put(CAT3, productJson.getString(CAT3));
		}
	}

	private static void putPrimaryImage(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(PRIMARY_IMAGE) && !productJson.isNull(PRIMARY_IMAGE)) {
			scopes.put(PRIMARY_IMAGE, productJson.getString(PRIMARY_IMAGE));
		}
	}

	private static void putSecondaryImages(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(SECONDARY_IMAGES) && !productJson.isNull(SECONDARY_IMAGES)) {
			List<String> secondaryImages = new ArrayList<>();
			JSONArray imagesArray = new JSONArray(productJson.getString(SECONDARY_IMAGES));

			for (int i = 0; i < imagesArray.length(); i++) {
				secondaryImages.add(imagesArray.getString(i));
			}

			scopes.put(SECONDARY_IMAGES, secondaryImages);
		}
	}

	private static void putMarketplaces(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(MARKETPLACE) && !productJson.isNull(MARKETPLACE)) {
			Map<String, Object> marketPlaces = new HashMap<>();
			JSONArray arrayMarketplaces = new JSONArray(productJson.getString(MARKETPLACE));

			for (int i = 0; i < arrayMarketplaces.length(); i++) {
				JSONObject jsonMarketplace = arrayMarketplaces.getJSONObject(i);

				if (jsonMarketplace.has(NAME)) {
					String name = jsonMarketplace.getString(NAME);
					Map<Object, Object> prices = new HashMap<>();

					if (jsonMarketplace.has(PRICES)) {
						JSONObject pricesJson = jsonMarketplace.getJSONObject(PRICES);

						prices = getMapPricesFromJson(pricesJson);

						if (pricesJson.has(BANK_TICKET)) {
							JSONObject bankTicket = pricesJson.getJSONObject(BANK_TICKET);

							if (bankTicket.has("1")) {
								Map<Object, Object> bankTicketPrices = new HashMap<>();
								bankTicketPrices.put(1, bankTicket.get("1"));
								prices.put("Boleto", bankTicketPrices.entrySet());
							}
						}
					} else if (jsonMarketplace.has(PRICE)) {
						prices.put("1'", jsonMarketplace.get(PRICE));
					}

					marketPlaces.put(name, prices.entrySet());
				}
			}

			scopes.put(MARKETPLACE, marketPlaces.entrySet());
		}
	}

	private static void putPrices(JSONObject productJson, Map<String, Object> scopes) {
		if (productJson.has(PRICES) && !productJson.isNull(PRICES)) {
			JSONObject pricesJson = new JSONObject(productJson.getString(PRICES));
			Map<Object, Object> pricesMap = getMapPricesFromJson(pricesJson);

			if (pricesJson.has(BANK_TICKET)) {
				JSONObject bankTicket = pricesJson.getJSONObject(BANK_TICKET);

				if (bankTicket.has("1")) {
					scopes.put(BANK_TICKET, bankTicket.get("1"));
				}
			}

			if (pricesJson.has(FROM)) {
				scopes.put(FROM, pricesJson.get(FROM));
			}

			scopes.put(PRICES, pricesMap.entrySet());
		}
	}


	@SuppressWarnings("unchecked")
	private static Map<Object, Object> getMapPricesFromJson(JSONObject prices) {
		Map<Object, Object> pricesMap = new HashMap<>();

		if (prices.has("card")) {
			JSONObject cardJson = prices.getJSONObject("card");
			Set<String> cards = cardJson.keySet();

			for (String card : cards) {
				Map<Object, Object> cardPrices = new HashMap<>();
				JSONObject installmentsCard = cardJson.getJSONObject(card);

				Set<String> installments = installmentsCard.keySet();

				for (String installment : installments) {
					cardPrices.put(installment, installmentsCard.get(installment));
				}

				pricesMap.put(card, cardPrices.entrySet());
			}
		}

		return pricesMap;
	}
}
