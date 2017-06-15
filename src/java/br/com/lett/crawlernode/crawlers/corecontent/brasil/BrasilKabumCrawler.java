package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilKabumCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.kabum.com.br";

	public BrasilKabumCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}
	
	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");
		
		Map<String, String> cookiesMap = DataFetcher.fetchCookies(session, HOME_PAGE, cookies, 1);
		
		for(Entry<String, String> entry : cookiesMap.entrySet()) {
			BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
			cookie.setDomain(".kabum.com.br");
			cookie.setPath("/");
			this.cookies.add(cookie);
		}
		
	
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc); 
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/**
			 * Caso fixo do blackfriday
			 * onde aparentemente a página do produto dá um refresh para a página do produto na blackfriday.
			 */
			Element blackFriday = doc.select("meta[http-equiv=refresh]").first();

			if(blackFriday != null) {
				String url = blackFriday.attr("content");

				if(url.contains("url=")){
					int x = url.indexOf("url=")+4;

					url = url.substring(x);
				}

				doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
			}

			// internalId
			String internalID = null;
			Element elementInternalID = doc.select(".boxs .links_det").first();
			if(elementInternalID != null) {
				String text = elementInternalID.ownText();
				internalID = text.substring(text.indexOf(':')+1).trim();

				if(internalID.isEmpty()){
					Element e = elementInternalID.select("span[itemprop=sku]").first();

					if(e != null) {
						internalID = e.ownText().trim();
					}
				}
			} 

			Element elementProduct = doc.select("#pag-detalhes").first();

			// internalPid
			String internalPid = null;

			// price
			Float price = null;

			// availability
			boolean available = true;

			// categories
			String category1 = "";
			String category2 = "";
			String category3 = "";

			// Images
			String primaryImage = null;
			String secondaryImages = null;

			// name
			String name = null;

			// Prices
			Prices prices = new Prices();

			if(elementProduct != null){

				// Prices
				prices = crawlPrices(elementProduct);

				//Name 
				Element elementName = elementProduct.select("#titulo_det h1").first();
				if (elementName != null) {
					name = elementName.text().replace("'","").replace("’","").trim();
				}

				// Price
				Element elementPrice = elementProduct.select(".preco_normal").first();
				if(elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				} else {
					elementPrice = elementProduct.select(".preco_desconto-cm").first();

					if(elementPrice != null){
						price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				}

				//Available
				Element elementAvailability = elementProduct.select(".disponibilidade img").first();			
				if(elementAvailability == null || !elementAvailability.attr("alt").equals("produto_disponivel")) {
					available = false;
				}

				// Categories

				Elements elementCategories = elementProduct.select(".boxs .links_det a");
				for(Element e : elementCategories) {
					if(category1.isEmpty()) {
						category1 = e.text().replace(">", "").trim();
					} 
					else if(category2.isEmpty()) {
						category2 = e.text().replace(">", "").trim();
					} 
					else if(category3.isEmpty()) {
						category3 = e.text().replace(">", "").trim();
					}
				}


				// images
				Elements elementImages = elementProduct.select("#imagens-carrossel li img");
				JSONArray secondaryImagesArray = new JSONArray();
				for(Element e : elementImages) {
					if(primaryImage == null) {
						primaryImage = e.attr("src").replace("_p.", "_g.");
					} else {
						secondaryImagesArray.put(e.attr("src").replace("_p.", "_g."));
					}

				}

				if(secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				}
			}



			// description
			String description = "";   
			Element elementDescription = doc.select(".tab_").first();
			if(elementDescription != null) description = elementDescription.html().replace("’","").trim();

			// stock
			Integer stock = null;

			// marketplace
			Marketplace marketplace = new Marketplace();

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
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

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}


	private Prices crawlPrices(Element product){
		Prices prices = new Prices();
		Map<Integer,Float> installmentPriceMap = new HashMap<>();

		Element priceBoleto = product.select(".preco_desconto strong").first();

		if(priceBoleto != null){
			Float bankTicket = Float.parseFloat(priceBoleto.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			prices.setBankTicketPrice(bankTicket);
		}

		Elements installmentsPrices = product.select(".ParcelamentoCartao li");

		for(Element e : installmentsPrices){
			String text = e.text().toLowerCase();

			int x = text.indexOf("x");

			Integer installment = Integer.parseInt(text.substring(0,x).trim());
			Float value = Float.parseFloat(text.substring(x).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			installmentPriceMap.put(installment, value);
		}

		if(installmentPriceMap.size() > 0){
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}

		return prices;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("https://www.kabum.com.br/produto/") || url.startsWith("http://www.kabum.com.br/produto/") || url.contains("blackfriday"));
	}
}
