package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;



public class SaopauloShoptimeCrawler extends Crawler {
	
	public SaopauloShoptimeCrawler(CrawlerSession session) {
		super(session);
	}

	private final String SHOPTIME_ID = "01";
	private final String HOME_PAGE = "http://www.shoptime.com.br/";
	
	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product>  extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();
		
		if ( isProductPage(session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());
			
			//Variações indisponíveis
			boolean allVariationsUnnavailable = this.allVariationsAreUnnavailable(doc);

			// InternalId
			String internalId = crawlInternalIdFirstPiece(doc);

			// First Piece internalId
			String internalIDFirstPiece = internalId;

			// internalPid
			String internalPid = internalIDFirstPiece;

			// Variations
			boolean hasMoreProducts = this.hasMoreProducts(doc);
			
			// Name
			String name = this.crawlName(doc);
			
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
			
			Elements elementsProductOptions = doc.select(".pure-select option");
			if(elementsProductOptions.size() < 1){
				elementsProductOptions = doc.select(".pure-input-1 option");
			}
			
		
			if(elementsProductOptions.size() > 0){
				for(Element e : elementsProductOptions){				
					
					String variationInternalId = internalIDFirstPiece + "-" + e.attr("value");
					
					//variation name
					String variation = crawlNameVariation(e);
					
					// Name
					String nameVariations = name;
					
					if(variation != null){
						nameVariations += " - " + variation;
					}
					
					// Get ids partners
					Map<String, String> mapPartners = crawlIdPartners(e);
					
					// Document market places
					Document docMarketplace  = fetchMarketplaceInfoDoc(internalPid, e.attr("value"), allVariationsUnnavailable);
	
					// Marketplace map
					Map<String, Float> marketplaceMap = crawlMarketplace(doc, docMarketplace, mapPartners, hasMoreProducts, internalPid);
					
					// Availability
					boolean available = crawlAvailability(marketplaceMap);
					
					// Price
					Float price = crawlPrice(marketplaceMap, available);
	
					// Marketplace
					JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
					
					// Creating the product
					Product product = new Product();
					
					product.setUrl(session.getUrl());
					product.setInternalId(variationInternalId);
					product.setInternalPid(internalPid);
					product.setName(nameVariations);
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
				}
				
				
			} else {
				
				// nesses casos o crawler não estava pegando o nome corretamente
				// coloquei esse seletor aqui para consertar na urgência
				Element unavailableProductName = doc.select("h1.mp-tit-name[itemprop=name]").first();
				if (unavailableProductName != null) name = unavailableProductName.text().trim();
				
				// Montando o internalId
				String secondPartInternalId = crawlInternalIdSingleProduct(doc, internalIDFirstPiece);
				
				internalId += "-" + secondPartInternalId;
				
				// Document market places
				Document docMarketplace  = fetchMarketplaceInfoDoc(internalPid, secondPartInternalId, false);

				// Marketplace map
				Map<String, Float> marketplaceMap = crawlMarketplace(doc, docMarketplace, null, false, internalPid);
				
				// Availability
				boolean available = crawlAvailability(marketplaceMap);
				
				// Price
				Float price = crawlPrice(marketplaceMap, available);
				
				// Marketplace
				JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
				
				// Creating the product
				Product product = new Product();

				product.setUrl(session.getUrl());
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
		if ( url.startsWith("http://www.shoptime.com.br/produto/") ) return true;
		return false;
	}

	
	/*******************
	 * General methods *
	 *******************/
	
	private String crawlInternalIdFirstPiece(Document doc){
		String internalID = null;

		Element elementInternalID = doc.select(".p-name#main-product-name .p-code").first();
		if (elementInternalID != null) {
			internalID =  elementInternalID.text().split(" ")[1].replace(")", " ").trim() ;
		}

		return internalID;
	}

	private boolean allVariationsAreUnnavailable(Document doc){
		//Variações indisponíveis
		boolean allVariationsUnnavailable = false;

		Elements elementsProductOptions = doc.select(".pure-select option"); // caso que tem seletor 110v e 220v
		if(elementsProductOptions.size() < 1){
			allVariationsUnnavailable = true;
		}

		return allVariationsUnnavailable;
	}


	private boolean hasMoreProducts(Document doc) {
		if ( doc.select(".pure-select option").size() > 0 ){
			return true;
		}

		if ( doc.select(".pure-input-1 option").size() > 0 ){
			return true;
		}

		return false;
	}


	private String crawlInternalIdSingleProduct(Document doc, String id){
		String secondInternalId = null;
		
		// montando restante do internalId
		Element secondPartInternalId = doc.select(".toggle-container[data-sku]").first();
		if (secondPartInternalId != null) {
			secondInternalId = secondPartInternalId.attr("data-sku").trim();
		} else {
			secondPartInternalId = doc.select("input[name=soldout.skus]").first();
			if (secondPartInternalId != null) {
				secondInternalId = secondPartInternalId.attr("value").trim();
			}
		}
		
		return secondInternalId;
	}
	
	private String crawlName(Document doc) {
		String name = null;
		Element elementName = doc.select("#main-product-name").first();

		if (elementName != null) {
			name = elementName.textNodes().get(0).toString().replace("'", "").replace("’","").trim();
		}

		return name;
	}

	private String crawlNameVariation(Element e) {
		String variation = e.text().trim();
	
		return variation;
	}
	
	private Float crawlPrice(Map<String, Float> marketplaceMap, boolean available) {
		Float price = null;
		
		if(available){
			price = marketplaceMap.get("shoptime");
		}

		return price;
	}
	
	private boolean crawlAvailability(Map<String, Float> marketplaceMap) {
		boolean available = false;
		
		for (String partnerName : marketplaceMap.keySet()) {
			if (partnerName.equals("shoptime")) { // se o shoptime está no mapa dos lojistas, então o produto está disponível
				available = true;
			}
		}
		
		return available;
	}
	
	private Document fetchMarketplaceInfoDoc(String internalPid, String internalId, boolean allVariationsUnnavailable) {
		Document docMarketplace = null;
	
		if(!allVariationsUnnavailable){
			String urlMarketplaceInfo = "http://www.shoptime.com.br/parceiros/"+ internalPid +"/?codItemFusion="+ internalId;
			String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null).trim();
			
			try {
				docMarketplace = Jsoup.parse(response);
			} catch (Exception e) {
				docMarketplace = new Document(response);
			}
			
		}

		return docMarketplace;
	}
	
