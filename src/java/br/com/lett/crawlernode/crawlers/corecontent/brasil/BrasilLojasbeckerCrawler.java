package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (25/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is not in the secondary images selector.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available): http://www.strar.com.br/umidificador-de-ar-springer-pure-3-5-litros-110.html
 * ex2 (unavailable): http://www.strar.com.br/ar-condicionado-janela-springer-silentia-mecanico-18000-btu-h-quente-frio-220v.html
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLojasbeckerCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.elojasbecker.com.br/";

	public BrasilLojasbeckerCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
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
			
			// Prices
			Prices prices = crawlPrices(doc, price);
			
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
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Creating the product
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select("#produto-detalhe2").first() != null ) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#id_produto").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element pid = document.select("#produto-detalhe2 h2").first();
		
		if(pid != null){
			String text = pid.text();
			
			if(text.contains(":")){
				internalPid = text.split(":")[1].trim();
			}
		}

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#produto-detalhe2 > h1").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".preco-atual").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("div.botao-grande:not(.lista-casamento)  #formCarrinho .botao-imagem").first();
		
		if (notifyMeElement != null) {
			if(notifyMeElement.attr("src").trim().contains("indisponivel")){
				return false;
			} else {
				return true;
			}
		}
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#fotos #imagem-grande a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#miniaturas li a");

		for (int i = 1; i < imagesElement.size(); i++) { //start with index 1 because the firts image is the primary image
			String image = imagesElement.get(i).attr("href").trim();
			
			if(!image.startsWith(HOME_PAGE)){
				 image = HOME_PAGE + image;
			}
			secondaryImagesArray.put( image );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".bread a");

		for (int i = 0; i < elementCategories.size(); i++) {
			categories.add( elementCategories.get(i).text().trim() );
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
		Element descriptionElement = document.select("#descricao-detalhes").first();
		
		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}
	
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			Elements installments = doc.select(".ulCondicoesPagamento li");
			
			for(Element e : installments){
				Integer installment = Integer.parseInt(e.ownText().replaceAll("[^0-9]", ""));
				
				Element installmentValue = e.select("strong").first();
				
				if(installmentValue != null){
					Float value = MathUtils.parseFloatWithComma(installmentValue.text());
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
