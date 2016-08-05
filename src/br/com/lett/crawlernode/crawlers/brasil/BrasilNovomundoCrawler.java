package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilNovomundoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.novomundo.com.br/";

	public BrasilNovomundoCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".bread-crumb li a");
			for (int i = 1; i < elementCategories.size(); i++) {
				String c = elementCategories.get(i).text().trim();
				if (category1.isEmpty()) {
					category1 = c;
				} else if (category2.isEmpty()) {
					category2 = c;
				} else if (category3.isEmpty()) {
					category3 = c;
				}
			}


			Element ids = doc.select("#___rc-p-sku-ids").first();

			/*
			 * Já tratamos os dois casos de uma vez. O caso em que temos mais de uma variação
			 * na mesma página, e o caso em que não temos variação. Para cada id interno do produto,
			 * capturamos as informações através da API da Novomundo. 
			 */
			if (ids != null) {

				// Pegandos os ids internos dos produtos
				String[] productsIds = ids.attr("value").split(",");

				for (String id : productsIds) {

					String internalId = null;
					String internalPid = null;
					String name = null;
					Boolean available = null;
					Float price = null;
					String primaryImage = null;
					String secondaryImages = null;
					JSONArray marketplace = new JSONArray();
					Integer stock = null;

					// Pegar dados da API
					JSONObject productInfoJSON = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, ("http://www.novomundo.com.br/produto/sku/" + id.trim()), null, null).getJSONObject(0);

					// ID interno
					internalId = String.valueOf(productInfoJSON.getInt("Id"));

					// Pid
					internalPid = null;
					Element internalPidElement = doc.select("#___rc-p-id").first();
					if (internalPidElement != null) {
						internalPid = internalPidElement.attr("value");
					}

					// Nome
					if (productInfoJSON.has("Name")) {
						name = productInfoJSON.getString("Name");
					}

					// Disponibilidade
					if (productInfoJSON.has("Availability")) {
						available = productInfoJSON.getBoolean("Availability");
					}

					// Preço
					price = null;

					if (available != null && available == true) {
						JSONArray skuSellersInfo = productInfoJSON.getJSONArray("SkuSellersInformation");
						for (int i = 0; i < skuSellersInfo.length(); i++) {
							JSONObject seller = skuSellersInfo.getJSONObject(i);
							String sellerId = seller.getString("SellerId");
							String sellerName = seller.getString("Name");
							Float sellerPrice = (float)seller.getDouble("Price");
							Integer sellerStock = seller.getInt("AvailableQuantity");

							// Marketplace
							if (sellerId.equals("1")) { // Novomundo id = 1
								price = sellerPrice; 
								stock = sellerStock;
							} else { // é um parceiro
								JSONObject partner = new JSONObject();
								partner.put("name", sellerName);
								partner.put("price", sellerPrice);

								marketplace.put(partner);
							}
						}
					}

					// Imagens
					primaryImage = null;
					secondaryImages = null;
					JSONArray secondaryImagesArray = new JSONArray();
					JSONArray allImagesInfo = null;
					if (productInfoJSON.has("Images")) {
						allImagesInfo = productInfoJSON.getJSONArray("Images");
					}

					if (allImagesInfo != null) {
						for (int i = 0; i < allImagesInfo.length(); i++) {
							JSONArray images = (JSONArray) allImagesInfo.get(i);
							JSONObject largestImage = extractLargestImage(images);

							if (largestImage != null) {
								if (largestImage.getBoolean("IsMain")) {
									primaryImage = largestImage.getString("Path");
								} else {
									secondaryImagesArray.put(largestImage.getString("Path"));
								}
							}
						}

						if (secondaryImagesArray.length() > 0) {
							secondaryImages = secondaryImagesArray.toString();
						}
					}

					// Descrição
					String description = "";
					Element elementDescription = doc.select(".product-menu .productDescription").first();
					Element elementSpecs = doc.select("#caracteristicas").first();
					if (elementDescription != null) {
						description = description + elementDescription.html();
					}
					if (elementSpecs != null) {
						description = description + elementSpecs.html();
					}

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
		Element elementProduct = document.select(".line.product-info").first();
		return (url.endsWith("/p") && elementProduct != null);		
	}
	

	private JSONObject extractLargestImage(JSONArray images) {
		for (int i = 0; i < images.length(); i++) {
			JSONObject image = (JSONObject) images.get(i);
			String path = image.getString("Path");

			if (path.contains("-1000-1000")) {
				return image;
			}
		}

		return null;
	}
}
