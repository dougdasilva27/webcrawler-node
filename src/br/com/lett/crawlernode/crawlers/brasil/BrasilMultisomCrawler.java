package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;


/************************************************************************************************************************************************************************************
 * Crawling notes (01/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku, but are urls from variations in url product.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is not displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) To crawled the variations, there are urls in the HTML element on the page, except for the product that is already loaded 
 *	it is not necessary to reload
 * 
 * Examples:
 * ex1 (available): http://www.multisom.com.br/produto/ar-condicionado-split-lg-inverter-libero-18-000-btus-quente-frio-220v-tecnologia-inverter-economia-de-energia-modelo-art-cool-asuw182crg2-2535
 * ex2 (unavailable): http://www.multisom.com.br/produto/escaleta-stagg-melosta-32-teclas-preto-5668
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilMultisomCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.multisom.com.br/";

	public BrasilMultisomCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			// InternalId
			String internalIDFirstProduct = crawlInternalId(doc);

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Name
			String name = crawlName(doc);

			// Price
			Float price = crawlMainPagePrice(doc);
			
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
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalIDFirstProduct);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
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

			ArrayList<String> variationsUrls = this.crawlUrlForMutipleVariations(doc, internalIDFirstProduct);
			
			for(String urlVariation : variationsUrls){
				Document docVariation = this.fetchPageVariation(urlVariation);
				
				// InternalId
				String internalIDVariation = crawlInternalId(docVariation);

				// Name
				String nameVariation  = crawlName(docVariation);

				// Price
				Float priceVariation  = crawlMainPagePrice(docVariation);
				
				// Availability
				boolean availableVariation  = crawlAvailability(docVariation);

				// Categories
				ArrayList<String> categoriesVariation  = crawlCategories(docVariation);
				String category1Variation  = getCategory(categoriesVariation , 0);
				String category2Variation  = getCategory(categoriesVariation , 1);
				String category3Variation  = getCategory(categoriesVariation , 2);

				// Primary image
				String primaryImageVariation = crawlPrimaryImage(docVariation);

				// Secondary images
				String secondaryImagesVariation = crawlSecondaryImages(docVariation);

				// Description
				String descriptionVariation = crawlDescription(doc);

				// Creating the product
				Product productVariation = new Product();
				productVariation.setSeedId(this.session.getSeedId());
				productVariation.setUrl(urlVariation);
				productVariation.setInternalId(internalIDVariation);
				productVariation.setInternalPid(internalPid);
				productVariation.setName(nameVariation);
				productVariation.setPrice(priceVariation);
				productVariation.setAvailable(availableVariation);
				productVariation.setCategory1(category1Variation);
				productVariation.setCategory2(category2Variation);
				productVariation.setCategory3(category3Variation);
				productVariation.setPrimaryImage(primaryImageVariation);
				productVariation.setSecondaryImages(secondaryImagesVariation);
				productVariation.setDescription(descriptionVariation);
				productVariation.setStock(stock);
				productVariation.setMarketplace(marketplace);

				products.add(productVariation);
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		if ( document.select(".detailProduct").first() != null ) return true;
		return false;
	}
	
	/********************
	 * Multiple Product *
	 ********************/
	
	private ArrayList<String> crawlUrlForMutipleVariations(Document doc, String internalIDFirstProduct){
		ArrayList<String> productsUrls = new ArrayList<String>();
		Elements variations = doc.select(".variation li input");
		
		for(Element e : variations){
			String idVariation 	= e.attr("value");
			String url			= e.attr("data-urlproduct");
			
			if(!idVariation.equals(internalIDFirstProduct)){
				productsUrls.add(url);
			}
		}
		
		
		return productsUrls;
	}
	
	private Document fetchPageVariation(String url){
		Document doc = new Document("");
		
		if(url != null){
			doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);
		}
		
		return doc;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("input[name=data[Produto][rating][id_produto]]").first();

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
		Element nameElement = document.select(".detailProduct h1 span").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element specialPrice = document.select("p.prices ins").first();		
		
		if (specialPrice != null) {
			price = Float.parseFloat( specialPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".productUnavailable").first();
		
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
		Element primaryImageElement = document.select("figure.imageWrapper a").first();

		if (primaryImageElement != null) {
			primaryImage = HOME_PAGE + primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#carousel li a img");

		for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image is the primary image
			secondaryImagesArray.put( HOME_PAGE + imagesElement.get(i).attr("src").trim().replaceAll("false", "true") ); // montando url para pegar a maior imagem
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs.breadcrumbsPages ul li a");

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
		Element descriptionElement = document.select("#descricao .boxDescricao").first();
		Element specElement = document.select("#especificacao .boxDescricao").first();

		if (descriptionElement != null) description = description + descriptionElement.html();
		if (specElement != null) description = description + specElement.html();

		return description;
	}

}
