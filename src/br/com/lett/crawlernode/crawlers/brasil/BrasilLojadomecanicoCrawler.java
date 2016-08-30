package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (20/07/2016):
 * 
 * 1) For this crawler, we have URLs with multiple variations of a sku.
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
 * 
 * Examples:
 * ex1 (available): http://www.lojadomecanico.com.br/produto/34988/21/159/mini-compressor-de-ar-air-plus-digital-de-12v-schulz-9201163-0
 * ex2 (unavailable): http://www.lojadomecanico.com.br/produto/96544/3/163/tampa-de-reservatorio-arrefecimento-do-onix-para-sa1000-planatc-10201195867 
 * ex3 (variations): http://www.lojadomecanico.com.br//produto/18790/21/221/furadeira--parafusadeira-eletrica-280w-220v
 * 
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilLojadomecanicoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.lojadomecanico.com.br/";

	public BrasilLojadomecanicoCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());
			
			// InternalId
			String internalId = crawlInternalId(doc);
			
			// Pid
			String internalPid = crawlInternalPid(doc);
			
			// Stock
			Integer stock = null;

			// Marketplace map
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);

			// Marketplace
			JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
			
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

			// Creating the product
			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
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
			
			/* ***********************************
			 * crawling data of mutiple products *
			 *************************************/
			if(hasVariations(doc)) {
				
				Elements skuVariations = doc.select("td[align=left] div[style=padding-top:20px;padding-bottom:30px;width:100%;border-bottom:1px solid #E2E2E2;] div > a");
				
				for(int i = 0; i < skuVariations.size(); i++){
					Element e = skuVariations.get(i);
					
					String urlVariation =  e.attr("href");
					
					if(!urlVariation.contains(internalId)){
						doc = this.fetchPageVariation(urlVariation); // if url contains internalID, is the atual url
					
						
						// InternalId
						String internalIdVariation = crawlInternalId(doc);
	
						// Name
						String nameVariation = crawlName(doc);
	
						// Price
						Float priceVariation = crawlMainPagePrice(doc);
						
						// Availability
						boolean availableVariation = crawlAvailability(doc);
	
						// Categories
						ArrayList<String> categoriesVariation = crawlCategories(doc);
						String category1Variation = getCategory(categoriesVariation, 0);
						String category2Variation = getCategory(categoriesVariation, 1);
						String category3Variation = getCategory(categoriesVariation, 2);
	
						// Primary image
						String primaryImageVariation = crawlPrimaryImage(doc);
	
						// Secondary images
						String secondaryImagesVariation = crawlSecondaryImages(doc);
	
						// Description
						String descriptionVariation = crawlDescription(doc);
	
						// Creating the product
						Product productVariation = new Product();
						productVariation.setSeedId(this.session.getSeedId());
						productVariation.setUrl(urlVariation );
						productVariation.setInternalId(internalIdVariation );
						productVariation.setInternalPid(internalPid);
						productVariation.setName(nameVariation );
						productVariation.setPrice(priceVariation );
						productVariation.setAvailable(availableVariation );
						productVariation.setCategory1(category1Variation );
						productVariation.setCategory2(category2Variation );
						productVariation.setCategory3(category3Variation );
						productVariation.setPrimaryImage(primaryImageVariation );
						productVariation.setSecondaryImages(secondaryImagesVariation );
						productVariation.setDescription(descriptionVariation);
						productVariation.setStock(stock);
						productVariation.setMarketplace(marketplace);
	
						products.add(productVariation);
					}
					
				}
			} 

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith(HOME_PAGE + "produto/") || url.startsWith(HOME_PAGE + "/produto/")) return true;
		return false;
	}
	
	/*********************
	 * Product variation *
	 *********************/
	
	private boolean hasVariations(Document doc){
		Element variation = doc.select(".tensao_ativa").first();
		
		if(variation != null) return true;
		
		return false;
	}
	
	private Document fetchPageVariation(String url){
		Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, null);
		
		return doc;
	}
	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("meta[itemprop=sku]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("content").toString().trim();			
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;

		return internalPid;
	}
	
	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().toString().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select("td.produto > div[style] > div[style=font-size:19px;font-weight:bold;color:#0074c5;]").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select("#comprar_maior_1000").first();
		
		if (notifyMeElement != null) {
			return true;
		}
		
		return false;
	}

	private Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<String, Float>();
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new JSONArray();
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".clearfix > img").first();

		if (primaryImageElement != null) {
			String image = primaryImageElement.attr("data-zoom-image").trim();
			if (image.endsWith(".JPG")) image = image.replace(".JPG", ".jpg");
			primaryImage = image;
		}

		return primaryImage.toLowerCase();
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("#gal1 > a");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("data-zoom-image").trim().toLowerCase() );
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".Menu_Navegacao > a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).attr("title").trim() );
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
		Element descriptionElement = document.select(".descricao p").first();

		if (descriptionElement != null) description = description + descriptionElement.html();

		return description;
	}

}
