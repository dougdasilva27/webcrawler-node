package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static models.pricing.Pricing.PricingBuilder;

public class MexicoSamsclubCrawler extends Crawler {
	private static final String IMAGE_URL = "www.sams.com.mx";

	private static final String SELLER_FULL_NAME = "Sams Club";

	protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
			Card.AMEX.toString());

	public MexicoSamsclubCrawler(Session session) {
		super(session);
	}

	@Override
	protected Object fetch() {
		String url = session.getOriginalURL();

		if (url.contains("?")) {
			url = url.split("\\?")[0];
		}

		String finalParameter = CommonMethods.getLast(url.split("/"));

		if (finalParameter.contains("_")) {
			finalParameter = CommonMethods.getLast(finalParameter.split("_")).trim();
		}

		String apiUrl = "https://www.sams.com.mx/rest/model/atg/commerce/catalog/ProductCatalogActor/getSkuSummaryDetails?storeId=0000009999&upc="
				+ finalParameter + "&skuId=" + finalParameter;

		Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
		return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
	}

	@Override
	public List<Product> extractInformation(JSONObject apiJson) throws Exception {
		super.extractInformation(apiJson);
		List<Product> products = new ArrayList<>();

		if (apiJson.has("skuId")) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject imagesJson = JSONUtils.getJSONValue(JSONUtils.getJSONValue(apiJson, "sku"), "auxiliaryMedia");

			String internalId = crawlInternalId(apiJson);
			String name = crawlName(apiJson);
			CategoryCollection categories = crawlCategories(apiJson);
			String primaryImage = crawlPrimaryImage(imagesJson);
			String secondaryImages = crawlSecondaryImages(imagesJson, primaryImage);
			String description = crawlDescription(apiJson);
			Integer stock = null;

			List<String> eans = new ArrayList<>();
			eans.add(internalId);

			Offers offers = scrapOffers(apiJson);


			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setName(name)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setEans(eans)
					.setOffers(offers)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;

	}

	private Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
		Offers offers = new Offers();
		List<String> sales = scrapSales(json);
		Pricing pricing = scrapPricing(json);

		offers.add(OfferBuilder.create()
				.setUseSlugNameAsInternalSellerId(true)
				.setSellerFullName(SELLER_FULL_NAME)
				.setMainPagePosition(1)
				.setIsBuybox(false)
				.setIsMainRetailer(true)
				.setPricing(pricing)
				.setSales(sales)
				.build());

		return offers;
	}

	private String crawlInternalId(JSONObject apiJson) {
		String internalId = null;

		if (apiJson.has("skuId")) {
			internalId = apiJson.getString("skuId");
		}

		return internalId;
	}

	private String crawlName(JSONObject apiJson) {
		StringBuilder name = new StringBuilder();

		if (apiJson.has("sku") && !apiJson.isNull("sku")) {
			JSONObject sku = apiJson.getJSONObject("sku");

			if (sku.has("displayName") && !sku.isNull("displayName")) {
				name.append(sku.getString("displayName").trim());
			}

			if (sku.has("brand") && !sku.isNull("brand")) {
				name.append(" ");
				name.append(sku.getString("brand").trim());
			}

			if (sku.has("shopTicketDesc") && !sku.isNull("shopTicketDesc")) {
				name.append(" ");
				name.append(sku.getString("shopTicketDesc").trim());
			}
		}

		return name.toString();
	}

	private boolean crawlAvailability(JSONObject apiJson) {
		boolean available = false;

		if (apiJson.has("status")) {
			String status = apiJson.getString("status");

			available = status.equalsIgnoreCase("SELLABLE");
		}

		return available;
	}

	private String crawlPrimaryImage(JSONObject json) {
		String primaryImage = null;

		if (json.has("MEDIUM") && json.get("MEDIUM") instanceof JSONObject) {
			json = json.getJSONObject("MEDIUM");

			if (json.has("url") && json.get("url") instanceof String) {
				primaryImage = CrawlerUtils.completeUrl(json.getString("url"), "https:", IMAGE_URL);
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(JSONObject json, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		for (String key : json.keySet()) {
			if (key.contains("MEDIUM") && json.get(key) instanceof JSONObject) {
				JSONObject imgJson = json.getJSONObject(key);

				if (imgJson.has("url") && imgJson.get("url") instanceof String) {
					String secondaryImage = CrawlerUtils.completeUrl(imgJson.getString("url"), "https:", IMAGE_URL);

					if (!secondaryImage.equals(primaryImage)) {
						secondaryImagesArray.put(secondaryImage);
					}
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(JSONObject apiJson) {
		CategoryCollection categories = new CategoryCollection();

		if (apiJson.has("breadcrumb") && apiJson.get("breadcrumb") instanceof JSONObject) {
			JSONObject breadcrumb = apiJson.getJSONObject("breadcrumb");

			if (breadcrumb.has("departmentName")) {
				categories.add(breadcrumb.get("departmentName").toString());
			}

			if (breadcrumb.has("familyName")) {
				categories.add(breadcrumb.get("familyName").toString());
			}

			if (breadcrumb.has("fineLineName")) {
				categories.add(breadcrumb.get("fineLineName").toString());
			}
		}

		return categories;
	}

	private String crawlDescription(JSONObject apiJson) {
		StringBuilder description = new StringBuilder();

		if (apiJson.has("longDescription")) {
			description.append("<div id=\"desc\"> <h3> Descripción </h3>");
			description.append(apiJson.get("longDescription") + "</div>");
		}

		StringBuilder nutritionalTable = new StringBuilder();
		StringBuilder caracteristicas = new StringBuilder();

		if (apiJson.has("attributesMap")) {
			JSONObject attributesMap = apiJson.getJSONObject("attributesMap");

			for (String key : attributesMap.keySet()) {
				if (attributesMap.get(key) instanceof JSONObject) {
					JSONObject attribute = attributesMap.getJSONObject(key);

					if (attribute.has("attrGroupId")) {
						JSONObject attrGroupId = attribute.getJSONObject("attrGroupId");

						if (attrGroupId.has("optionValue")) {
							String optionValue = attrGroupId.getString("optionValue");

							if (optionValue.equalsIgnoreCase("Tabla nutrimental")) {
								setAPIDescription(attribute, nutritionalTable);
							} else if (optionValue.equalsIgnoreCase("Caracterisitcas")) {
								setAPIDescription(attribute, caracteristicas);
							}
						}
					}
				} else if (attributesMap.get(key) instanceof JSONArray) {
					JSONArray attribute = attributesMap.getJSONArray(key);
					for (Object object : attribute) {
						JSONObject attrMap = (JSONObject) object;
						setAPIDescription(attrMap, caracteristicas);
					}
				}
			}
		}

		if (!nutritionalTable.toString().isEmpty()) {
			description.append("<div id=\"table\"> <h3> Nutrición </h3>");
			description.append(nutritionalTable + "</div>");
		}

		if (!caracteristicas.toString().isEmpty()) {
			description.append("<div id=\"short\"> <h3> Características </h3>");
			description.append(caracteristicas + "</div>");
		}

		return description.toString();
	}

	private void setAPIDescription(JSONObject attributesMap, StringBuilder desc) {
		if (attributesMap.has("attrDesc") && attributesMap.has("value")) {
			desc.append("<div>");
			desc.append("<span float=\"left\">" + attributesMap.get("attrDesc") + "&nbsp </span>");
			desc.append("<span float=\"right\">" + attributesMap.get("value") + " </span>");
			desc.append("</div>");
		}
	}

	private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
		CreditCards creditCards = new CreditCards();
		Installments installments = scrapInstallment(spotlightPrice);

		for (String card : cards) {
			creditCards.add(CreditCard.CreditCardBuilder.create()
					.setBrand(card)
					.setInstallments(installments)
					.setIsShopCard(true)
					.build());
		}

		return creditCards;

	}

	private Installments scrapInstallment(Double spotlightPrice) throws MalformedPricingException {
		Installments installments = new Installments();

		installments.add(InstallmentBuilder.create()
				.setInstallmentNumber(1)
				.setInstallmentPrice(spotlightPrice)
				.build());

		return installments;
	}

	private List<String> scrapSales(JSONObject json) {
		List<String> sales = new ArrayList<>();
		JSONArray salesJson = json.optJSONArray("ItemPromotions");
		if (salesJson != null) {
			for (Object promo : salesJson) {
				if (promo instanceof String) {
					sales.add((String) promo);
				}
			}
		}
		return sales;
	}

	private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
		Double price = json.optDouble("specialPrice", 0D);
		Double priceFrom = json.optDouble("basePrice");
		if (price == 0D) {
			price = priceFrom;
			priceFrom = null;
		}

		CreditCards creditCards = scrapCreditCards(price);

		return PricingBuilder.create()
				.setSpotlightPrice(price)
				.setPriceFrom(priceFrom)
				.setCreditCards(creditCards)
				.build();
	}
}
