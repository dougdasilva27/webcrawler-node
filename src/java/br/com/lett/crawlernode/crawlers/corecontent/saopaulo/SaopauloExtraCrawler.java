package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloExtraCrawler extends Crawler {

	public SaopauloExtraCrawler(Session session) {
		super(session);
	}


	private static final String HOME_PAGE = "https://www.deliveryextra.com.br";
	private static final String HOME_PAGE_HTTP = "http://www.deliveryextra.com.br";


	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	// Loja 241 sp
	private static final String STORE_ID = "241";

	@Override
	public void handleCookiesBeforeFetch() {

		// Criando cookie da loja 501 = São Paulo capital
		BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", STORE_ID);
		cookie.setDomain(".deliveryextra.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);

	}


	@Override
	protected Object fetch() {
		JSONObject productsInfo = new JSONObject();

		String productUrl = session.getOriginalURL();

		String id;
		if (productUrl.startsWith(HOME_PAGE)) {
			id = productUrl.replace(HOME_PAGE, "").split("/")[2];
		} else {
			id = productUrl.replace(HOME_PAGE_HTTP, "").split("/")[2];
		}

		String url = "https://api.gpa.digital/ex/products/" + id + "?storeId=" + STORE_ID
				+ "&isClienteMais=false";

		String res = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

		try {
			JSONObject apiGPA = new JSONObject(res);
			if (apiGPA.has("content")) {
				productsInfo = apiGPA.getJSONObject("content");
			}
		} catch (JSONException e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
		}

		return productsInfo;
	}

	@Override
	public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
		super.extractInformation(jsonSku);
		List<Product> products = new ArrayList<>();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(jsonSku);
			String internalPid = crawlInternalPid(jsonSku);
			CategoryCollection categories = crawlCategories(jsonSku);
			String description = crawlDescription(jsonSku);
			boolean available = crawlAvailability(jsonSku);
			Float price = crawlPrice(jsonSku);
			String primaryImage = crawlPrimaryImage(jsonSku);
			String name = crawlName(jsonSku);
			String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
			Prices prices = crawlPrices(price);
			Integer stock = null;

			// Creating the product
			Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
					.setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
					.setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
					.setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

			products.add(product);


		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if (url.contains("deliveryextra.com.br/produto/")) {
			return true;
		}
		return false;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if (json.has("id")) {
			internalId = json.get("id").toString();
		}

		return internalId;
	}


	private String crawlInternalPid(JSONObject json) {
		String internalPid = null;

		if (json.has("id")) {
			internalPid = json.getString("sku");
		}

		return internalPid;
	}

	private String crawlName(JSONObject json) {
		String name = null;

		if (json.has("name")) {
			name = json.getString("name");
		}

		return name;
	}

	private Float crawlPrice(JSONObject json) {
		Float price = null;

		if (json.has("currentPrice")) {
			Object pObj = json.get("currentPrice");

			if (pObj instanceof Double) {
				price = MathCommonsMethods.normalizeTwoDecimalPlaces(((Double) pObj).floatValue());
			}
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject json) {
		if (json.has("stock") && json.getBoolean("stock")) {
			return true;
		}

		return false;
	}

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;

		if (json.has("mapOfImages")) {
			JSONObject images = json.getJSONObject("mapOfImages");

			for (int i = 0; i < images.length(); i++) {
				if (images.length() > 0 && images.has(Integer.toString(i))) {
					JSONObject imageObj = images.getJSONObject(Integer.toString(i));

					if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("BIG");

						if (image.contains("img")) {
							primaryImage = HOME_PAGE + imageObj.getString("BIG");
						}
					} else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("MEDIUM");;

						if (image.contains("img")) {
							primaryImage = HOME_PAGE + imageObj.getString("MEDIUM");
						}
					} else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("SMALL");;

						if (image.contains("img")) {
							primaryImage = HOME_PAGE + imageObj.getString("SMALL");
						}
					}
				}

				if (primaryImage != null) {
					break;
				}
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject json, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		String primaryImageId = getImageId(primaryImage);

		if (json.has("mapOfImages")) {
			JSONObject images = json.getJSONObject("mapOfImages");

			for (int i = 1; i < images.length(); i++) { // index 0 may be a primary Image
				if (images.length() > 0 && images.has(Integer.toString(i))) {
					JSONObject imageObj = images.getJSONObject(Integer.toString(i));

					if (imageObj.has("BIG") && !imageObj.getString("BIG").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("BIG");
						String imageId = getImageId(image);

						if (image.contains("img") && !imageId.equals(primaryImageId)) {
							secondaryImagesArray.put(HOME_PAGE + imageObj.getString("BIG"));
						}
					} else if (imageObj.has("MEDIUM") && !imageObj.getString("MEDIUM").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("MEDIUM");
						String imageId = getImageId(image);

						if (image.contains("img") && !imageId.equals(primaryImageId)) {
							secondaryImagesArray.put(HOME_PAGE + imageObj.getString("MEDIUM"));
						}
					} else if (imageObj.has("SMALL") && !imageObj.getString("SMALL").isEmpty()) {
						String image = HOME_PAGE + imageObj.getString("SMALL");
						String imageId = getImageId(image);

						if (image.contains("img") && !imageId.equals(primaryImageId)) {
							secondaryImagesArray.put(HOME_PAGE + imageObj.getString("SMALL"));
						}
					}
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private String getImageId(String imageUrl) {
		if (imageUrl != null) {
			return imageUrl.replace(HOME_PAGE, "").split("/")[4];
		}

		return null;
	}

	private CategoryCollection crawlCategories(JSONObject json) {
		CategoryCollection categories = new CategoryCollection();

		if (json.has("shelfList")) {
			JSONArray shelfList = json.getJSONArray("shelfList");

			List<String> listCategories = new ArrayList<>(); // It is a "set" because it has been noticed
																												// that there are repeated categories

			for (int i = shelfList.length() - 1; i >= 0; i--) { // the last item is the first category and
																													// the first item is the last category
				JSONObject cat = shelfList.getJSONObject(i);

				if (cat.has("name") && !listCategories.contains(cat.getString("name"))) {
					listCategories.add(cat.getString("name"));
				}
			}

			for (String category : listCategories) {
				categories.add(category);
			}
		}

		return categories;
	}


	private String crawlDescription(JSONObject json) {
		String description = "";

		if (json.has("shortDescription")) {
			if (json.get("shortDescription") instanceof String) {
				description += json.getString("shortDescription");
			}
		}

		// This key in json has a map of attributes -> {label: "", value = ""} , For crawl niutritional
		// table we make the html and put the values in html
		if (json.has("nutritionalMap") && json.getJSONObject("nutritionalMap").length() > 0) {
			JSONObject nutritionalJson = json.getJSONObject("nutritionalMap");

			StringBuilder str = new StringBuilder();

			str.append("<div class=\"product-nutritional-table\">\n"
					+ "	<p class=\"title\">Tabela nutricional</p>\n"
					+ "	<!-- ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->"
					+ "<div class=\"main-infos ng-scope\" ng-if=\"productDetailCtrl.product.nutritionalMap.cabecalho\">\n"
					+ "			<p ng-bind-html=\"productDetailCtrl.product.nutritionalMap.cabecalho || "
					+ "productDetailCtrl.product.nutritionalMap.cabecalho.value\" class=\"ng-binding\"></p>\n"
					+ "		</div><!-- end ngIf: productDetailCtrl.product.nutritionalMap.cabecalho -->\n"
					+ "		<table class=\"table table-responsive\">\n" + "			<thead>\n" + "				<tr>\n"
					+ "					<th>Item</th>\n" + "					<th>Quantidade por porção</th>\n"
					+ "					<th>Valores diários</th>\n" + "				</tr>\n" + "			</thead>\n");
			str.append(crawlNutritionalTableAttributes(nutritionalJson));
			str.append("</table>\n</div>");

			description += str.toString();
		}

		return description;
	}

	private String crawlNutritionalTableAttributes(JSONObject nutritionalMap) {
		StringBuilder str = new StringBuilder();
		str.append("<tbody>");

		List<String> attributesList = Arrays.asList("valor_energetico", "carboidratos", "acucares",
				"proteinas", "gorduras_totais", "gorduras_saturadas", "gorduras_trans", "fibra_alimentar",
				"sodio", "rodape");

		for (String attribute : attributesList) {
			if (nutritionalMap.has(attribute)) {
				JSONObject attributeJson = nutritionalMap.getJSONObject(attribute);

				if (attributeJson.has("value") && attributeJson.has("label")) {
					str.append(
							putAttribute(attributeJson.getString("value"), attributeJson.getString("label")));
				}
			}
		}

		str.append("</tbody");
		return str.toString();
	}

	private String putAttribute(String value, String label) {
		if (label != null) {
			if (label.equalsIgnoreCase("rodape")) {
				return "<tfoot>\n" + "	<tr>\n"
						+ "		<td colspan=\"3\" ng-bind-html=\"productDetailCtrl.product.nutritionalMap.rodape.value\""
						+ "class=\"last ng-binding\">" + value + "</td>\n" + "	</tr>\n" + "</tfoot>\n";
			} else {
				return "	<tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 \" class=\"ng-scope\">\n"
						+ "		<td class=\"ng-binding\">" + label + "</td>\n" + "		<td class=\"ng-binding\">"
						+ value + "</td>\n" + "		<td class=\"ng-binding\"></td>\n"
						+ "	</tr><!-- end ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 --><!-- end ngRepeat: "
						+ "(key, item) in productDetailCtrl.product.nutritionalMap --><!-- ngIf: [ 'cabecalho', 'rodape'].indexOf(key) === -1 -->"
						+ "<tr ng-repeat=\"(key, item) in productDetailCtrl.product.nutritionalMap \" ng-if=\"[ 'cabecalho', 'rodape'].indexOf(key) === -1 "
						+ "\" class=\"ng-scope\">\n";
			}
		}

		return "";
	}

	/**
	 * In this site has no information of installments
	 * 
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price) {
		Prices p = new Prices();

		if (price != null) {
			Map<Integer, Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);

			p.setBankTicketPrice(price);

			p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

		}

		return p;
	}
}
