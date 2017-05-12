package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.Prices;

public class SaopauloPanvelCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.panvel.com/";
	private final String HOME_PAGE_HTTPS = "https://www.panvel.com/";

	public SaopauloPanvelCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");

		// Cookie do cep do centro de são paulo
		// Mas o certo seria porto alegre, precisa mudar a cidade.

		/*BasicClientCookie cookie = new BasicClientCookie("LojaVirtualPanvelCepNavegacao", "01030-010");
		cookie.setDomain("www.panvel.com");
		cookie.setPath("/panvel");
		this.cookies.add(cookie);*/
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select(".cod-produto").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.text().split(":")[1].trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select(".det_col02 h1").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			Element elementAvailable = doc.select("#itemAvisemeId").first();
			boolean available = (elementAvailable == null);

			// Preço
			Float price = null;
			if (available) {
				Element elementPrice = doc.select(".etiqueta_preco .precofinal").first();
				if (elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb a"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[5];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			cat[4] = "";

			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			if(elementCategories.size() > 4) {
				category3 = cat[3];
			} else {
				category3 = null;
			}

			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = "";
			Element elementTab1 = doc.select("#tab1").first(); 
			Element elementTab2 = doc.select("#tab2").first();
			if (elementTab1 != null) {
				description += elementTab1.html();
			}
			if (elementTab2 != null) {
				description += elementTab2.html();
			}			

			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();

			// Prices
			Prices prices = crawlPrices(doc, price);

			Product product = new Product();

			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			System.err.println(secondaryImages);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return 	url.startsWith("http://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("http://www.panvel.com/panvel/produto") 
				|| url.startsWith("https://www.panvel.com/panvel/visualizarProduto") 
				|| url.startsWith("https://www.panvel.com/panvel/produto");
	}

	private String crawlPrimaryImage(Document doc) {
		Element primaryImageElement = doc.select("div.ms-slide a").first();
		if (primaryImageElement != null) {
			return primaryImageElement.attr("href");		
		}
		return null;
	}

	private String crawlSecondaryImages(Document doc) throws MalformedURLException {
		Elements elementSecondaryImages = doc.select("div.ms-slide a");
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		for (int i = 1; i < elementSecondaryImages.size(); i++) { // a primeira imagem secundária é igual a primária
			String imageUrl = elementSecondaryImages.get(i).attr("href");

			if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.contains("youtube")) {
				secondaryImagesArray.put(imageUrl);
			}
		}

		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * Showcase price is the price sight
	 * Some cases has installments
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			Element installments = doc.select(".vezes").first();

			if(installments != null){
				String text = installments.text().toLowerCase();

				if(text.contains("x")){
					int x = text.indexOf("x");

					Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
					Float value = MathCommonsMethods.parseFloat(text.substring(x));

					installmentPriceMap.put(installment, value);
				}
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
		}

		return prices;
	}
}
