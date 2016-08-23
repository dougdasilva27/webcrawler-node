package br.com.lett.crawlernode.crawlers.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class BrasilEletrocityCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.eletrocity.com.br/";
	private final String ELETROCITY_SELLER_NAME_LOWER_CASE = "eletrocity";

	public BrasilEletrocityCrawler(CrawlerSession session) {
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

			/* *********************************************************
			 * crawling data common to both the cases of product page  *
			 ***********************************************************/

			// Pid
			String internalPid = crawlInternalPid(doc);

			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = "";
			String category2 = "";
			String category3 = "";
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c;
				} else if (category2.isEmpty()) {
					category2 = c;
				} else if (category3.isEmpty()) {
					category3 = c;
				}
			}

			// Description
			String description = crawlDescription(doc);

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Stock
			Integer stock = null;


			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if ( hasProductVariations(doc) ) {
				Logging.printLogDebug(logger, session, "Crawling multiple variations of a product...");

				// geting list of skus in page
				Elements skusElements = doc.select(".skuList");

				for (int i = 0; i < skusElements.size(); i++) {
					Element sku = skusElements.get(i);

					// InternalId
					String internalId = crawlInternalForElement(doc, i);

					// Name
					String name = crawlNameFromElement(sku);

					// Price
					Float price = crawlPriceFromElement(sku);

					// Marketplace map
					Map<String, Float> marketplaceMap = crawlMarketplaceFromElement(sku);

					// Marketplace
					JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);

					// Availability and Price from marketplace
					boolean available = false;
					boolean hasNotifyMe = variationHasNotifyMe(sku);
					boolean hasMainSellerOnMarketplace = hasMainSellerOnMarketplace(marketplaceMap);
					
					if  (hasNotifyMe) {
						available = false;
					} else {
						if (hasMainSellerOnMarketplace) {
							available = true;
						} else {
							available = false;
						}
					}					
					if (!available) price = null;

					// Creating the product
					Product product = new Product();
					product.setSeedId(this.session.getSeedId());
					product.setUrl(this.session.getUrl());
					product.setInternalId(internalId);
					product.setInternalPid(internalPid);
					product.setName(name);
					product.setAvailable(available);
					product.setPrice(price);
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
			}

			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {
				Logging.printLogDebug(logger, session, "Crawling only one product...");

				// InternalId
				String internalId = crawlInternalId(doc);

				// Name
				String name = crawlName(doc);

				// Price
				Float price = crawlMainPagePrice(doc);

				// Marketplace map
				Map<String, Float> marketplaceMap = crawlMarketplace(doc);
				
				// Marketplace
				JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);
				
				// Availability and Price from marketplace
				boolean available = false;
				boolean hasNotifyMe = hasNotifyMe(doc);
				boolean hasMainSellerOnMarketplace = hasMainSellerOnMarketplace(marketplaceMap);
				
				if  (hasNotifyMe) {
					available = false;
				} else {
					if (hasMainSellerOnMarketplace) {
						available = true;
					} else {
						available = false;
					}
				}					
				if (!available) price = null;

				// Creating the product
				Product product = new Product();
				product.setSeedId(this.session.getSeedId());
				product.setUrl(this.session.getUrl());
				product.setInternalId(internalId);
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

	private boolean isProductPage(String url) {
		if (url.endsWith("/p")) return true;
		return false;
	}


	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasProductVariations(Document document) {
		Elements skuList = document.select(".gtp-compra .skuList");

		if (skuList.size() > 1) return true;
		return false;
	}


	/*******************************
	 * Single product page methods *
	 *******************************/

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".gtp-compra h1 .fn").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#___rc-p-sku-ids").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").toString().trim();			
		}

		return internalId;
	}
	
	private boolean hasNotifyMe(Document document) {
		Element notifymeElement = document.select(".portal-notify-me-ref").first();
		
		if (notifymeElement != null) return true;
		return false;
	}


	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select(".descricao-preco .skuBestPrice").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}
	
	private Map<String, Float> crawlMarketplace(Document document) {
		Map<String, Float> marketplace = new HashMap<String, Float>();
		Element sellerElement = document.select(".seller-description .seller-name a").first();
		
		if (sellerElement != null) {
			String sellerName = sellerElement.text().toString().trim().toLowerCase();
			Float sellerPrice = this.crawlMainPagePrice(document);

			marketplace.put(sellerName, sellerPrice);
		}


		return marketplace;
	}


	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private String crawlNameFromElement(Element sku) {
		String name = null;
		Element nameElement = sku.select(".nomeSku").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}


	private String crawlInternalForElement(Document document, int i) {
		String internalId = null;
		Element internalIdElement = document.select("#___rc-p-sku-ids").first();

		if (internalIdElement != null) {
			String[] ids = internalIdElement.attr("value").toString().split(",");			
			if (i <= ids.length - 1) {
				internalId = ids[i];
			}
		}

		return internalId;
	}
	
	private boolean variationHasNotifyMe(Element sku) {
		Element notifymeElement = sku.select(".portal-notify-me-ref").first();
		
		if (notifymeElement != null) return true;
		return false;
	}

	private Map<String, Float> crawlMarketplaceFromElement(Element sku) {
		Map<String, Float> marketplace = new HashMap<String, Float>();

		Element sellerElement = sku.select("a[href^=/seller-info?]").first();
		if (sellerElement != null) {
			String sellerName = sellerElement.text().toString().trim().toLowerCase();
			Float sellerPrice = crawlPriceFromElement(sku);

			marketplace.put(sellerName, sellerPrice);
		}


		return marketplace;
	}


	private Float crawlPriceFromElement(Element sku) {
		Float price = null;
		Element priceElement = sku.select(".preco .valor-por").first();

		if (priceElement != null) {
			price = Float.parseFloat(priceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element internalPidElement = document.select("#___rc-p-id").first();

		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value").toString().trim();
		}

		return internalPid;
	}
	
	private boolean hasMainSellerOnMarketplace(Map<String, Float> marketplaceMap) {
		for (String seller : marketplaceMap.keySet()) {
			if (seller.equals(ELETROCITY_SELLER_NAME_LOWER_CASE)) {
				return true;
			}
		}
		
		return false;
	}


	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionSpecElement = document.select("section .gtp-descricao.spec").first();

		if (descriptionSpecElement != null) {
			description = descriptionSpecElement.html();
		}

		return description;
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for(String sellerName : marketplaceMap.keySet()) {
			if ( !sellerName.equals(ELETROCITY_SELLER_NAME_LOWER_CASE) ) {
				JSONObject seller = new JSONObject();
				seller.put("name", sellerName);
				seller.put("price", marketplaceMap.get(sellerName));

				marketplace.put(seller);
			}
		}

		return marketplace;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Elements imagesElement = document.select(".thumbs li #botaoZoom");

		if (imagesElement.size() > 0) {
			primaryImage = imagesElement.get(0).attr("zoom").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements imagesElement = document.select(".thumbs li #botaoZoom");

		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("zoom").trim() );
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".wrapped .bread-crumb ul li a");
		
		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
		}
		
		return categories;
	}

}
