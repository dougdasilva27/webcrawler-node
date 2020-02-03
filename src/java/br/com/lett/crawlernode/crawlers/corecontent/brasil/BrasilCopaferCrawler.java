package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/************************************************************************************************************************************************************************************
 * Crawling notes (22/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) If the sku is unavailable, it's price is displayed.
 * 
 * 6) The price of sku, found in json script, is wrong when the same is unavailable, then it is not crawled.
 * 
 * 7) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image on the secondary images.
 * 
 * Examples:
 * ex1 (available): http://www.copafer.com.br/furadeira-impacto-velocidade-variavel-e-reversivel-%C2%BD-550w-hd500br-black-e-decker/p4732510
 * ex2 (unavailable): http://www.copafer.com.br/ar-condicionado-portatil-127v-10000-btus-8951001-ventisol/p8709190
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCopaferCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.copafer.com.br/";

	public BrasilCopaferCrawler(Session session) {
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

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String nameMainPage = crawlName(doc);

			// Price
			Float price = crawlPrice(doc);
			
			// Prices
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
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

			// Sku variations
			Elements skus = doc.select(".prod-skulist");

			if(skus.size() > 0){

				/* ***********************************
				 * crawling data of mutiple products *
				 *************************************/

				for(Element sku : skus){

					// InternalId
					String internalID = crawlInternalIdForMutipleVariations(sku);

					// Name
					String name = crawlNameForMutipleVariations(sku, nameMainPage);

					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalID);
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
				}

				/* *********************************
				 * crawling data of single product *
				 ***********************************/

			} else {

				// InternalId
				String internalID = crawlInternalIdSingleProduct(doc);

				Product product = new Product();
				product.setUrl(session.getOriginalURL());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
				product.setName(nameMainPage);
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
			}


		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		return document.select(".productbox").first() != null;
	}

	/*********************
	 * Variation methods *
	 *********************/

	private String crawlInternalIdForMutipleVariations(Element sku) {
		String internalId = null;
		Element e = sku.select("input").first();

		internalId = e.attr("idproduct").trim();

		return internalId;
	}

	private String crawlNameForMutipleVariations(Element sku, String name) {
		String nameVariation = name;	

		if(!sku.text().isEmpty()){
			nameVariation = nameVariation + " - " + sku.text();
		}	

		return nameVariation;
	}

	/**********************
	 * Single Sku methods *
	 **********************/

	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".info-side h2").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().replaceAll("[^0-9]", "").trim();
		}

		return internalId;
	}

	private Float crawlPrice(Document doc) {
		Float price = null;	
		Element priceElement = doc.select(".text_preco_prod_listagem_por").first();

		if(priceElement != null){
			price = Float.parseFloat( priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim() );
		}	

		return price;
	}
	
	/**
	 * We consider the 1x payment option the same as the bank slip price.
	 * The payment options are the same across all the card brands, and we only have
	 * 1x or 5x payment (or any other maximum number of installments).
	 * 
	 * @param document
	 * @return
	 */
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		
		// bank slip
		Float bankSlipPrice = crawlBankSlipPrice(document);
		if (bankSlipPrice != null) {
			prices.setBankTicketPrice(bankSlipPrice);
		}
		
		// installments
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		if (bankSlipPrice != null) { // 1x
			installments.put(1, bankSlipPrice);
		}
		
		Element maxInstallmentNumberElement = document.select("#ContentSite_divAvailable .text_preco_prod_listagem_parcelas").first();
		Element installmentPriceElement = document.select("#ContentSite_divAvailable .text_preco_prod_listagem_parcelado").first();
		if (maxInstallmentNumberElement != null && installmentPriceElement != null) {
			List<String> parsedNumbers = MathUtils.parsePositiveNumbers(maxInstallmentNumberElement.text());
			if (parsedNumbers.size() > 0) {
				Integer maxInstallmentNumber = Integer.parseInt(parsedNumbers.get(0));
				Float installmentPrice = MathUtils.parseFloatWithComma(installmentPriceElement.text());
				
				installments.put(maxInstallmentNumber, installmentPrice);
			}
		}
		
		if (installments.size() > 0) {
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
			prices.insertCardInstallment(Card.VISA.toString(), installments);
			prices.insertCardInstallment(Card.AMEX.toString(), installments);
			prices.insertCardInstallment(Card.ELO.toString(), installments);
			prices.insertCardInstallment(Card.DINERS.toString(), installments);
		}
		
		return prices;
	}
	
	private Float crawlBankSlipPrice(Document document) {
		Float bankSlipPrice = null;
		Element bankSlipPriceElement = document.select("#ContentSite_divAvailable3 div.conteudo_preco_selos_detalhe #ContentSite_divAvailable .text_preco_prod_listagem_por").first();
		if (bankSlipPriceElement != null) {
			bankSlipPrice = MathUtils.parseFloatWithComma(bankSlipPriceElement.text());
		}
		return bankSlipPrice;
	}

	private boolean crawlAvailability(Document doc) {
		Element e = doc.select(".text_produto_indisponivel").first();

		return e == null;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element pid = document.select(".info-side h2").first();
		
		if(pid != null){
			internalPid = pid.text().replaceAll("[^0-9]", "").trim();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#dsProductName").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}

	private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".img_prod_detalhe a").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("rel").trim().toLowerCase();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".conteudo_miniaturas_detalhe ul li a");

		for (int i = 1; i < imagesElement.size(); i++) { // start with indez 1 because the first image is the primary image
			Element e = imagesElement.get(i);

			if(e.hasAttr("rel") && e.attr("rel").startsWith("http"))	{
				secondaryImagesArray.put( e.attr("rel").trim().toLowerCase() );
			} else if(e.hasAttr("rel2") && e.attr("rel2").startsWith("http")){
				secondaryImagesArray.put( e.attr("rel2").trim().toLowerCase() );
			} else {
				Element img = e.select("img").first();

				if(img != null){
					secondaryImagesArray.put( e.attr("src").trim() );
				}
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb_listagem a[id]");

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
		Element descriptionElement = document.select(".conteudo_mais_detalhes").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
