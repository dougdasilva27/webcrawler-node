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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (16/08/2016):
 * 
 * 1) For this crawler, we have URLs with multiple variations of a sku(in this market we have one url for sku).
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku. 
 * 
 * 7) For this market, is crawl one product per url.
 * 
 * Examples:
 * ex1 (available): http://www.dutramaquinas.com.br/produtos/kit-de-ferramentas-com-5-pecas-st-kit1-st-kit1
 * ex2 (unavailable): http://www.dutramaquinas.com.br/produtos/torno-de-bancada-forjado-n-10-33890-010 
 * ex3 (variations):http://www.dutramaquinas.com.br/produtos/furadeira-parafusadeira-3-8-12-volts-rotacao-de-ate-1-500-rpm-com-1-bateria-de-litio-dcd710s2-dcd710s2-br
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilDutramaquinasCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.dutramaquinas.com.br/";
	
	public BrasilDutramaquinasCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
			
			// InternalId
			String internalId = crawlInternalId(doc);
			
			// Pid
			String internalPid = crawlInternalPid(doc);
			
			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
			// Api variations
			Document docVariation = crawlVariationsFromApi(internalId);
			
			// HasVariations
			boolean hasSkuVariations = this.hasVariations(docVariation);
			
			// Name
			String nameMainPage = crawlMainPageName(doc);
			
			// Name variation
			String name = crawlNameVariations(null, hasSkuVariations, nameMainPage, docVariation);
			
			// Price
			Float price = crawlMainPagePrice(docVariation);
			
			Prices prices = crawlPrices(doc);
			
			// Availability
			boolean available = crawlAvailability(docVariation);

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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "produtos/") || url.startsWith(HOME_PAGE + "/produtos/")) return true;
		return false;
	}
	
	/*********************
	 * Product variation *
	 *********************/
	
	private boolean hasVariations(Document doc) {
		Element variation = doc.select(".variacao").first();
		if(variation != null) return true;
		
		return false;
	}
	
	private Document crawlVariationsFromApi(String internalID) {
		String urlParameters = "id_produto=" + internalID;
		return DataFetcher.fetchDocument(DataFetcher.POST_REQUEST, session, "http://www.dutramaquinas.com.br/model/md_interna_produtos_dados.php", urlParameters, null);
	}
	
	private String crawlNameVariations(Element e, boolean hasVariations, String nameMainPage, Document doc) {
		String name = nameMainPage;
		
		if(hasVariations){
			if( e == null){
				Elements options = doc.select(".variacao select option");
				
				if(options.size() > 1){
					for(Element x : options){
						if(x.hasAttr("selected")){
							e = x;
							break;
						}
					}
				} else {
					e = options.get(0);
				}
			}
			
			if(e != null){
				name = name + " - " + e.text();
			}
		}

		return name;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#ls_id_produto").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlMainPageName(Document document) {
		String name = null;
		Element nameElement = document.select("#mainCenterProdutos .titulo").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".valor div.preco").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.ownText().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		
		// bank slip
		Float bankSlipPrice = crawlBankSlipPrice(document);
		if (bankSlipPrice != null) {
			prices.setBankTicketPrice(bankSlipPrice);
		}
		
		// installments
		Map<Integer, Float> installments = crawlCardInstallments(document);
		prices.insertCardInstallment(Card.VISA.toString(), installments);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
		prices.insertCardInstallment(Card.DINERS.toString(), installments);
		prices.insertCardInstallment(Card.AMEX.toString(), installments);
		prices.insertCardInstallment(Card.ELO.toString(), installments);
				
		return prices;
	}
	
	/**
	 * Get the bank slip price that is displayed on the product main page.
	 * Only one price is displayed as cash price, so we consider it to be the
	 * bank slip price too.
	 * 
	 * @param document
	 * @return
	 */
	private Float crawlBankSlipPrice(Document document) {
		Float bankSlipPrice = null;
		
		Element bankSlipPriceElement = document.select(".valor div.preco").first();
		if (bankSlipPriceElement != null) {
			String bankSlipPriceText = bankSlipPriceElement.ownText();
			if (!bankSlipPriceText.isEmpty()) {
				bankSlipPrice = MathUtils.parseFloatWithComma(bankSlipPriceText);
			}
		}
		
		return bankSlipPrice;
	}
	
	/**
	 * Get all cards payment options. They are all the same across
	 * the cards brands.
	 * The first installment price is in a separated html element. On this
	 * ecommerce is a cash price payment option.
	 * 
	 * @param document
	 * @return
	 */
	private Map<Integer, Float> crawlCardInstallments(Document document) {
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		
		// first installment
		Element firstInstallmentElement = document.select(".parcelas .parcelamento #cartao-avista .txt").first();
		if (firstInstallmentElement != null) {
			Element installmentNumberElement = document.select(".numero-parcela").first();
			Element installmentPriceElement = document.select(".valor-parcela span").first();
			if (installmentNumberElement != null && installmentPriceElement != null) {
				String installmentNumberText = installmentNumberElement.text();
				String installmentPriceText = installmentPriceElement.text();
				
				List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberText);
				Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
				Float installmentPrice = MathUtils.parseFloatWithComma(installmentPriceText);
				
				installments.put(installmentNumber, installmentPrice);
			}
		}
		
		// the remaining installments
		Elements installmentsElements = document.select(".parcelas .parcelamento .cartao-parcelas .txt");
		for (Element installmentElement : installmentsElements) {
			Element installmentNumberElement = installmentElement.select(".numero-parcela").first();
			Element installmentPriceElement = installmentElement.select(".valor-parcela span").first();
			if (installmentNumberElement != null && installmentPriceElement != null) {
				String installmentNumberText = installmentNumberElement.text();
				String installmentPriceText = installmentPriceElement.text();
				
				List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberText);
				Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
				Float installmentPrice = MathUtils.parseFloatWithComma(installmentPriceText);
				
				installments.put(installmentNumber, installmentPrice);
			}
		}
		
		return installments;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".btn_aviseme").first();
		
		if (notifyMeElement != null) {
			return false;
		}
		
		return true;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<>();
	}
	
	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select("#imagens ul li a").first();

		if (primaryImageElement != null) {
			if(primaryImageElement.hasAttr("data-large")){
				if(primaryImageElement.attr("data-large").endsWith(".jpg")){
					primaryImage = HOME_PAGE + primaryImageElement.attr("data-large");
				} else {
					primaryImage = HOME_PAGE + primaryImageElement.attr("href");
				}
			} else {
				primaryImage = HOME_PAGE + primaryImageElement.attr("href");
			}
		}

		return primaryImage.toLowerCase();
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#imagens ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			Element e = imagesElement.get(i);
			if(e.hasAttr("data-large")){
				if(e.attr("data-large").endsWith(".jpg")){
					secondaryImagesArray.put(HOME_PAGE + e.attr("data-large"));
				} else {
					secondaryImagesArray.put(HOME_PAGE + e.attr("href"));
				}
			} else {
				secondaryImagesArray.put(HOME_PAGE + e.attr("href"));
			}
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			String href = elementCategories.get(i).attr("href").trim();
			
			if(!href.equals("/home")){
				categories.add( elementCategories.get(i).text().trim() );
			}
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
		Elements descriptionElements = document.select("#descricao");

		for(Element e : descriptionElements){
			Element temp = e.select(".produto-videos").first();
			
			if(temp == null){
				description = description + e.html();
			}
		}

		return description;
	}

}