	private Map<String, String> crawlIdPartners(Element e){
		Map<String, String> mapPartners = new HashMap<String, String>();
		
		String partnersIdVector[] = e.attr("data-partners").split(","); // pegando os ids dos parceiros que vendem essa variação
		
		for (int k = 0; k < partnersIdVector.length; k++) {
			mapPartners.put(partnersIdVector[k], "");
		}
		
		return mapPartners;
	}

	private Map<String, Float> crawlMarketplace(Document doc, Document marketplaceDocPage, Map<String, String> partnersIds, boolean hasVariations, String pid) {
		Map<String, Float> marketplace = new HashMap<String, Float>();

	
		// pegando o lojista da página principal
		String mainPageSellerName = this.extractMainPageSeller(doc);
		Float mainPageSellerPrice = this.extractMainPagePrice(doc);
		
		if (mainPageSellerName != null && mainPageSellerPrice != null) {
			marketplace.put(mainPageSellerName, mainPageSellerPrice);
		}
		
					
		if(hasPidInMarketplacePage(marketplaceDocPage, pid)){
		
			Elements lines = marketplaceDocPage.select(".panel.nospacing .partners ul li[data-partner-id]");
	
			for(Element linePartner: lines) {
	
				Element partnerNameElement = linePartner.select(".partner-info > a").first();
				if (partnerNameElement != null) {
					String partnerName = partnerNameElement.text().trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.attr("data-partner-value"));
					String idPartner = lines.attr("data-partner-id").trim();
					
					if(!marketplace.containsKey(partnerName)){
						if (partnerName.contains("shoptime")) partnerName = "shoptime";
						
						if(partnersIds != null){
							if(partnersIds.containsKey(idPartner) || (partnerName.equals("shoptime") && partnersIds.containsKey(SHOPTIME_ID))){
								marketplace.put(partnerName, partnerPrice);
							}
						} else {
							marketplace.put(partnerName, partnerPrice);
						}
					}
				}
			}
		}
		

		return marketplace;
	}
	
	private boolean hasPidInMarketplacePage(Document doc, String internalPid){
		if(doc != null){
			Element hasPid = doc.select("input[name=codProdFusion]").first();
			
			if(hasPid != null){
				String pid = hasPid.attr("value").trim();
				
				if(pid.equals(internalPid)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();
		for (String partnerName : marketplaceMap.keySet()) {
			if (!partnerName.equals("shoptime")) { 
				
				JSONObject partner = new JSONObject();
				partner.put("name", partnerName);
				partner.put("price", marketplaceMap.get(partnerName));

				marketplace.put(partner);
			}
		}
		
		return marketplace;
	}
	
	private String extractMainPageSeller(Document doc) {
		String seller = null;
		Element elementSeller = doc.select(".p-deliveredby-store").first();
		if (elementSeller != null) {
			seller = elementSeller.text().toString().toLowerCase();
		}
		
		return seller;
	}
	
	private Float extractMainPagePrice(Document doc) {
		Float price = null;
		Element elementPriceFirst = doc.select("div .p-prices .p-price .value").first();
		
		if (elementPriceFirst != null) {
			price = Float.parseFloat(elementPriceFirst.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	
	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element primaryImageElement = doc.select(".p-gallery-image.swiper-slide a").first();
		
		if(primaryImageElement != null){
			primaryImage = primaryImageElement.attr("href").trim();
			if(!primaryImage.startsWith("http")){
				primaryImage = null;
			}
		}
		
		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc, String primaryImage) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
	
		Elements secondaryImagesElements = doc.select(".p-gallery-image.swiper-slide a");
		
		for(int i = 1; i < secondaryImagesElements.size(); i++) { // começando da segunda imagem porque a primeira é a imagem primária						
			String secondaryImage = secondaryImagesElements.get(i).attr("href").trim();
			if (!secondaryImage.equals(primaryImage)) {
				secondaryImagesArray.put( secondaryImage );
			}
		}
		
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb span a");

		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).text().replace(">", "").trim() );
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlDescription(Document doc) {
		String description = "";	
		Element elementBasicInfo = doc.select("#basicinfotoggle").first();
		Element elementTechSpec = doc.select("#informacoes-tecnicas").first();
		
		if (elementBasicInfo != null) description = description + elementBasicInfo.html();
		if (elementTechSpec != null) description = description + elementTechSpec.html();

		return description;
	}

}