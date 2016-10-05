package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/**
 * 1) Only one page per sku.
 * 2) No variations of skus in a page.
 * 3) No voltage, size or any other selector.
 * 4) InternalId is crawled from a json object embedded inside the html code.
 * this json object is located inside a <script> html tag and is called skuJson_0. 
 * 
 * @author samirleao
 *
 */
public class BrasilFnacCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.fnac.com.br/";

	public BrasilFnacCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			JSONObject skuJson = crawlSkuJsonArray(doc);

			if (skuJson.has("skus")) {

				JSONArray skus = skuJson.getJSONArray("skus");

				JSONObject sku = skus.getJSONObject(0);

				// internal id
				String internalId = null;
				if (skuJson.has("productId")) {
					internalId = String.valueOf(skuJson.getLong("productId"));
				}

				// pid
				String internalPid = null;

				// name
				String name = null;
				if (skuJson.has("name")) {
					name = skuJson.getString("name");
				}

				// availability
				boolean available = false;
				if (skuJson.has("available")) {
					available = skuJson.getBoolean("available");
				}

				// price
				Float price = null;
				if (sku.has("bestPriceFormated") && available) {
					price = Float.parseFloat(sku.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}

				// Categories
				ArrayList<String> categories = crawlCategories(doc);
				String category1 = getCategory(categories, 0);
				String category2 = getCategory(categories, 1);
				String category3 = getCategory(categories, 2);

				// primary image
				String primaryImage = null;
				Elements imageElements = doc.select(".x-images #show .thumbs li a");
				if (imageElements.size() > 0) {
					String imageURL = imageElements.get(0).attr("rel");
					if (imageURL != null) {
						primaryImage = imageURL.trim();
					}
				}

				// Imagens
				String secondaryImages = null;
				JSONArray secondaryImagesArray = new JSONArray();
				if (imageElements.size() > 1) {
					for (int i = 1; i < imageElements.size(); i++) {
						String imageURL = imageElements.get(i).attr("rel");
						if (imageURL != null) {
							secondaryImagesArray.put(imageURL.trim());
						}
					}
				}
				if (secondaryImagesArray.length() > 0) {
					secondaryImages = secondaryImagesArray.toString();
				}

				// Descrição
				String description = "";
				Element info = doc.select("#x-desc-info").first();
				Element specs = doc.select("#x-about").first();
				if (info != null) description += info.html();
				if (specs != null) description += specs.html();

				// Estoque
				Integer stock = null;

				// Marketplace
				JSONArray marketplace = null;

				Product product = new Product();
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

	private boolean isProductPage(String url, Document document) {
		return (url.contains("/p") && document.select(".x-product-details").first() != null);
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".x-product-details .x-breadcrumb ul li");

		for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 becuase the first item is page home
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


	/**
	 * Get the script having a json with the availability information
	 * @return
	 */
	private JSONObject crawlSkuJsonArray(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJson = null;
		JSONArray skuJsonArray = null;

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var skuJson_0 = ")) {

					skuJson = new JSONObject
							(
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1] +
									node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]
									);

				}
			}        
		}

		return skuJson;
	}



}

