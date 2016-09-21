package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class BrasilEfacilCrawler extends Crawler {

	public BrasilEfacilCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.efacil.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if (session.getUrl().startsWith("http://www.efacil.com.br/loja/produto/")) {
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

			// Filtragem
			boolean mustInsert = true;

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			if (variationSelector == null) { // sem variações

				// InternalPid
				String internalPid = null;
				Element elementInternalPid = doc.select("input#productId").first();
				if (elementInternalPid != null) {
					internalPid = elementInternalPid.attr("value").trim();
				}
				
				// ID interno
				String internalId = null;
				Element internalIdElement = doc.select("#entitledItem_" + internalPid).first();
				
				if(internalIdElement != null){
					JSONArray infoArray = new JSONArray(internalIdElement.text().trim());
					JSONObject info = infoArray.getJSONObject(0);
					internalId = info.getString("catentry_id");
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
					Elements elementPrice = doc.select(".container-price-installment span.blue");
					
					for(Element e : elementPrice){
						String temp = e.text().trim();
						
						if(temp.startsWith("R$")){
							price = Float.parseFloat(temp.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
							break; 
						}
						
					}
				}

				if (mustInsert) {

					Product product = new Product();
					product.setUrl(session.getUrl());
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
							price = crawlPriceFromApi(internalId, internalPid);
						}

						if (mustInsert) {

							Product product = new Product();
							product.setUrl(session.getUrl());
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

					}
				} catch (Exception e) {
					Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
				}
			} // fim do caso de múltiplas variacoes

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}

	
	private Float crawlPriceFromApi(String internalId, String internalPid){
		Float price = null;
		String url = "http://www.efacil.com.br/webapp/wcs/stores/servlet/GetCatalogEntryDetailsByIDView?storeId=10154"
				+ "&langId=-6&catalogId=10051&catalogEntryId=" + internalId + "&productId=" + internalPid;
		
		String json = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, url, null, null);
		
		int x = json.indexOf("/*");
		int y = json.indexOf("*/", x + 2);
		
		json = json.substring(x+2, y);
		
		
		JSONObject jsonPrice;
		try{
			jsonPrice = new JSONObject(json);
		} catch(Exception e){
			jsonPrice = new JSONObject();
			e.printStackTrace();
		}
		
		if(jsonPrice.has("catalogEntry")){
			JSONObject jsonCatalog = jsonPrice.getJSONObject("catalogEntry");
			
			if(jsonCatalog.has("formattedTotalAVista")){
				price = Float.parseFloat(jsonCatalog.getString("formattedTotalAVista").trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		}
		
		return price;
	}
}