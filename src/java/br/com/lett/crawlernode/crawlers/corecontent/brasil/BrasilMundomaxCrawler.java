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
 * Crawling notes (17/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is page is more than one sku in it, but we have one URL per each.
 * Business logic: We only crawl the sku of the URL passed, the variations are not crawled, because de web-crawler will discover than later.
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
 * 8) To get secondary images we must crawl the javascript text and parse it.
 * 
 * 9) Products in 'destaque' section, are not crawled. In these cases, when we click on the sku image, it redirects to another URL
 * of the same product. This last URL is crawled instead of the first one.
 * 
 * Examples:
 * ex1 (available): http://www.mundomax.com.br/inversor-de-onda-modificada-12vd127v-usb-800w-hayonik
 * ex2 (unavailable): http://www.mundomax.com.br/calculadora-de-mesa-12-digitos-mx120s-prata-casio?su=1
 * ex3: (variations): http://www.mundomax.com.br/caixa-multiuso-player-80-20w-rms-usb-rosa-hayonik
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMundomaxCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.mundomax.com.br/";

	public BrasilMundomaxCrawler(Session session) {
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

		if ( isProductPage(doc, session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// InternalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);
			
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
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);

			// Description
			String description = crawlDescription(doc);

			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

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

	private boolean isProductPage(Document document, String url) {
		if ((document.select(".product").first() != null || document.select(".content > div > div > a > h1").first() != null)
				&& !url.startsWith(HOME_PAGE + "destaque/")) return true;
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("meta[itemprop=sku]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("content").toString().trim();			
		} else {
			Element unnavailableProduct = document.select(".content > div > div > div > a > span").first();

			if(unnavailableProduct != null) {
				internalId = unnavailableProduct.text().replaceAll("[^0-9]", "").trim();
			}
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".preco_por2").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	/**
	 * Card payment options are the same across all card brands.
	 * 
	 * @param document
	 * @return
	 */
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		
		// bank slip
		Element bankSlipPriceElement = document.select(".desconto label").first();
		if (bankSlipPriceElement != null) {
			Float bankSlipPrice = MathCommonsMethods.parseFloat(bankSlipPriceElement.text());
			prices.setBankTicketPrice(bankSlipPrice);
		}
		
		// installments
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		Elements installmentsElements = document.select(".parcels .parcel_list ul li");
		for (Element installmentElement : installmentsElements) {
			Element installmentNumberElement = installmentElement.select("p.p-obs").first();
			Element installmentPriceElement = installmentElement.select("p.p-value").first();
			
			if (installmentNumberElement != null && installmentPriceElement != null) {
				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(installmentNumberElement.text());
				Integer installmentNumber = Integer.parseInt(parsedNumbers.get(0));
				Float installmentPrice = MathCommonsMethods.parseFloat(installmentPriceElement.text());
				
				installments.put(installmentNumber, installmentPrice);
			}
		}
		
		// only insert the installments is they are not empty
		if (installments.size() > 0) {
			prices.insertCardInstallment(Card.VISA.toString(), installments);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
			prices.insertCardInstallment(Card.HIPER.toString(), installments);
			prices.insertCardInstallment(Card.ELO.toString(), installments);
			prices.insertCardInstallment(Card.DINERS.toString(), installments);
			prices.insertCardInstallment(Card.AMEX.toString(), installments);
		}
				
		return prices;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#informs_supply").first();
		
		if (notifyMeElement != null) {
			return false;
		}
		
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
		Element primaryImageElement = document.select("#galleria .jqzoom").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		} else {
			Element unnavailableProduct = document.select(".content > div > a > img").first();

			if(unnavailableProduct != null) {
				primaryImage = unnavailableProduct.attr("src");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		ArrayList<String> imagesArray = getImagesFromScript(doc);	

		for (String image : imagesArray) { 
			if(!image.equals(primaryImage)){
				secondaryImagesArray.put(image);
			}
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	
	/**
	 * Pegando as imagens de um script java script
	 * @param doc
	 * @return ArrayList<String> 
	 */
	private ArrayList<String>  getImagesFromScript(Document doc){
		ArrayList<String> imagesArray = new ArrayList<String>();
		Elements scripts = doc.select(".product > script[type=text/javascript]");
		String scriptImages = null;
		
		for(Element e : scripts){
			if(!e.hasAttr("src")){
				scriptImages = e.outerHtml();
			}
		}
		
		//após pegar o script, é formatado para pegar somente as urls de imagens
		if(scriptImages != null && scriptImages.contains(";")) {
			String[] tokens = scriptImages.split(";");

			for (int i = 0; i < (tokens.length - 1); i++) {
				String temp = tokens[i];
				if (temp.startsWith("gal.add('IMAGE','http") || temp.startsWith("gal.add('OFFER','http")) {
					String[] tokens2 = temp.split(",");
					String imageTemp = tokens2[1].replaceAll("'", "").replace("_440_", "_2000_").trim(); // tornando a imagem maior substituindo 440 por 2000

					int x = imageTemp.indexOf(".jpg");

					imagesArray.add(imageTemp.substring(0, x + 4));
				}
			}
		}
		return imagesArray;
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#stmp div > a > h6");

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
		Elements descriptionElement = document.select(".dog");
		
		for(Element e : descriptionElement){
			description += e.html();
		}

		return description;
	}

}
