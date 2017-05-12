package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import models.Prices;


/************************************************************************************************************************************************************************************
 * Crawling notes (08/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done by looking the URL format and if the html contains an specific element that identifies a sku.
 * 
 * 5) If a product is unavailable, it's price is not displayed. Still, the crawler tries to crawl the price of the sku.
 * 
 * 6) There are two cases that classifies a sku as unavailable: 
 * 	6.1) The html contains the element to notify the customer when the product becomes available
 * 	6.2) The html contains the button element to evaluate a product price. It's not available for immediate purchase.
 * 
 * 7) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.centraltec.com.br/hi-wall/7-000-a-9-000-btus/split-hi-wall-agratto-9000-btus-220v-frio-gas-r410
 * ex2 (unavailable - 6.2 case): http://www.centraltec.com.br/inverter/linha-lg/split-art-cool-inverter-22000-btus-220v---frio
 * ex3 (unavailable - 6.1 case): http://www.centraltec.com.br/inverter/linha-gree/split-gree-cozy-22000-btus-inverter-220v---frio 
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCentraltecCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.centraltec.com.br/";

	public BrasilCentraltecCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);
			
			// prices
			Prices prices = crawlPrices(doc);

			// Availability
			boolean available = crawlAvailability(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Creating the product
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setAvailable(available);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

			products.add(product);


		} else {
			Logging.printLogDebug(logger, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element skuElement = document.select("#prod_padd").first();
		if ( skuElement != null && !url.startsWith(HOME_PAGE + "p/") ) return true;
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("input[id=ProdutoId]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#prod_header .prod_tit span[itemprop=name]").first();

		if (nameElement != null) {
			name = sanitizeName( nameElement.text() );
		}

		Element modelName = document.select(".prod_tit_ref > span[content]").first();

		if(modelName != null) {
			name = name + " " + modelName.ownText();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Elements mainPagePriceElements = document.select("div[itemprop=offers] .prod_valor_por span"); // the first <span> is the 'R$' and the other is the number itself

		if (mainPagePriceElements.size() > 1) {
			Element priceElement = mainPagePriceElements.get(1);
			if (priceElement != null) {
				price = Float.parseFloat( priceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
			}
		}

		return price;
	}

	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		Float bankTicketPrice = null;
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();

		// bank ticket price
		Element bankTicketPriceElement = document.select("div[itemprop=offers] .prod_valor_avista span[itemprop=price]").first();
		if (bankTicketPriceElement != null) {
			bankTicketPrice = MathCommonsMethods.parseFloat(bankTicketPriceElement.text());
		}

		// installments prices
		// for this ecommerce we have only two installment informations: 1x on card and 10x (sem juros)
		// all the payment options are the same for all card brands
		Element firstInstallmentElement = document.select("div[itemprop=offers] .prod_valor_por span").last(); // 1x
		if (firstInstallmentElement != null) {
			installments.put(1, MathCommonsMethods.parseFloat(firstInstallmentElement.text()));
		}

		Element lastInstallmentElement = document.select("div[itemprop=offers] .prod_valor_parc").first();
		if (lastInstallmentElement != null) {
			String line = lastInstallmentElement.text().trim(); // 10x de R$ 308,80 sem juros
			Integer installmentNumber = null;
			Float installmentPrice = null;

			// parsing the installment number
			int indexOfX = line.indexOf('x') + 1;
			String installmentNumberString = line.substring(0, indexOfX); // "10x"
			installmentNumber = Integer.parseInt( MathCommonsMethods.parseNumbers(installmentNumberString).get(0) );

			// parsing the installment price
			String installmentPriceString = line.substring(indexOfX, line.length()); // " de R$ 308,80 sem juros"
			installmentPrice = MathCommonsMethods.parseFloat(installmentPriceString);

			// the payment options are the same for all cards brands
			installments.put(installmentNumber, installmentPrice);
		}
		
		// insert the prices on the Prices object
		prices.setBankTicketPrice(bankTicketPrice);
		
		// insert the installments, they are all the same across all the card brands
		prices.insertCardInstallment(Card.VISA.toString(), installments);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
		prices.insertCardInstallment(Card.AMEX.toString(), installments);

		return prices;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#prod_box_indisponivel").first();
		Element toEvaluateElement = document.select("#orcar-btn").first();

		if (notifyMeElement != null) return false;
		if (toEvaluateElement != null) return false;		

		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#produto-detalhe-imagem .zoomPad a img").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("src").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#produto-detalhe-imagem .zoomPad a img");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("src").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".prod_nav ul li a");

		for (int i = 0; i < elementCategories.size(); i++) {
			String category = sanitizeName( elementCategories.get(i).text() );
			categories.add(category);
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select("#descricao-aba-produto").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

	private String sanitizeName(String name) {
		return name.replace("'","").replace("’","").trim();
	}

}
