package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Prices;

/**
 * Date: 25/11/2016
 * 1) Only one sku per page.
 * 
 * Price crawling notes:
 * 1) For this market was not found product unnavailable
 * 2) Page of product disappear when javascripit is off, so is accessed this api: "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc="+id
 * 3) InternalId of product is in url and a json, but to fetch api is required internalId, so it is crawl in url
 * 4) Has no bank ticket in this market
 * 5) Has no internalPid in this market
 * 6) IN api when have a json, sometimes has duplicates keys, so is used GSON from google.
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloOnofreCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.onofre.com.br/";
	private final String HOME_PAGE_HTTP = "http://www.onofre.com.br/";

	public SaopauloOnofreCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTP));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
			
			// InternalId
			String internalId = crawlInternalId(doc);
						
			// InternalPid
			String internalPid = crawlInternalPid(doc);
			
			// Name
			String name = crawlName(doc);
			
			// Price
			Float price = crawlPrice(doc);
			
			// Prices
			Prices prices = crawlPrices(price);
			
			// Avaiability
			boolean available = crawlAvailability(doc);
			
			// Categories
			CategoryCollection categories = crawlCategories(doc);
			
			// Primary Image
			String primaryImage = crawlPrimaryImage(doc);
			
			// SecondaryImages
			String secondaryImages = crawlSecondaryImages(doc, primaryImage);
			
			// Description
			String description = crawlDescription(doc);
			
			// Stock
			Integer stock = null;
			
			// Marketplace
			Marketplace marketplace = crawlMarketplace(doc);

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();
			
			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		Element id = doc.select("#cphConteudo_hf_id_produto").first();
		
		if(id != null) {
			return true;
		}
		
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element id = doc.select("#cphConteudo_hf_id_produto").first();
		
		if(id != null) {
			internalId = id.val();
		}

		return internalId;
	}

	/**
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;
		
		Element elementInternalPid = document.select(".code.col #cphConteudo_lblCode").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.text().split(":")[1].trim();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#lblProductName").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element elementPrice = document.select("#cphConteudo_lblPrecoPor").first();

		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = true;
		Elements elementAvailable = document.select(".product-spec .action-box .buy-area .unavailable");
		if(elementAvailable.first() != null) {
			available = false;
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage;
		Element elementPrimaryImage = document.select(".dp_box_produto .jqzoom").first();

		if(elementPrimaryImage == null){
			elementPrimaryImage = document.select("#cphConteudo_imgGrande").first();
			primaryImage = elementPrimaryImage.attr("src");
		} else {
			primaryImage = elementPrimaryImage.attr("href");
		}
		
		if (primaryImage.startsWith("https://www.onofre.com.br") || primaryImage.startsWith("http://www.onofre.com.br")) {
			primaryImage = primaryImage.replace("Produto/Normal", "Produto/Super");
		} else {
			primaryImage = HOME_PAGE + primaryImage.replace("Produto/Normal", "Produto/Super");
		}

		if(primaryImage.contains("imagem_prescricao")) { // only for meds
			primaryImage = "";
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementSecondaryImages = document.select(".main-product-image .img-thumbs ul li img");

		for(Element e : elementSecondaryImages) {
			String image = e.attr("src").replace("Produto/Normal", "Produto/Super");
			
			if(!image.equals(primaryImage)) {
				secondaryImagesArray.put(e.attr("src").replace("Produto/Normal", "Produto/Super"));
			}
		}

		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private Marketplace crawlMarketplace(Document document) {
		return new Marketplace();
	}

	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select("#breadcrumbs a");

		for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
			categories.add( elementCategories.get(i).text().trim());
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();

		Element elementDescription = doc.select(".product-details").first(); 
		if(elementDescription != null) {
			description.append(elementDescription.html());
		}	

		return description.toString();
	}


	/**
	 * In product page has only one price,
	 * but in footer has informations of payment methods
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
			
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
		}
				
		
		return prices;
	}

}
