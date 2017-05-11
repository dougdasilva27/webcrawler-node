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
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

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
 * 6) There is internalPid for skus in this ecommerce. 
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * Examples:
 * ex1 (available): http://www.arcondicionado.com.br/produto/ar-condicionado-split-9000-btus-frio-220v-lg-smile-ts-c092tnw6-68635
 * ex2 (unavailable): http://www.arcondicionado.com.br/produto/cortina-de-ar-90-cm-220v-springer-acs09s5-68690
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/
public class BrasilArcondicionadoCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.arcondicionado.com.br/";

	public BrasilArcondicionadoCrawler(Session session) {
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
			
			
			String url = session.getOriginalURL();
			if(this.session.getRedirectedToURL(url) != null && name != null) {
				url = this.session.getRedirectedToURL(url);
			}
			
			// Creating the product
			Product product = new Product();
			product.setUrl(url);
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
		if ( url.startsWith(HOME_PAGE + "produto/") ){
			return true; 
		}
		return false;
	}
	
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#hdnProdutoVarianteId").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element internalPidElement = document.select("#hdnProdutoId").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value").toString().trim();			
		}
		
		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".prodTitle").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select(".produtoInfo .precoPor").first();		
		
		if (specialPrice == null) {
			specialPrice = document.select("#fbits-forma-pagamento .precoPor").first();
		} 
				
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}
		
		return price;
	}
	
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();
		Float bankTicketPrice = null;
		Map<Integer, Float> installments = new TreeMap<Integer, Float>();
		
		// bank ticket
		Element bankTicketPriceElement = document.select("#divFormaPagamento .precoVista .fbits-boleto-preco").last();
		if (bankTicketPriceElement != null) {
			bankTicketPrice = MathCommonsMethods.parseFloat(bankTicketPriceElement.text());
		}
		
		// card payment options
		// the payment options are the same across all the card brands
		Elements installmentsElements = document.select(".fbits-parcelamento-padrao .details .details-content p");
		for (Element installmentElement : installmentsElements) {
			Integer installmentNumber = null;
			Float installmentPrice = null;
			
			// installmentElement is the <p></p> html element
			// <p> 
			// 	<b>1</b> 
			//		x sem juros de 
			// 	<b>R$ 1.931,58</b> 
			// </p>
			
			// installment number is the first <b></b> child element
			Element installmentNumberElement = installmentElement.select("b").first();
			if (installmentNumberElement != null) {
				installmentNumber = Integer.parseInt(installmentNumberElement.text().trim());
			}
			
			// installment price is the last <b></b> child element
			Element installmentPriceElement = installmentElement.select("b").last();
			if (installmentPriceElement != null) {
				installmentPrice = MathCommonsMethods.parseFloat(installmentPriceElement.text());
			}
			
			installments.put(installmentNumber, installmentPrice);			
		}
		
		// insert the prices on the Prices object
		prices.insertBankTicket(bankTicketPrice);
		
		prices.insertCardInstallment(Card.VISA.toString(), installments);
		prices.insertCardInstallment(Card.AMEX.toString(), installments);
		prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
		prices.insertCardInstallment(Card.DINERS.toString(), installments);
		prices.insertCardInstallment(Card.ELO.toString(), installments);
		prices.insertCardInstallment(Card.DISCOVER.toString(), installments);
		prices.insertCardInstallment(Card.BNDES.toString(), installments);
		
		return prices;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".avisoIndisponivel:not([style])").first();
		
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
		Element primaryImageElement = document.select(".fbits-componente-imagem img").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("data-zoom-image").trim();
			
			if(!image.startsWith("http")){
				image = primaryImageElement.attr("src").trim();
			}
			
			if(image.contains("?")){
				image = image.split("\\?")[0];
			}
			
			primaryImage = image;
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".fbits-produto-imagens a");

		for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image is the primary image
			String image = imagesElement.get(i).attr("data-zoom-image").trim();
			
			if(image.startsWith("http")){
				image = imagesElement.get(i).attr("data-image").trim();
			}
			
			if(image.contains("?")){
				image = image.split("\\?")[0];
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
		Elements elementCategories = document.select("#fbits-breadcrumb span[itemprop=name]");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
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
		Element descriptionElement = document.select("#descricao-produto").first();
		Element princElement = document.select("#carac-principais").first();
		Element specElement = document.select("#carac-tecnicas").first();
		Element dimensionsElement = document.select("#dimensoes").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (princElement != null) description = description + princElement.html();
		if (specElement != null) description = description + specElement.html();
		if (dimensionsElement != null) description = description + dimensionsElement.html();

		return description;
	}

}
