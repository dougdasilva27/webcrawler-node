package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 22/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaFarmacityCrawler extends Crawler {

	private static final String HOME_PAGE = "https://www.farmacity.com/";

	public ArgentinaFarmacityCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(doc)) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			JSONObject productJson = crawlProductJson(doc);
			String internalPid = crawlInternalPid(productJson);
			CategoryCollection categories = crawlCategories(doc);
			String description = crawlDescription(doc);
			Integer stock = null;

			JSONArray arraySkus =
					productJson.has("variants") ? productJson.getJSONArray("variants") : new JSONArray();

			for (int i = 0; i < arraySkus.length(); i++) {
				JSONObject jsonSku = arraySkus.getJSONObject(i);

				String internalId = crawlInternalId(jsonSku);
				String name = crawlName(jsonSku);
				boolean available = crawlAvailability(jsonSku);
				Float price = crawlMainPagePrice(jsonSku, available);
				String primaryImage = crawlPrimaryImage(jsonSku);
				String secondaryImages = crawlSecondaryImages(jsonSku);
				Prices prices = crawlPrices(price);

				// Creating the product
				Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
						.setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
						.setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
						.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
						.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
						.setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

				products.add(product);
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document doc) {
		return doc.select(".main-title > a").first() != null;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(JSONObject json) {
		String internalId = null;

		if (json.has("sku")) {
			internalId = Integer.toString(json.getInt("sku")).trim();
		}

		return internalId;
	}


	private String crawlInternalPid(JSONObject json) {
		String internalPid = null;

		if (json.has("id")) {
			internalPid = json.get("id").toString();
		}

		return internalPid;
	}

	private String crawlName(JSONObject jsonSku) {
		StringBuilder name = new StringBuilder();

		if (jsonSku.has("name")) {
			name.append(jsonSku.getString("name"));

			if (jsonSku.has("brand")) {
				JSONObject brand = jsonSku.getJSONObject("brand");

				if (brand.has("name")) {
					name.append(" " + brand.getString("name"));
				}
			}

			if (jsonSku.has("option_values")) {
				JSONArray properties = jsonSku.getJSONArray("option_values");

				for (int i = 0; i < properties.length(); i++) {
					JSONObject p = properties.getJSONObject(i);

					if (p.has("presentation")) {
						name.append(" " + p.get("presentation"));
					}
				}
			}
		}


		return name.toString();
	}

	private Float crawlMainPagePrice(JSONObject json, boolean available) {
		Float price = null;

		if (json.has("price") && available) {
			price = Float.parseFloat(json.getString("price"));
		}

		return price;
	}

	private boolean crawlAvailability(JSONObject json) {
		if (json.has("in_stock")) {
			return json.getBoolean("in_stock");
		}
		return false;
	}

	private String crawlPrimaryImage(JSONObject skuJson) {
		String primaryImage = null;

		if (skuJson.has("images") && skuJson.getJSONArray("images").length() > 0) {
			JSONObject image = skuJson.getJSONArray("images").getJSONObject(0);

			if (image.has("gallery_large_url")
					&& image.get("gallery_large_url").toString().startsWith("http")) {
				primaryImage = image.get("gallery_large_url").toString();
			} else if (image.has("gallery_small_url")
					&& image.get("gallery_small_url").toString().startsWith("http")) {
				primaryImage = image.get("gallery_small_url").toString();
			} else if (image.has("gallery_thumb_url")
					&& image.get("gallery_thumb_url").toString().startsWith("http")) {
				primaryImage = image.get("gallery_thumb_url").toString();
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject skuJson) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		if (skuJson.has("images") && skuJson.getJSONArray("images").length() > 1) {
			JSONArray images = skuJson.getJSONArray("images");

			for (int i = 1; i < images.length(); i++) { // starts with index 1, because the first image is
																									// the primary image
				JSONObject image = images.getJSONObject(i);

				if (image.has("gallery_large_url")
						&& image.get("gallery_large_url").toString().startsWith("http")) {
					secondaryImagesArray.put(image.get("gallery_large_url").toString());
				} else if (image.has("gallery_small_url")
						&& image.get("gallery_small_url").toString().startsWith("http")) {
					secondaryImagesArray.put(image.get("gallery_small_url").toString());
				} else if (image.has("gallery_thumb_url")
						&& image.get("gallery_thumb_url").toString().startsWith("http")) {
					secondaryImagesArray.put(image.get("gallery_thumb_url").toString());
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumb li[itemprop=child] a span");

		for (Element e : elementCategories) {
			String cat = e.ownText().trim();

			if (!cat.isEmpty()) {
				categories.add(cat);
			}
		}

		return categories;
	}


	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();

		Element elementShortdescription = doc.select("#tech-detail").first();

		if (elementShortdescription != null) {
			description.append(elementShortdescription.html());
		}

		Element elementDescription = doc.select(".product-description-container").first();

		if (elementDescription != null) {
			description.append(elementDescription.html());
		}

		return description.toString();
	}

	/**
	 * There is no bank slip payment method Has no informations of installments
	 * 
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price) {
		Prices prices = new Prices();

		if (price != null) {
			Map<Integer, Float> mapInstallments = new HashMap<>();
			mapInstallments.put(1, price);

			prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
			prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
			prices.insertCardInstallment(Card.NARANJA.toString(), mapInstallments);
			prices.insertCardInstallment(Card.NATIVA.toString(), mapInstallments);
			prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);

		}

		return prices;
	}

	/**
	 * Get the script having a json with the availability information
	 * 
	 * @return
	 */
	private JSONObject crawlProductJson(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = new JSONObject();

		for (Element tag : scriptTags) {
			for (DataNode node : tag.dataNodes()) {
				if (tag.html().trim().startsWith("var data = ")) {
					skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var data = "))[1]
							+ node.getWholeData().split(Pattern.quote("var data = "))[1]
									.split(Pattern.quote("};"))[0]);
				}
			}
		}

		if (skuJson.has("product") && skuJson.getJSONObject("product").has("variants")) {
			return skuJson.getJSONObject("product");
		}

		return new JSONObject();
	}
}
