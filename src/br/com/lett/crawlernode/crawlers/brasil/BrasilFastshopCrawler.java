package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilFastshopCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.fastshop.com.br/";
	private final String HOME_PAGE_HTTPS = "https://www.fastshop.com.br";

	public BrasilFastshopCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && ( (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS)) ); 
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			String script = getDataLayerJson(doc);

			JSONObject dataLayerObject = null;
			try {
				dataLayerObject = new JSONObject(script);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (dataLayerObject == null) {
				dataLayerObject = new JSONObject();
			}

			Element variationSelector = doc.select(".options_dropdown").first();

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("p.sku span[id]").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("id").split("_")[2].trim();
			}

			// Nome
			String name = null;
			if (dataLayerObject.has("productName")) {
				name = dataLayerObject.getString("productName");
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("#widget_breadcrumb ul li");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element e : elementCategories) {
				if( e.select("a").first() != null ) {
					String tmp = e.select("a").first().text();
					if( !tmp.equals("Home") ) {
						categories.add(tmp);
					} 
				} else {
					categories.add(e.text());
				}
			}
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select(".image_container #productMainImage").first();
			if(elementPrimaryImage != null) {
				primaryImage = "http:" + elementPrimaryImage.attr("src");
			}

			// Imagens secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementsSecondaryImages = doc.select(".other_views ul li a img");
			for (Element e : elementsSecondaryImages) {
				String secondaryImage = e.attr("src");
				if( !secondaryImage.contains("PRD_447_1.jpg") ) {
					secondaryImagesArray.put("http:" + e.attr("src"));
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			// Estoque
			Integer stock = null;

			/*
			 * Produto sem variação
			 */
			if(variationSelector == null) {

				String idSelector0 = "#entitledItem_" + internalPid;
				Element elementInfo0 = doc.select(idSelector0).first();
				JSONArray jsonInfo0 = null;
				if (elementInfo0 != null) {
					try{ 
						jsonInfo0 = new JSONArray(elementInfo0.text());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// ID interno
				String internalId = null;
				if (jsonInfo0.getJSONObject(0).has("catentry_id")) {
					internalId = jsonInfo0.getJSONObject(0).getString("catentry_id").trim();
				}

				// Disponibilidade
				boolean available = true;
				Element elementUnavailable = doc.select(".price_holder .unavailableProductLabel").first();
				if(elementUnavailable != null) {
					available = false;
				}

				String partnerName = null;
				if (dataLayerObject.has("mktPlacePartner")) {
					partnerName = dataLayerObject.getString("mktPlacePartner");
				}
				if(partnerName != null && !partnerName.isEmpty()) {
					if( !partnerName.equals("Fastshop") || !partnerName.equals("fastshop") ) {
						available = false;
					}
				}

				// Preço
				Float price = null;
				if(available) {
					if (dataLayerObject.has("productSalePrice")) {
						price = Float.parseFloat( dataLayerObject.getString("productSalePrice") );
					}
				}

				// Marketplace
				JSONArray marketplace = new JSONArray();
				if(partnerName != null && !partnerName.isEmpty()) {
					JSONObject partner = new JSONObject();
					partner.put("name", partnerName);
					partner.put("price", price);
					marketplace.put(partner);
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

			else { // Produto com variação

				String idSelector = "#entitledItem_" + internalPid;
				Element elementInfo = doc.select(idSelector).first();
				JSONArray jsonInfo = null;
				if (elementInfo != null) {
					jsonInfo = new JSONArray(elementInfo.text());
				}

				for (int i = 0; i < jsonInfo.length(); i++) {
					JSONObject productInfo = jsonInfo.getJSONObject(i);

					// InternalId
					String internalId = productInfo.getString("catentry_id").trim();

					// Nome
					String variationName = name + " ";
					if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_110V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_110V").equals("1"));
						variationName = variationName + "110V";
					}
					else if ( !productInfo.getJSONObject("Attributes").isNull("Voltagem_220V") ) {
						if (productInfo.getJSONObject("Attributes").get("Voltagem_220V").equals("1"));
						variationName = variationName + "220V";
					}

					// Disponibilidade
					boolean variationAvailable = productInfo.getString("ShippingAvailability").equals("1");

					// Preço					
					Float variationPrice = null;
					if(variationAvailable) {
						variationPrice = Float.parseFloat( dataLayerObject.getString("productSalePrice") );
					}

					Product product = new Product();
					product.setSeedId(this.session.getSeedId());
					product.setUrl(this.session.getUrl());
					product.setInternalId(internalId);
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
					//product.setMarketplace(marketplace);
					product.setAvailable(variationAvailable);
					
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

	private boolean isProductPage(String url, Document doc) {
		Element elementProductInfoViewer = doc.select("#widget_product_info_viewer").first();
		return elementProductInfoViewer != null;
	}

	private String getDataLayerJson(Document doc) {
		String script = searchScript(doc);
		String dataLayer = script.substring(0, script.length()-5) + "}";

		Scanner scanner = new Scanner(dataLayer);
		String jsonDataLayerObject = "{";

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if ( line.contains("productId") || line.contains("productName") || line.contains("mktPlacePartner") || line.contains("productSalePrice") ) {
				//jsonDataLayerObject = jsonDataLayerObject + line.replaceAll("'", "\\\"").substring(0, line.length()) + "\n";
				jsonDataLayerObject = jsonDataLayerObject + line.substring(0, line.length()) + "\n";
			}

		}

		return jsonDataLayerObject;
	}

	private String searchScript(Document doc) {
		Elements scripts = doc.select("script");
		for(Element script : scripts) {
			String scriptBody = script.html().toString();
			if(scriptBody.contains("dataLayer")) {
				return scriptBody;
			}
		}
		return null;
	}

}
