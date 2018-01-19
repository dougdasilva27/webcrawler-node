package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
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

/*****************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. 2) There is no stock information for skus in
 * this ecommerce by the time this crawler was made. 3) There is no marketplace in this ecommerce by
 * the time this crawler was made. 4) The sku page identification is done simply looking for an
 * specific html element. 5) if the sku is unavailable, it's price is not displayed. Yet the crawler
 * tries to crawl the price. 6) There is no internalPid for skus in this ecommerce. The internalPid
 * must be a number that is the same for all the variations of a given sku. 7) We have one method
 * for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples: ex1 (available):
 * https://www.dufrio.com.br/ar-condicionado-frio-split-cassete-inverter-35000-btus-220v-lg.html ex2
 * (unavailable):
 * https://www.dufrio.com.br/ar-condicionado-frio-split-piso-teto-36000-btus-220v-1-lg.html
 *
 ******************************************************************************************************************************/

public class BrasilDufrioCrawler extends Crawler {

	private static final String HOME_PAGE_HTTP = "http://www.dufrio.com.br/";
	private static final String HOME_PAGE_HTTPS = "https://www.dufrio.com.br/";

	private static final String USER_AGENT = DataFetcher.randUserAgent();
	private LettProxy proxyToBeUsed = null;

	public BrasilDufrioCrawler(Session session) {
		super(session);
	}

