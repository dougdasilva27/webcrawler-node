package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloSubmarinoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.submarino.com.br/";

	public SaopauloSubmarinoCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String id = this.session.getUrl().split("/")[4];
			String internalID = Integer.toString(Integer.parseInt(id.replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".a-cod-prod span[itemprop=productID]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().trim();
			}

			// Nome
			Elements elementName = doc.select(".mp-title h1 span");
			String name = elementName.text().replace("'","").replace("’","").trim();

			// Preço e disponibilidade
			Float price = null;
			boolean available = true;

			Element elementPrice = doc.select(".mp-pb-to.mp-price").first();

			if(elementPrice == null) {

				elementName = doc.select(".unavailable-product .mp-tit-name");
				name = elementName.text().replace("'","").replace("’","").trim();

				available = false;
				price = null;

			} else {
				available = true;

				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb span[itemprop=\"name\"]"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";

			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {

				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;

			}
			category1 = cat[0];
			category2 = cat[1];
			category3 = cat[2];

			// Imagem primária
			Elements elementPrimaryImage = doc.select(".mp-picture img");
			String primaryImage = elementPrimaryImage.attr("src");

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray secundaryImagesArray = new JSONArray();
			Elements elementSecondaryImages = doc.select(".carousel-item img");

			if(elementSecondaryImages.size() > 1){
				for(int i = 1; i < elementSecondaryImages.size(); i++) {
					Element e = elementSecondaryImages.get(i);
					secundaryImagesArray.put(e.attr("src"));						
				}
			}

			if(secundaryImagesArray.length() > 0) {
				secondaryImages = secundaryImagesArray.toString();
			}

			//Descrição
			String description = "";  

			Element elementProductDetails = doc.select("#productdetails").first();
			if(elementProductDetails != null) {
				description = description + elementProductDetails.html();
			}

			// Marketplace
			JSONArray marketplace = new JSONArray();

			String urlMarketplaceInfo = "http://www.submarino.com.br/parceiros/" + internalID + "/";

			Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

			Elements lines = docMarketplaceInfo.select("table.offers-table tbody tr.partner-line");

			for(Element linePartner: lines) {

				String partnerName = linePartner.select("td .part-info a").first().text().trim().toLowerCase();
				Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

				if( !partnerName.equals("submarino.com.br") ) {

					JSONObject partner = new JSONObject();
					partner.put("name", partnerName);
					partner.put("price", partnerPrice);

					marketplace.put(partner);

				}

			}

			// Segunda análise de disponibilidade olhando o marketplace
			Elements elementSoldDeliveredBy = doc.select("div[data-sku] .pure-g.buy-float-wp .pure-u-3-5 .mp-delivered-by a.bp-lnk");
			Elements elementAlsoSeeOn = doc.select("div.pure-g[data-partner-id] .mp-delivered-by");
			String nameSoldAndDeliveredBy = null;
			String textAlsoSeeOn = null;

			if(elementSoldDeliveredBy.size() > 0) {
				nameSoldAndDeliveredBy = elementSoldDeliveredBy.first().text();
			}

			if(elementAlsoSeeOn.size() > 0) {
				textAlsoSeeOn = elementAlsoSeeOn.first().text();
			}

			if(nameSoldAndDeliveredBy != null && !nameSoldAndDeliveredBy.equals("Submarino")) { // se não for o Submarino que está vendendo e entregando
				if(textAlsoSeeOn != null && textAlsoSeeOn.contains("Submarino")) { // se o produto estiver disponível no Submarino, o id do partner será 03, que é do Submarino
					available = true;
					Elements tmpPrice = doc.select("div.pure-g[data-partner-id] .mp-price");
					if(tmpPrice.size() > 0) { // pegar o preço do Submarino e não o do parceiro dele
						price = Float.parseFloat(tmpPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				}
				else {
					available = false;
					price = null;
				}
			}

			// Estoque
			Integer stock = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setSeedId(this.session.getSeedId());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("http://www.submarino.com.br/produto/") && !url.contains("?loja="));
	}
	
}











//package br.com.lett.crawlers.saopaulo;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//
//import br.com.lett.Main;
//import br.com.lett.crawlers.model.Product;
//import br.com.lett.crawlers.models.ProcessedModel;
//import br.com.lett.util.DataFetcher;
//import br.com.lett.util.Logging;
//import br.com.lett.util.MarketCrawler;
//import edu.uci.ics.crawler4j.crawler.Page;
//import edu.uci.ics.crawler4j.url.WebURL;
//
//public class SaopauloSubmarinoCrawler extends MarketCrawler {
//	
//	private final String SUBMARINO_ID = "03";
//
//	@Override
//	public boolean shouldVisit(Page referringPage, WebURL url) {
//		String href = url.getURL().toLowerCase();
//		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.submarino.com.br/");
//	}
//
//
//	@Override
//	public void extractInformation(Document doc, String seedId, String url, ProcessedModel truco) {
//		super.extractInformation(doc, seedId, url, truco);
//
//		if(url.startsWith("http://www.submarino.com.br/produto/") && !url.contains("?loja=")) {
//
//			Logging.printLogDebug(marketCrawlerLogger, "Página de produto: " + url);
//
//			Elements elementsProductOptions = doc.select(".buy-form .mb-sku-choose .selected"); // caso que tem seletor 110v e 220v
//
//			// Internal Pid
//			String internalPid = null;
//			Element elementInternalPid = doc.select(".a-cod-prod span[itemprop=productID]").first();
//			if (elementInternalPid != null) {
//				internalPid = elementInternalPid.text().toString().replaceAll("[()]", "").trim();
//			}
//
//			// Nome
//			String name = null;
//			Element elementName = doc.select(".mp-title h1").first();
//			if (elementName != null) {
//				name = elementName.attr("title").toString().replace("'","").replace("’","").trim();
//			}
//
//			// Categorias
//			Elements elementCategories = doc.select(".breadcrumb span[itemprop=\"name\"]"); 
//			String category1; 
//			String category2; 
//			String category3;
//
//			String[] cat = new String[3];
//			cat[0] = "";
//			cat[1] = "";
//			cat[2] = "";
//
//			int j = 0;
//			for(int i = 0; i < elementCategories.size(); i++) {
//
//				Element e = elementCategories.get(i);
//				cat[j] = e.text().toString();
//				cat[j] = cat[j].replace(">", "");
//				j++;
//
//			}
//			category1 = cat[0];
//			category2 = cat[1];
//			category3 = cat[2];
//
//			// Imagem primária
//			Elements elementPrimaryImage = doc.select(".mp-picture img");
//			String primaryImage = elementPrimaryImage.attr("src");
//
//			// Imagens secundárias
//			String secondaryImages = null;
//
//			JSONArray secundaryImagesArray = new JSONArray();
//			Elements elementSecondaryImages = doc.select(".carousel-item img");
//
//			if(elementSecondaryImages.size() > 1){
//				for(int i = 1; i < elementSecondaryImages.size(); i++) {
//					Element e = elementSecondaryImages.get(i);
//					secundaryImagesArray.put(e.attr("src"));						
//				}
//			}
//
//			if(secundaryImagesArray.length() > 0) {
//				secondaryImages = secundaryImagesArray.toString();
//			}
//
//			//Descrição
//			String description = "";  
//
//			Element elementProductDetails = doc.select("#productdetails").first();
//			if(elementProductDetails != null) {
//				description = description + elementProductDetails.html();
//			}
//
//			/*
//			 * Pegar informações que são específicas de cada caso (com ou seem variação)
//			 * O nome deve ser alterado também, apendando o nome da variação na frente do nome
//			 * já capturado.
//			 * 
//			 * InternalId
//			 * Name
//			 * Price
//			 * Availability
//			 * Marketplace
//			 */
//
//			if (elementsProductOptions.size() == 0) { // não possui variações
//
//				Logging.printLogDebug(marketCrawlerLogger, "Capturando informações de produto sem variações...");
//
//				// internalId
//				String internalId = null;
//				Element internalIdElement = doc.select(".buy-form [data-sku]").first();
//				if (internalIdElement != null) {
//					internalId = internalIdElement.attr("data-sku").trim();
//				}
//				if (internalId == null) {
//					internalIdElement = doc.select(".mp-title .a-cod-prod [name=codItemFusionWithoutStock]").first();
//					if (internalIdElement != null) {
//						internalId = internalIdElement.attr("value").toString().trim();
//					}
//				}
//
//				// montar o mapa de marketplace
//				String urlMarketplaceInfo = "http://www.submarino.com.br/parceiros/" + internalPid + "/";
//				Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, urlMarketplaceInfo, null, this);
//				Map<String, Float> marketplaceMap = this.extractMarketplace(doc, docMarketplaceInfo);
//
//				// availability
//				boolean available = false;
//				if (marketplaceMap.containsKey("submarino")) {
//					if (marketplaceMap.get("submarino") != null) {
//						available = true;
//					}
//				}
//
//				// price
//				Float price = null;
//				if (marketplaceMap.containsKey("submarino")) {
//					price = marketplaceMap.get("submarino");
//				}
//
//				// marketplace
//				JSONArray marketplace = new JSONArray();
//
//				if (marketplaceMap.containsKey("submarino")) { // remover submarino do mapa de marketplace
//					marketplaceMap.remove("submarino");
//				}
//				for (String partnerName : marketplaceMap.keySet()) {
//					this.addNewPartner(marketplace, partnerName, marketplaceMap.get(partnerName));
//				}				
//
//				// Estoque
//				Integer stock = null;
//
//				Product product = new Product();
//				product.setSeedId(seedId);
//				product.setUrl(url);
//				product.setInternalId(internalId);
//				product.setInternalPid(internalPid);
//				product.setName(name);
//				product.setPrice(price);
//				product.setCategory1(category1);
//				product.setCategory2(category2);
//				product.setCategory3(category3);
//				product.setPrimaryImage(primaryImage);
//				product.setSecondaryImages(secondaryImages);
//				product.setDescription(description);
//				product.setStock(stock);
//				product.setMarketplace(marketplace);
//				product.setAvailable(available);
//
//				// Se estamos em modo clients e não encontramos as informações principais do produto, escalonamos a página para ser reprocessada
//				if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
//					this.crawlerController.scheduleUrlToReprocess(url);
//				}
//
//				else {
//
//					// Print information on console
//					this.printExtractedInformation(product);
//
//					// Upload image to s3
//					if(secondaryImages != null && !secondaryImages.equals("")) {
//						db.uploadImageToAmazon(this, secondaryImages, internalId);
//					}
//
//					// Persist information on database
//					this.persistInformation(product, this.marketId, truco, url);
//
//					count++;
//					if (product.getAvailable()) availableCount++;
//
//					Logging.printLogDebug(marketCrawlerLogger, city, market, " UPDATE [PRODUCTS]" + count + " [AVAILABLE]" + availableCount);
//				}
//
//
//			}
//
//			else { // produto com variação
//
//				Logging.printLogDebug(marketCrawlerLogger, "Capturando informações de produto com variações...");
//
//				ArrayList<Document> docMarketplaceInfoArray = this.fetchMarketplaceInfoArray(elementsProductOptions, internalPid);
//				Map<String, String> idPartnerNameMap = this.createPagePartnersNameAndId(docMarketplaceInfoArray);
//				
//				for (int i = 0; i < elementsProductOptions.size(); i++) {
//					Element option = elementsProductOptions.get(i);
//					
//					String[] partnersIds = option.select("input").first().attr("data-partners").toString().split(","); // ids de parceiros vendendo este produto
//					
//					// internalId
//					String internalId = null;
//					Element internalIdElement = option.select("input").first();
//					if (internalIdElement != null) {
//						internalId = internalIdElement.attr("value").toString().trim();
//					}
//
//					// name
//					String variationName = null;
//					Element variationElement = option.select("input").first();
//					if (variationElement != null) {
//						variationName = name + " - " + variationElement.attr("data-value-name").toString().trim();
//					}
//
//					// montar o mapa de marketplace
//					Document docMarketplaceInfo = docMarketplaceInfoArray.get(0);
//					Map<String, Float> marketplaceMap = this.extractMarketplace(docMarketplaceInfo, idPartnerNameMap, partnersIds);
//
//					// availability
//					boolean available = false;
//					if (marketplaceMap.containsKey("submarino")) {
//						if (marketplaceMap.get("submarino") != null) {
//							available = true;
//						}
//					}
//
//					// price
//					Float price = null;
//					if (marketplaceMap.containsKey("submarino")) {
//						price = marketplaceMap.get("submarino");
//					}
//
//					// marketplace
//					JSONArray marketplace = new JSONArray();
//
//					if (marketplaceMap.containsKey("submarino")) { // remover submarino do mapa de marketplace
//						marketplaceMap.remove("submarino");
//					}
//					for (String partnerName : marketplaceMap.keySet()) {
//						this.addNewPartner(marketplace, partnerName, marketplaceMap.get(partnerName));
//					}				
//
//					// Estoque
//					Integer stock = null;
//
//					Product product = new Product();
//					product.setSeedId(seedId);
//					product.setUrl(url);
//					product.setInternalId(internalId);
//					product.setInternalPid(internalPid);
//					product.setName(variationName);
//					product.setPrice(price);
//					product.setCategory1(category1);
//					product.setCategory2(category2);
//					product.setCategory3(category3);
//					product.setPrimaryImage(primaryImage);
//					product.setSecondaryImages(secondaryImages);
//					product.setDescription(description);
//					product.setStock(stock);
//					product.setMarketplace(marketplace);
//					product.setAvailable(available);
//
//					// Se estamos em modo clients e não encontramos as informações principais do produto, escalonamos a página para ser reprocessada
//					if ( this.missingProduct(product) && Main.mode.equals(Main.MODE_INSIGHTS) ) {
//						this.crawlerController.scheduleUrlToReprocess(url);
//					}
//
//					else {
//
//						// Print information on console
//						this.printExtractedInformation(product);
//
//						// Upload image to s3
//						if(secondaryImages != null && !secondaryImages.equals("")) {
//							db.uploadImageToAmazon(this, secondaryImages, internalId);
//						}
//
//						// Persist information on database
//						this.persistInformation(product, this.marketId, truco, url);
//
//						count++;
//						if (product.getAvailable()) availableCount++;
//
//						Logging.printLogDebug(marketCrawlerLogger, city, market, " UPDATE [PRODUCTS]" + count + " [AVAILABLE]" + availableCount);
//					}
//
//				}
//			}
//			
//		} else {
//			Logging.printLogTrace(marketCrawlerLogger, "Não é uma página de produto!" + seedId);
//
//			if ( Main.mode.equals(Main.MODE_INSIGHTS) ) {
//				this.crawlerController.scheduleUrlToReprocess(url);
//			}
//		}
//
//	}
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//	
//
//
//	private ArrayList<Document> fetchMarketplaceInfoArray(Elements elementsProductOptions, String internalPid) {
//		Logging.printLogDebug(marketCrawlerLogger, "Pegando páginas com informações de lojistas das variações...");
//
//		ArrayList<Document> docMarketplaceInfoArray = new ArrayList<Document>();
//
//		for (Element option : elementsProductOptions) {
//
//			// internalId
//			String internalId = null;
//			Element internalIdElement = option.select("input").first();
//			if (internalIdElement != null) {
//				internalId = internalIdElement.attr("value").toString().trim();
//			}
//
//			String urlMarketplaceInfo = "http://www.submarino.com.br/parceiros/" + internalPid + "/" + "?codItemFusion=" + internalId;
//			Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, urlMarketplaceInfo, null, this);
//			docMarketplaceInfoArray.add(docMarketplaceInfo);					
//		}
//
//		return docMarketplaceInfoArray;
//	}
//
//
//	private HashMap<String, String> createPagePartnersNameAndId(ArrayList<Document> docMarketplaceInfoArray) {
//		HashMap<String, String> idPartnerNameMap = new HashMap<String, String>();
//
//		for (Document marketplaceDocPage : docMarketplaceInfoArray) {
//			Elements lines = marketplaceDocPage.select("table.offers-table tbody tr.partner-line");
//
//			for(Element linePartner: lines) {
//
//				Element partnerNameElement = linePartner.select("td .part-logo a [alt]").first();
//				if (partnerNameElement != null) {
//					String partnerId = linePartner.attr("data-partner-id").toString();
//					String partnerName = partnerNameElement.attr("alt").trim().toLowerCase();
//
//					if (partnerName.contains("submarino")) {		// no html o nome aparece como "submarino.com.br" e não apenas "submarino"
//						idPartnerNameMap.put("submarino", "03");
//					} else { // se não for, inserimos com o nome normal
//						idPartnerNameMap.put(partnerName, partnerId);
//					}
//				}
//			}
//		}
//
//		return idPartnerNameMap;
//	}
//
//
//
//	private Map<String, Float> extractMarketplace(Document marketplaceDocPage, Map<String, String> idPartnerNameMap, String[] partnersIds) {
//		Map<String, Float> marketplace = new HashMap<String, Float>();
//
//		Elements lines = marketplaceDocPage.select("table.offers-table tbody tr.partner-line");
//
//		for(Element linePartner: lines) {
//
//			Element partnerNameElement = linePartner.select("td .part-logo a [alt]").first();
//			if (partnerNameElement != null) {
//				String partnerName = partnerNameElement.attr("alt").trim().toLowerCase();
//				if (partnerName.contains("submarino")) partnerName = "submarino";
//				
//				Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//				String partnerId = idPartnerNameMap.get(partnerName);				
//				
//				if ( this.contains(partnersIds, partnerId) ) {
//					if (partnerName.contains("submarino")) {		// no html o nome aparece como "submarino.com.br" e não apenas "submarino"
//						marketplace.put("submarino", partnerPrice);
//					} else { // se não for, inserimos com o nome normal
//						marketplace.put(partnerName, partnerPrice);
//					}
//				}
//			}
//		}
//
//		return marketplace;
//	}
//
//	private Map<String, Float> extractMarketplace(Document doc, Document marketplaceDocPage) {
//		Map<String, Float> marketplace = new HashMap<String, Float>();
//		
//		// pegando o lojista da página principal
//		String mainPageSellerName = this.extractMainPageSeller(marketplaceDocPage);
//		Float mainPageSellerPrice = this.extractMainPagePrice(marketplaceDocPage);
//		
//		if (mainPageSellerName != null && mainPageSellerPrice != null) {
//			marketplace.put(mainPageSellerName, mainPageSellerPrice);
//		}
//		
//		Elements lines = marketplaceDocPage.select("table.offers-table tbody tr.partner-line");
//		
//		for(Element linePartner: lines) {
//
//			Element partnerNameElement = linePartner.select("td .part-logo a [alt]").first();
//			if (partnerNameElement != null) {
//				String partnerName = partnerNameElement.attr("alt").trim().toLowerCase();
//				Float partnerPrice = Float.parseFloat(linePartner.select(".value-prod").text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//
//				if (partnerName.contains("submarino")) {		// no html o nome aparece como "submarino.com.br" e não apenas "submarino"
//					marketplace.put("submarino", partnerPrice);
//				} else { // se não for, inserimos com o nome normal
//					marketplace.put(partnerName, partnerPrice);
//				}
//			}
//		}
//
//		return marketplace;
//	}
//
//	private Float extractMainPagePrice(Document docMainPage) {
//		Float price = null;
//
//		Element elementPrice = docMainPage.select(".mp-pb-to.mp-price [itemprop='price/salesPrice']").first();
//		if(elementPrice != null) {			
//			price = Float.parseFloat(elementPrice.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
//		}
//
//		return price;
//	}
//
//	private void addNewPartner(JSONArray marketplace, String name, Float price) {
//		JSONObject partner = new JSONObject();
//		partner.put("name", name);
//		partner.put("price", price);
//
//		marketplace.put(partner);
//	}
//	
//	private boolean contains(String[] partnersIds, String id) {
//		for (String s : partnersIds) {
//			if (s.equals(id)) return true;
//		}
//		return false;
//	}
//	
//	private String extractMainPageSeller(Document doc) {
//		String seller = null;
//		Element elementSeller = doc.select(".mp-delivered-by a").first();
//		if (elementSeller != null) {
//			seller = elementSeller.text().toString().toLowerCase();
//		}
//		
//		return seller;
//	}
//
//}