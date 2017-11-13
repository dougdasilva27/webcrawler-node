package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

public class BrasilWebcontinentalCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.webcontinental.com.br";

	public BrasilWebcontinentalCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(session.getOriginalURL())) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			// Pid
			String internalPid = crawlInternalPid(doc);

			JSONObject productInformation = fetchJsonProduct(internalPid);

			// Primary image
			String primaryImage = crawlPrimaryImage(productInformation);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(productInformation);

			// Categories
			CategoryCollection categories = crawlCategories(productInformation);

			// Description
			String description = crawlDescription(productInformation);

			if (productInformation.has("skus")) {
				// sku data in json
				JSONArray arraySkus = productInformation.getJSONArray("skus");

				for (int i = 0; i < arraySkus.length(); i++) {
					JSONObject jsonSku = arraySkus.getJSONObject(i);

					// InternalId
					String internalId = crawlInternalId(jsonSku);

					// ARRAY Marketplace
					Marketplace marketplace = new Marketplace();

					// Price
					Float price = crawlPrice(jsonSku);

					// Availability
					boolean available = crawlAvailability(price);

					// Name
					String name = crawlName(productInformation, jsonSku);

					// Prices
					Prices prices = crawlPrices(jsonSku, price);

					// Creating the product
					Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
							.setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
							.setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
							.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
							.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
							.setDescription(description).setStock(null).setMarketplace(marketplace).build();

					products.add(product);
				}
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if (url.contains("/product/") && url.startsWith(HOME_PAGE)) {
			return true;
		}
		return false;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if (json.has("skuId")) {
			internalId = json.getString("skuId");
		}

		return internalId;
	}


	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Elements scripts = doc.select("script[type=\"text/javascript\"]");
		String token = "varrequire=";

		for (Element e : scripts) {
			String script = e.outerHtml().replaceAll(" ", "");

			if (script.contains(token)) {
				int x = script.indexOf(token) + token.length();
				int y = script.indexOf(";", x);

				String json = script.substring(x, y);

				try {
					JSONObject require = new JSONObject(json);

					if (require.has("config")) {
						JSONObject config = require.getJSONObject("config");

						if (config.has("ccNavState")) {
							JSONObject product = config.getJSONObject("ccNavState");

							if (product.has("pageContext")) {
								internalPid = product.getString("pageContext");
							}
						}
					}

				} catch (JSONException ex) {
					Logging.printLogError(logger, CommonMethods.getStackTrace(ex));
				}

				break;
			}
		}

		return internalPid;
	}

	private String crawlName(JSONObject infoProduct, JSONObject sku) {
		String name = null;

		if (infoProduct.has("name")) {
			name = infoProduct.getString("name").replace("'", "").replace("’", "").trim();
		}

		if (sku.has("skuName")) {
			name += " " + sku.getString("skuName");
		}

		return name;
	}

	private Float crawlPrice(JSONObject sku) {
		Float price = null;

		if (sku.has("prices")) {
			JSONObject prices = sku.getJSONObject("prices");

			if (prices.has("realaPrazo")) {
				Double priceDouble = prices.getDouble("realaPrazo");
				price = MathCommonsMethods.normalizeTwoDecimalPlaces(priceDouble.floatValue());
			}
		}

		return price;
	}

	private boolean crawlAvailability(Float price) {
		if (price != null) {
			return true;
		}
		return false;
	}

	private String crawlPrimaryImage(JSONObject productInfo) {
		String primaryImage = null;

		if (productInfo.has("primaryImage")) {
			primaryImage = productInfo.getString("primaryImage");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject productInfo) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();


		if (productInfo.has("secondaryImages")) {
			JSONArray secondaryArray = productInfo.getJSONArray("secondaryImages");

			for (int i = 0; i < secondaryArray.length(); i++) {
				String image = secondaryArray.getString(i);

				if (image.startsWith(HOME_PAGE)) {
					secondaryImagesArray.put(image);
				} else {
					secondaryImagesArray.put(HOME_PAGE + image);
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(JSONObject productInfo) {
		CategoryCollection categories = new CategoryCollection();

		if (productInfo.has("categories")) {
			JSONArray catArray = productInfo.getJSONArray("categories");

			if (catArray.length() > 0) {
				String url = "https://mid.webcontinental.com.br/webservice/breadcrumb/"
						+ CommonMethods.removeAccents(catArray.getString(0)).replace(" ", "-").toLowerCase()
								.replaceAll("[^a-z0-9]+", "-");

				JSONArray cats =
						DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);

				for (int i = 1; i < cats.length(); i++) {
					JSONObject obj = cats.getJSONObject(i);

					if (obj.has("displayName")) {
						try {
							categories.add(URLDecoder.decode(obj.getString("displayName"), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
						}
					}
				}
			}

		}

		return categories;
	}


	private String crawlDescription(JSONObject productInfo) {
		String description = "";

		if (productInfo.has("description")) {
			description = productInfo.getString("description");
		}

		return description;
	}

	private Prices crawlPrices(JSONObject sku, Float price) {
		Prices p = new Prices();

		if (sku.has("prices")) {
			JSONObject pricesJson = sku.getJSONObject("prices");

			if (pricesJson.has("realaVista")) {
				Double boleto = pricesJson.getDouble("realaVista");

				p.setBankTicketPrice(boleto);

				Map<Integer, Float> installments = new HashMap<>();
				installments.put(1, price);

				p.insertCardInstallment(Card.AMEX.toString(), installments);
				p.insertCardInstallment(Card.VISA.toString(), installments);
				p.insertCardInstallment(Card.MASTERCARD.toString(), installments);
				p.insertCardInstallment(Card.DINERS.toString(), installments);
				p.insertCardInstallment(Card.HIPERCARD.toString(), installments);
				p.insertCardInstallment(Card.ELO.toString(), installments);
			}
		}

		return p;
	}

	/**
	 * This json must be found in
	 * https://www.webcontinental.com.br/ccstoreui/v1/pages/product?pageParam=26131&dataOnly=false&pageId=product&cacheableDataOnly=true
	 * 
	 * @param internalPid
	 * @return
	 */
	private JSONObject fetchJsonProduct(String internalPid) {
		if (internalPid != null) {
			String url = "https://www.webcontinental.com.br/ccstoreui/v1/pages/product?" + "pageParam="
					+ internalPid + "&dataOnly=false&pageId=product&cacheableDataOnly=true";

			JSONObject api =
					DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

			return sanityzeJsonAPI(api);
		}

		return new JSONObject();
	}

	private JSONObject sanityzeJsonAPI(JSONObject api) {
		JSONObject product = new JSONObject();

		if (api.has("data")) {
			JSONObject data = api.getJSONObject("data");

			if (data.has("page")) {
				JSONObject page = data.getJSONObject("page");

				if (page.has("product")) {
					JSONObject prod = page.getJSONObject("product");

					if (prod.has("displayName")) {
						product.put("name", prod.getString("displayName"));
					}

					computeImages(prod, product);
					computeDescription(prod, product);
					computeCategories(prod, product);
					computeSkus(prod, product);
				}
			}
		}

		return product;
	}

	private void computeSkus(JSONObject prod, JSONObject product) {
		JSONArray products = new JSONArray();

		if (prod.has("childSKUs")) {
			JSONArray skus = prod.getJSONArray("childSKUs");

			for (int i = 0; i < skus.length(); i++) {
				JSONObject child = new JSONObject();
				JSONObject sku = skus.getJSONObject(i);

				if (sku.has("repositoryId")) {
					child.put("skuId", sku.getString("repositoryId"));
				}

				if (sku.has("voltagem")) {
					child.put("skuName", sku.getString("voltagem"));
				}

				if (sku.has("salePrices")) {
					child.put("prices", sku.getJSONObject("salePrices"));
				}

				products.put(child);
			}
		}

		product.put("skus", products);
	}

	private void computeImages(JSONObject prod, JSONObject product) {
		if (prod.has("primaryFullImageURL")) {
			String image = prod.getString("primaryFullImageURL");

			if (image.startsWith(HOME_PAGE)) {
				product.put("primaryImage", image);
			} else {
				product.put("primaryImage", HOME_PAGE + image);
			}
		}

		if (prod.has("fullImageURLs")) {
			product.put("secondaryImages", prod.getJSONArray("fullImageURLs"));
		}
	}

	private void computeDescription(JSONObject prod, JSONObject product) {
		String descriptions = "";

		if (prod.has("mobileDescription") && prod.get("mobileDescription") instanceof String) {
			descriptions += prod.getString("mobileDescription");
		}

		if (prod.has("description") && prod.get("description") instanceof String) {
			descriptions += prod.getString("description");
		}

		if (prod.has("longDescription") && prod.get("longDescription") instanceof String) {
			descriptions += prod.getString("longDescription");
		}

		product.put("description", descriptions);
	}

	private void computeCategories(JSONObject prod, JSONObject product) {
		JSONArray categories = new JSONArray();

		if (prod.has("parentCategories")) {
			JSONArray cat = prod.getJSONArray("parentCategories");

			for (int i = 0; i < cat.length(); i++) {
				JSONObject obj = cat.getJSONObject(i);

				if (obj.has("displayName")) {
					categories.put(obj.getString("displayName"));
				}
			}
		}

		product.put("categories", categories);
	}
}
