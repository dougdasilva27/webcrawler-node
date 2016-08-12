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
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilEfacilCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.efacil.com.br/";

	public BrasilEfacilCrawler(CrawlerSession session) {
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

			Element variationSelector = doc.select(".options_attributes").first();

			// Nome
			Element elementName = doc.select("h1.product-name").first();
			String name = null;
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Categorias
			Elements elementsCategories = doc.select("#widget_breadcrumb ul li a");
			String category1 = "";
			String category2 = "";
			String category3 = "";
			for (int i = 1; i < elementsCategories.size(); i++) {
				if (category1.isEmpty()) {
					category1 = elementsCategories.get(i).text().trim();
				} else if (category2.isEmpty()) {
					category2 = elementsCategories.get(i).text().trim();
				} else if (category3.isEmpty()) {
					category3 = elementsCategories.get(i).text().trim();
				}
			}

			// Imagem primária
			Element elementPrimaryImage = doc.select("div.product-photo a").first();
			String primaryImage = null;
			if (elementPrimaryImage != null) {
				primaryImage = "http:" + elementPrimaryImage.attr("href").trim();
			}

			// Imagem secundária
			Elements elementImages = doc.select("div.thumbnails a");
			JSONArray secondaryImagesArray = new JSONArray();
			String secondaryImages = null;
			if (elementImages.size() > 1) {
				for (int i = 1; i < elementImages.size(); i++) { // primeira imagem eh primaria
					secondaryImagesArray.put("http:" + elementImages.get(i).attr("data-original").trim());
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementSpecs = doc.select("#especificacoes").first();
			Element elementTabContainer = doc.select("#tabContainer").first();
			if (elementTabContainer != null) description += elementTabContainer.html();
			if (elementSpecs != null) description = description + elementSpecs.html();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			if (variationSelector == null) { // sem variações

				// ID interno
				String internalId = null;
				Element tmpIdElement = doc.select("input[name=productId]").first();
				String tmpId = null;
				if (tmpIdElement != null) {
					tmpId = tmpIdElement.attr("value").trim();
				}
				JSONArray infoArray = new JSONArray(doc.select("#entitledItem_" + tmpId).text().trim());
				JSONObject info = infoArray.getJSONObject(0);
				internalId = info.getString("catentry_id");

				// InternalPid
				String internalPid = null;
				Element elementInternalPid = doc.select("input[name=productId]").first();
				if (elementInternalPid != null) {
					internalPid = elementInternalPid.attr("value").trim();
				}

				// Disponibilidade
				boolean available = true;
				Element elementAvailable = doc.select("#disponibilidade-estoque").first();
				if (elementAvailable == null) {
					available = false;
				}

				// Preço
				Float price = null;
				if (available) {
					Element elementPrice = doc.select("span[itemprop=price]").first();
					if (elementPrice != null) {
						price = Float.parseFloat(elementPrice.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
					}
				}

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

			else { // múltiplas variações

				Element tmpIdElement = doc.select("input[id=productId]").first();
				String tmpId = null;
				if (tmpIdElement != null) {
					tmpId = tmpIdElement.attr("value").trim();
				}

				try {

					JSONArray variationsJsonInfo = new JSONArray(doc.select("#entitledItem_" + tmpId).text().trim());

					for(int i = 0; i < variationsJsonInfo.length(); i++) {

						JSONObject variationJsonObject = variationsJsonInfo.getJSONObject(i);

						// ID interno
						String internalId = variationJsonObject.getString("catentry_id").trim();

						// InternalPid
						String internalPid = null;
						Element elementInternalPid = doc.select("input#productId").first();
						if (elementInternalPid != null) {
							internalPid = elementInternalPid.attr("value").trim();
						}

						// Nome
						JSONObject attributes = variationJsonObject.getJSONObject("Attributes");
						String variationName = null;
						if (attributes.has("Voltagem_110V")) {
							variationName = name + " 110V";
						}
						else if (attributes.has("Voltagem_220V")) {
							variationName = name + " 220V";
						}

						// Disponibilidade
						boolean available = true;
						if (variationJsonObject.getString("hasInventory").equals("false")) {
							available = false;
						}

						// Preço
						Float price = null;
						if (available) {
							price = Float.parseFloat(variationJsonObject.getString("offerPrice").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
						}

						Product product = new Product();
						product.setSeedId(this.session.getSeedId());
						product.setUrl(this.session.getUrl());
						product.setInternalId(internalId);
						product.setInternalPid(internalPid);
						product.setName(variationName);
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
					
				} catch (Exception e) {
					e.printStackTrace();
					Logging.printLogError(logger, session, "Error processing product with variations!");
				}
			} // fim do caso de múltiplas variacoes

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.efacil.com.br/loja/produto/");
	}

}