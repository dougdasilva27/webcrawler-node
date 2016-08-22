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
import org.openqa.selenium.WebElement;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloPontofrioCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.pontofrio.com.br/";
	private final String MAIN_SELLER_NAME_LOWER = "pontofrio";
	
	public SaopauloPontofrioCrawler(CrawlerSession session) {
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

		if( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// InternalId
			String internalID = this.crawlInternalIdSingleProduct(doc);

			// Pid
			String internalPid = internalID;

			// Name
			String name = this.crawlMainPageName(doc);

			// Price
			Float price = this.crawlMainPagePrice(doc);

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// Primary image
			String primaryImage = this.crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = this.crawlSecondaryImages(doc, primaryImage);

			// Description
			String description = this.crawlDescription(doc);

			// Estoque
			Integer stock = null;
			
			


			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if( hasProductVariations(doc) ) {
				Logging.printLogDebug(logger, session, "Crawling information of more than one product...");


				/* ***************************************************
				 * crawling variations internal ids in case we 		 *
				 * have a voltage selector instead of a sku selector *
				 *****************************************************/
				Map<String, String> voltageIdToInternalIdMap = null;
				if ( isVoltageSelector(doc) ) {
					Logging.printLogDebug(logger, session, "Voltage selector detected for variations...");
					Logging.printLogDebug(logger, session, "Will use remote webdriver to fetch data...");

					if (this.webdriver == null) {
						this.webdriver = new CrawlerWebdriver();
					}

					voltageIdToInternalIdMap = this.crawlInternalIds(this.session.getUrl());

					// check if the two internalIds are equal, if they are, the fetching with webdriver went wrong
					ArrayList<String> ids = new ArrayList<String>();
					for (String s : voltageIdToInternalIdMap.keySet()) {
						ids.add(voltageIdToInternalIdMap.get(s));
					}
					String id1 = ids.get(0);
					String id2 = ids.get(1);
					if (id1 != null && id2 != null) {
						if (id1.equals(id2)) {
							Logging.printLogError(logger, session, "The waiting on webdriver was not sufficient...discarding read");
							return products;
						}
					} else {
						Logging.printLogError(logger, session, "One or both the ids are null...discarding read");
						return products;
					}

				}

				Elements productVariationElements = this.crawlSkuOptions(doc);
				for(int i = 0; i < productVariationElements.size(); i++) {

					Element sku = productVariationElements.get(i);

					if( !sku.attr("value").equals("") ) { // se tem o atributo value diferente de vazio, então é uma variação de produto

						// InternalId
						String variationInternalID = null;
						if ( isVoltageSelector(doc) ) {
							if (voltageIdToInternalIdMap != null) {
								variationInternalID = voltageIdToInternalIdMap.get( sku.attr("value") );
							} else {
								Logging.printLogError(logger, session, "Has a voltage selector but the map of voltage to internalId is null...");
								return products;
							}
						} else {
							variationInternalID = sku.attr("value").trim();
						}

						// Fetch marketplace page for this sku
						Document docMarketplaceInfo = this.fetchMarketplacePageForMultipleSkus(this.session.getUrl(), variationInternalID);

						// Getting name from marketplace page
						String variationName = docMarketplaceInfo.select(".fn.name a").text();

						// Marketplace map
						Map<String, Float> marketplaceMap = this.crawlMarketplace(docMarketplaceInfo);

						// Assemble marketplace from marketplace map
						JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

						// Availability and Price
						boolean available = false;
						Float variationPrice = null;

						for (String seller : marketplaceMap.keySet()) {
							if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
								available = true;
								variationPrice = marketplaceMap.get(seller);
							}
						}

						if( !available ) price = null;

						Product product = new Product();
						product.setUrl(this.session.getUrl());
						product.setSeedId(this.session.getSeedId());
						product.setInternalId(variationInternalID);
						product.setInternalPid(internalPid);
						product.setName(variationName);
						product.setPrice(variationPrice);
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
					}
				}
			}


			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {

				// Fetch marketplace page
				Document docMarketplaceInfo = fetchMarketplacePageForSingleProduct(this.session.getUrl());

				// Marketplace map
				Map<String, Float> marketplaceMap = this.crawlMarketplace(docMarketplaceInfo);

				// Assemble marketplace from marketplace map
				JSONArray marketplace = this.assembleMarketplaceFromMap(marketplaceMap);

				// Availability and Price
				boolean available = false;
				for (String seller : marketplaceMap.keySet()) {
					if (seller.equals(MAIN_SELLER_NAME_LOWER)) {
						available = true;
						price = marketplaceMap.get(seller);
					}
				}

				if( !available ) price = null;

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
		Element elementInternalID = document.select(".fn.name span").first();

		if (elementInternalID != null) return true;
		return false;
	}



	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Element selectProductVariation = document.select(".produtoSku .listaSku.selSku").first();
		if( selectProductVariation != null ) return true;

		selectProductVariation = document.select(".produtoSku .lista-voltagem.sel-voltagem").first();
		if ( selectProductVariation != null ) return true;

		return false;
	}

	//	private boolean isSkuSelector(Document document) {
	//		Element selectProductVariation = document.select(".produtoSku .listaSku.selSku").first();
	//		if( selectProductVariation != null ) return true;
	//
	//		return false;
	//	}

	private boolean isVoltageSelector(Document document) {		
		Element selectProductVariation = document.select(".produtoSku .lista-voltagem.sel-voltagem").first();
		if ( selectProductVariation != null ) return true;

		return false;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlInternalIdSingleProduct(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".fn.name span").first();
		if (internalIdElement != null) {
			internalId = Integer.toString(Integer.parseInt(internalIdElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
		}

		return internalId;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element elementprice = document.select(".sale.price").first();
		if(elementprice != null) {
			price = Float.parseFloat(elementprice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private Document fetchMarketplacePageForSingleProduct(String mainProductURL) {
		String urlMarketplaceInfo = (mainProductURL.split(".html")[0] + "/lista-de-lojistas.html");
		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

		return docMarketplaceInfo;
	}


	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private Document fetchMarketplacePageForMultipleSkus(String mainProductURL, String skuVariationId) {
		String regex = "(-)(\\d+)(\\/lista-de-lojistas\\.html)";
		String urlMarketplaceInfo = (mainProductURL.split(".html")[0] + "/lista-de-lojistas.html").replaceAll(regex, "$1" + skuVariationId + "$3");
		Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);

		return docMarketplaceInfo;
	}

	private Map<String, String> crawlInternalIds(String url) {
		Map<String, String> voltageIdToInternalId = new HashMap<String, String>();

		// Load the first product url
		Document docMainPageSKU1 = Jsoup.parse( webdriver.loadUrl(url) );

		// Load the second product url
		Document docMainPageSKU2 = null;
		List<WebElement> options = webdriver.findElementsByCssSelector(".produtoSku .lista-voltagem.sel-voltagem option");

		for(WebElement option : options) {
			String voltageId = option.getAttribute("value").trim();
			String internalId = null;

			if ( !option.isSelected() ) {
				option.click();

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				docMainPageSKU2 = Jsoup.parse( webdriver.getCurrentPageSource() );
				internalId = crawlInternalIdSingleProduct(docMainPageSKU2);

			} else {
				internalId = crawlInternalIdSingleProduct(docMainPageSKU1);
			}

			voltageIdToInternalId.put(voltageId, internalId);
		}

		return voltageIdToInternalId;
	}

	private Elements crawlSkuOptions(Document document) {
		Elements skuOptions = null;

		skuOptions = document.select(".produtoSku .listaSku.selSku option");
		if (skuOptions.size() == 0) {
			skuOptions = document.select(".produtoSku .lista-voltagem.sel-voltagem option");
		}

		return skuOptions;
	}

	/*******************
	 * General methods *
	 *******************/

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".photo").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("src").trim();	
			if(primaryImage.contains("indisponivel.gif")) primaryImage = "";
		}

		return primaryImage;
	}

	private String crawlMainPageName(Document document) {
		Elements elementName = document.select(".fn.name b");
		String name = elementName.text().replace("'", "").replace("’", "").trim();

		return name;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumb a span");

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

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;
		Elements elementsSecondaryImages = document.select(".boxImg .carouselBox ul li a");
		JSONArray secondaryImagesArray = new JSONArray();

		if(elementsSecondaryImages.size() > 0) {
			for(int i = 0; i < elementsSecondaryImages.size(); i++) {
				Element e = elementsSecondaryImages.get(i);
				String image = e.attr("href");
				if( !image.equals(primaryImage) ) {
					secondaryImagesArray.put(image);
				}
			}
		}
		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private Map<String, Float> crawlMarketplace(Document documentMarketplaceInfo) {
		Map<String, Float> marketplace = new HashMap<String, Float>();

		Elements lines = documentMarketplaceInfo.select("table#sellerList tbody tr");

		for(Element linePartner: lines) { // olhar cada parceiro
			Element elementPartnerName = linePartner.select("td.lojista div a.seller").first();
			String partnerName = "";
			if(elementPartnerName != null) {
				partnerName = elementPartnerName.text().trim().toLowerCase();

				Element elementPartnerPrice = linePartner.select(".valor").first();
				Float partnerPrice = null;
				if(elementPartnerPrice != null) {
					partnerPrice = Float.parseFloat(elementPartnerPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}

				marketplace.put(partnerName, partnerPrice);
			}			
		}

		return marketplace;
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(MAIN_SELLER_NAME_LOWER) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				seller.put("price", marketplaceMap.get(sellerName));

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private String crawlDescription(Document document) {
		Elements elementDescription = document.select("#detalhes");
		String description = elementDescription.html().trim();

		return description;
	}

}
