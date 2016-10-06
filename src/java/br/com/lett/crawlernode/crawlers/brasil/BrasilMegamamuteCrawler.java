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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


public class BrasilMegamamuteCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.megamamute.com.br/";
	private final String MAIN_SELLER_LOWER_CASE = "megamamute";

	public BrasilMegamamuteCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc, this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


			Element elementInternalId = doc.select("#___rc-p-sku-ids").first();
			String[] internalIds = null;
			if (elementInternalId != null) {
				internalIds = elementInternalId.attr("value").trim().split(",");
			}

			for (String skuId : internalIds) {

				// ID interno
				String internalId = skuId;

				// Pid
				String internalPid = internalId;

				// requisitar informações da API
				JSONArray jsonArrayAPI = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, ("http://www.megamamute.com.br/produto/sku/" + internalId), null, null);
				JSONObject productJsonAPI = (JSONObject) jsonArrayAPI.get(0);

				// Nome
				String name = productJsonAPI.getString("Name");

				// Categoria
				String category1 = "";
				String category2 = "";
				String category3 = "";
				Elements elementCategories = doc.select(".x-breadcrumb .bread-crumb ul li a");
				ArrayList<String> categories = new ArrayList<String>();
				for(Element e : elementCategories) {
					String tmp = e.text().trim();
					if( !tmp.equals("megamamute") ) {
						categories.add(tmp);
					}
				}
				for (String c : categories) {
					if (category1.isEmpty()) {
						category1 = c;
					} else if (category2.isEmpty()) {
						category2 = c;
					} else if (category3.isEmpty()) {
						category3 = c;
					}
				}

				// Imagens
				JSONArray images = productJsonAPI.getJSONArray("Images");
				String primaryImage = null;
				String secondaryImages = null;
				JSONArray secondaryImagesArray = new JSONArray();

				for(int i = 0; i < images.length(); i++) {
					JSONArray tmpArray = images.getJSONArray(i);
					JSONObject image = tmpArray.getJSONObject(0);
					if(primaryImage == null) {
						primaryImage = image.getString("Path");
					} else {
						secondaryImagesArray.put(image.getString("Path"));
					}
				}
				if (secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				}

				// Descrição
				String description = "";
				Element elementDescription = doc.select(".x-description-group .x-item .productDescription").first();
				Element elementEspecification = doc.select("#caracteristicas").first();
				if(elementDescription != null) {
					description = description + elementDescription.html();
				}
				if(elementEspecification != null) {
					description = description + elementEspecification.html();
				}

				// Estoque
				Integer stock = null;

				// Marketplace map
				Map<String, Float> marketplaceMap = extractMarketplace(productJsonAPI);

				// Availability
				boolean available = crawlAvailability(marketplaceMap);

				// Price
				Float price = crawlPrice(marketplaceMap);

				// Marketplace
				JSONArray marketplace = assembleMarketplaceFromMap(marketplaceMap);


				Product product = new Product();
				product.setUrl(this.session.getOriginalURL());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private boolean crawlAvailability(Map<String, Float> marketplaceMap) {
		for (String seller : marketplaceMap.keySet()) {
			if (seller.equals(MAIN_SELLER_LOWER_CASE)) {
				if ( marketplaceMap.get(seller).equals(0.0f) ) {
					return false;
				}
			}
		}
		return true;
	}

	private Float crawlPrice(Map<String, Float> marketplaceMap) {
		Float price = null;

		for (String seller : marketplaceMap.keySet()) {
			if (seller.equals(MAIN_SELLER_LOWER_CASE)) {
				if ( !marketplaceMap.get(seller).equals(0.0f) ) {
					price = marketplaceMap.get(seller);
				}
			}
		}

		return price;
	}

	private Map<String, Float> extractMarketplace(JSONObject skuJson) {
		Map<String, Float> marketplaceMap = new HashMap<String, Float>();

		JSONArray skuSellers = skuJson.getJSONArray("SkuSellersInformation");

		for (int i = 0; i < skuSellers.length(); i++) {
			JSONObject seller = skuSellers.getJSONObject(i);

			String sellerName = seller.getString("Name").trim().toLowerCase();
			Float sellerPrice = (float) seller.getDouble("Price");

			marketplaceMap.put(sellerName, sellerPrice);
		}

		return marketplaceMap;		
	}

	private JSONArray assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		JSONArray marketplace = new JSONArray();

		for (String seller : marketplaceMap.keySet()) {
			if ( !seller.equals(MAIN_SELLER_LOWER_CASE) ) { 
				Float price = (float) marketplaceMap.get(seller);

				JSONObject partner = new JSONObject();
				partner.put("name", seller);
				partner.put("price", price);

				marketplace.put(partner);
			}
		}

		return marketplace;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document, String url) {
		return (document.select("#___rc-p-sku-ids").first() != null && url.endsWith("/p"));
	}

}
