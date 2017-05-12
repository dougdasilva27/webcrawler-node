
package br.com.lett.crawlernode.crawlers.corecontent.campogrande;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

public class CampograndeComperCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.comperdelivery.com.br/";

	public CampograndeComperCrawler(Session session) {
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
		
		// performing request to get cookie
		String cookieValue = DataFetcher.fetchCookie(session, HOME_PAGE, "ASP.NET_SessionId", null, 1);
		
		BasicClientCookie cookie = new BasicClientCookie("ASP.NET_SessionId", cookieValue);
		cookie.setDomain("www.comperdelivery.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementInternalId = doc.select("#ProdutoCodigo").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim(); 
			}			

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("span[itemprop=identifier]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().trim();
			}			

			// Nome
			String name = null;
			Element elementName = doc.select(".name.fn[itemprop=name]").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Price
			Float price = null;
			Element elementPricePor = doc.select("#lblPrecoPor strong").first();
			if(elementPricePor != null) {
				price = Float.parseFloat(elementPricePor.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			if (price == null) {
				available = false;
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";

			// Imagens
			String primaryImage = "";
			String secondaryImages = "";
			JSONArray secondaryImagesArray = new JSONArray();
			Element elementPrimaryImage = doc.select("#ProdutoImagem").first();

			if (elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src");
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementNutritionalTable = doc.select("#pnlTabelaNutricional").first();
			Element elementDescription = doc.select("#description").first();
			Element elementComposition = doc.select("#pnlComposicao").first();
			Element elementDivSizeTable = doc.select("#divSizeTable").first();

			if (elementNutritionalTable != null) {
				description = description + elementNutritionalTable.html();
			}
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}
			if (elementComposition != null) {
				description = description + elementComposition.html();
			}
			if (elementDivSizeTable != null) {
				description = description + elementDivSizeTable.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(price);

			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
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
		return (url.startsWith("http://www.comperdelivery.com.br/") && url.endsWith("/p"));
	}
	
	/**
	 * In this market, installments not appear in product page
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.setBankTicketPrice(price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		}

		return prices;
	}

}
