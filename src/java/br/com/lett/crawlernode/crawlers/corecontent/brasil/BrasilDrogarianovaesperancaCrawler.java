package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
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

/**
 * Date: 08/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogarianovaesperancaCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.drogarianovaesperanca.com.br";

	public BrasilDrogarianovaesperancaCrawler(Session session) {
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

			String internalId = crawlInternalId(doc);
			String internalPid = null;
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, internalId);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

			// Creating the product
			Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
					.setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
					.setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
					.setDescription(description).setStock(stock).setMarketplace(marketplace).build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("#ID_SubProduto").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select("#ID_SubProduto").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1 span[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select("span[itemprop=price]").first();

		if (salePriceElement != null) {
			priceText = salePriceElement.text();
			price = MathCommonsMethods.parseFloat(priceText);
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select("#imgProduto").first();

		if (elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("src");
		}

		return primaryImage;
	}

	/**
	 * In the time when this crawler was made, this market hasn't secondary Images
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements images = doc.select(".more-views-list > li:not([class=active]) a");

		for (Element e : images) {
			secondaryImagesArray.put(e.attr("href"));
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select("span[property=itemListElement] > a span");

		for (int i = 1; i < elementCategories.size(); i++) {
			String cat = elementCategories.get(i).ownText().replace("/", "").trim();

			if (!cat.isEmpty()) {
				categories.add(cat);
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();

		Element elementDescription = doc.select(".ficha-produto").first();

		if (elementDescription != null) {
			description.append(elementDescription.html());
		}

		Element elementInfo = doc.select(".tabs-produto #tabs").first();

		if (elementInfo != null) {
			description.append(elementInfo.html());
		}

		Element aviso = doc.select(".aviso-medicamento").first();

		if (aviso != null) {
			description.append(aviso.html());
		}

		return description.toString();
	}

	private boolean crawlAvailability(Document doc) {
		return doc.select("#BtComprarProduto").first() != null;
	}

	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, String internalId) {
		Prices prices = new Prices();

		if (price != null) {
			String pricesUrl =
					"https://www.drogarianovaesperanca.com.br/Funcoes_Ajax.aspx/CarregaFormaPagamento";
			String payload = "{\"ID_OpcaoPagamento\":0,\"ID_FormaPagamento\":0,\"ID_SubProduto\":\""
					+ internalId + "\"}";

			Map<String, String> headers = new HashMap<>();
			headers.put("content-type", "application/json");

			String json =
					POSTFetcher.fetchPagePOSTWithHeaders(pricesUrl, session, payload, cookies, 1, headers);

			try {
				JSONObject pricesJson = new JSONObject(json);

				if (pricesJson.has("d")) {
					JSONArray cards = pricesJson.getJSONArray("d");

					for (int i = 0; i < cards.length() - 1; i++) {
						JSONObject card = cards.getJSONObject(i);
						String cardName = crawlCardName(card);

						if (cardName != null && card.has("Itens")) {
							setInstallments(cardName, card, prices);
						}
					}

				}

			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}

		}

		return prices;
	}

	private String crawlCardName(JSONObject card) {
		String officalCardName = null;

		if (card.has("Icon")) {
			String cardName = card.getString("Icon").trim();

			switch (cardName) {
				case "visa":
					officalCardName = Card.VISA.toString();
					break;
				case "mastercard":
					officalCardName = Card.MASTERCARD.toString();
					break;
				case "diners":
					officalCardName = Card.DINERS.toString();
					break;
				case "american-express":
					officalCardName = Card.AMEX.toString();
					break;
				case "elo":
					officalCardName = Card.ELO.toString();
					break;
				case "boleto-bancario":
					officalCardName = "boleto";
					break;
				default:
					break;
			}
		}

		return officalCardName;
	}

	private void setInstallments(String cardName, JSONObject card, Prices prices) {
		JSONArray installments = card.getJSONArray("Itens");

		if (cardName.equals("boleto") && installments.length() > 0) {
			prices.setBankTicketPrice(installments.getJSONObject(0).getDouble("TotalGeral"));
		} else {
			Map<Integer, Float> installmentPriceMap = new HashMap<>();

			for (int i = 0; i < installments.length(); i++) {
				JSONObject installmentJson = installments.getJSONObject(i);

				if (installmentJson.has("Nparcel")) {
					Integer installment = installmentJson.getInt("Nparcel");

					if (installmentJson.has("TotalParcela")) {
						String parcelText = installmentJson.getString("TotalParcela").trim();
						Float value = parcelText.isEmpty() ? null : MathCommonsMethods.parseFloat(parcelText);

						if (value != null) {
							installmentPriceMap.put(installment, value);
						}
					}
				}
			}

			prices.insertCardInstallment(cardName, installmentPriceMap);
		}
	}
}