	@Override
	protected Object fetch() {
		return Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies,
				USER_AGENT, this.proxyToBeUsed, 1));
	}

	/**
	 * Esse market possui um verificador de javascript Esse verificador é uma função que monta o
	 * cookie e da um reload na página Com isso pegamos este script, trocamos o reload da página pelo
	 * retorno do cookie Para isso usamos o ScriptEngineManager para rodar o código javascript.
	 * 
	 * Para acessar o site deve se usar o cookie pego aqui e usar o mesmo user agent.
	 */
	@Override
	public void handleCookiesBeforeFetch() {
		Document doc = Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, USER_AGENT, null, 1));

		this.proxyToBeUsed = session.getRequestProxy(session.getOriginalURL());

		Element script = doc.select("script").first();

		if (script != null) {
			String eval = script.html().trim();

			if (eval.endsWith(";")) {
				int y = eval.indexOf(";}}") + 3;
				int x = eval.indexOf(';', y) + 1;

				String b = eval.substring(y, x);

				if (b.contains("(")) {
					int z = b.indexOf('(') + 1;
					int u = b.indexOf(')', z);

					String result = b.substring(z, u);

					eval = "var document = {};" + eval.replace(b, "") + " " + result + " = " + result
							+ ".replace(\"location.reload();\", \"\"); " + b;
				}
			}

			ScriptEngineManager factory = new ScriptEngineManager();
			ScriptEngine engine = factory.getEngineByName("js");
			try {
				String cookieString = engine.eval(eval).toString();
				if (cookieString != null && cookieString.contains("=")) {
					String cookieValues =
							cookieString.contains(";") ? cookieString.split(";")[0] : cookieString;

							String[] tokens = cookieValues.split("=");

							if (tokens.length > 1) {

								BasicClientCookie cookie = new BasicClientCookie(tokens[0], tokens[1]);
								cookie.setDomain("www.dufrio.com.br");
								cookie.setPath("/");
								this.cookies.add(cookie);

								BasicClientCookie cookie2 = new BasicClientCookie("newsletter", "Visitante");
								cookie2.setDomain("www.dufrio.com.br");
								cookie2.setPath("/");
								this.cookies.add(cookie2);
							}
				}

			} catch (ScriptException e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
			}
		}
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches()
				&& ((href.startsWith(HOME_PAGE_HTTPS)) || (href.startsWith(HOME_PAGE_HTTP)));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if (isProductPage(doc)) {
			Logging.printLogDebug(logger, session,
					"Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

			String productUrl = session.getOriginalURL();
			if (internalId != null && session.getRedirectedToURL(productUrl) != null) {
				productUrl = session.getRedirectedToURL(productUrl);
			}

			// Creating the product
			Product product = ProductBuilder.create().setUrl(productUrl).setInternalId(internalId)
					.setInternalPid(internalPid).setName(name).setPrice(price).setPrices(prices)
					.setAvailable(available).setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
					.setDescription(description).setStock(stock).setMarketplace(marketplace).build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select(".detalheProduto").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("div span[itemprop=mpn]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Elements scripts = doc.select("script");

		for (Element e : scripts) {
			String script = e.outerHtml();

			if (script.contains("dataLayer =")) {
				int x = script.indexOf('[') + 1;
				int y = script.indexOf("];", x) + 1;

				try {
					JSONObject datalayer = new JSONObject(script.substring(x, y));

					if (datalayer.has("google_tag_params")) {
						JSONObject google = datalayer.getJSONObject("google_tag_params");

						if (google.has("ecomm_prodid")) {
							internalPid = google.getString("ecomm_prodid").trim();
						}
					}
				} catch (Exception e1) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
				}

				break;
			}
		}

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".topoNome h1").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement = document.select(".boxComprarNew .boxComprarNew-t1").first();

		boolean oldPrice = false;

		if (salePriceElement != null) {
			String priceText = salePriceElement.text().toLowerCase();

			if (!priceText.contains("de")) {
				price = MathCommonsMethods.parseFloat(priceText);
			} else {
				oldPrice = true;
			}
		} else {
			oldPrice = true;
		}

		if (oldPrice) {
			salePriceElement = document.select(".boxComprarNew .boxComprarNew-t2").first();

			if (salePriceElement != null) {
				price = MathCommonsMethods.parseFloat(salePriceElement.text());
			}
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available;
		Element outOfStockElement = document.select("#aviseme").first();

		if (outOfStockElement != null) {
			available = false;
		} else {
			available = true;
		}

		return available;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".carouselPrincipal figure > a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".carouselPrincipal figure > a");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			String image = imagesElement.get(i).attr("href").trim();
			secondaryImagesArray.put(image);
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();

		Elements elementCategories = document.select(".breadcrumb li > a");
		for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
			categories.add(elementCategories.get(i).ownText().trim());
		}

		return categories;
	}

	private String crawlDescription(Document document) {
		StringBuilder description = new StringBuilder();
		Element descriptionElement = document.select("section.linha-01").first();
		Element caracteristicas = document.select(".caracteristicasDetalhe").first();
		Element dimensoes = document.select(".dimensoes").first();

		if (descriptionElement != null) {
			description.append(descriptionElement.html());
		}

		if (caracteristicas != null) {
			description.append(caracteristicas.html());
		}

		if (dimensoes != null) {
			description.append(dimensoes.html());
		}

		return description.toString();
	}

	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();

		if (price != null) {
			Element aVista = doc.select(".precos1x .precos1x-t2").first();
			if (aVista != null) {
				String text = aVista.text().trim();
				if (text.contains("boleto")) {
					int x = text.indexOf("ou") + 2;
					int y = text.indexOf("no", x);

					Float bankTicketPrice = MathCommonsMethods.parseFloat(text.substring(x, y));
					prices.setBankTicketPrice(bankTicketPrice);
				}
			}

			if (aVista == null) {
				aVista = doc.select(".boxComprarNew-t3").first();

				if (aVista != null) {
					String text = aVista.text().trim();

					if (text.contains("boleto")) {
						int x = text.indexOf("ou") + 2;
						int y = text.indexOf("no", x);

						Float bankTicketPrice = MathCommonsMethods.parseFloat(text.substring(x, y));
						prices.setBankTicketPrice(bankTicketPrice);
					}
				}
			}

			Map<Integer, Float> installmentPriceMap = new HashMap<>();
			Elements parcels = doc.select(".baixoDetalhe-parcelamentoNew span");

			for (Element e : parcels) {
				String parcelText = e.text().toLowerCase();

				if (parcelText.contains("x")) {
					Integer parcel = Integer.parseInt(parcelText.split("x")[0].replaceAll("[^0-9]", ""));

					if (parcelText.contains("(")) {
						int x = parcelText.indexOf('x');
						int y = parcelText.indexOf('(');

						Float parcelPrice = MathCommonsMethods.parseFloat(parcelText.substring(x, y));

						installmentPriceMap.put(parcel, parcelPrice);
					} else {
						Float parcelPrice = MathCommonsMethods.parseFloat(parcelText.split("x")[1]);
						installmentPriceMap.put(parcel, parcelPrice);
					}
				}
			}


			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
		}

		return prices;
	}

}
