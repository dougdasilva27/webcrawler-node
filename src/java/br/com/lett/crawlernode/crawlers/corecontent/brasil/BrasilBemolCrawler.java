package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;


public class BrasilBemolCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.bemol.com.br/loja/";
	private final String HOME_PAGE_HTTPS = "https://www.bemol.com.br/loja/";

	public BrasilBemolCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// InternalId
			String internalID = null;
			Element elementInternalID = doc.select("input.txtHiddenCantentryId").first();
			if (elementInternalID != null && elementInternalID.val() != null && !elementInternalID.val().isEmpty()) {
				internalID = elementInternalID.val().trim();
			}

			// Pid
			String internalPid = internalID;

			// Name
			Element elementName = doc.select(".product-title-content h1").first();
			String name = elementName.text().replace("’", "").trim();

			// Preço
			Float price = null;

			Element elementPrice = doc.select(".product-title-content .price-blue").first();

			if (elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";

			Elements elementCategories = doc.select(".breadcrumb_links").select("a");
			elementCategories.remove(0); // Tirando 'Home'

			for (Element e : elementCategories) {
				if (category1.isEmpty()) {
					category1 = e.text();
				} else if (category2.isEmpty()) {
					category2 = e.text();
				} else if (category3.isEmpty()) {
					category3 = e.text();
				}
			}

			// Imagem primária
			Elements elementImages = doc.select("#galleria").select("img");

			String primaryImage = null;

			// Imagens secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for (Element e : elementImages) {

				// Tirando o 87x87 para pegar imagem original
				if (primaryImage == null) {
					primaryImage = "http://www.bemol.com.br" + e.attr("src");
				} else {
					secondaryImagesArray.put("http://www.bemol.com.br" + e.attr("src"));
				}

			}

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".product_descs").first();
			if (elementDescription != null)
				description = elementDescription.html().trim();

			// Disponibilidade
			JSONObject jsonInfo = crawlJsonInfo(internalID);
			boolean available = true;
			
			if (jsonInfo.has("onlineInventory")) {
				JSONObject inventory = jsonInfo.getJSONObject("onlineInventory");
				
				if(inventory.has("name")){
					String text = inventory.getString("name").trim().toLowerCase();
					
					if(text.equals("available")){
						available = true;
					} else {
						available = false;
					}
				} else if(inventory.has("literal")) {
					String text = inventory.getString("literal").trim().toLowerCase();
					
					if(text.equals("available")){
						available = true;
					} else {
						available = false;
					}
				}
			} else {
				available = false;
			}

			// JSON Prices
			JSONArray jsonPrices = crawlPriceFromApi(internalID, price);
			
			// Prices
			Prices prices = crawlPrices(price, jsonPrices);
			
			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document doc) {
		return (doc.select("#product").first() != null);
	}
	
	private Prices crawlPrices(Float price, JSONArray jsonPrices) {
		Prices prices = new Prices();

		if(price != null){

			prices.setBankTicketPrice(price);

			try{
				for(int i = 0; i < jsonPrices.length(); i++) {
					JSONObject json = jsonPrices.getJSONObject(i);
					Map<Integer,Float> installmentPriceMap = new HashMap<>();

					if (json.has("paymentMethodName")) {
						String cardName = json.getString("paymentMethodName").replaceAll(" ", "").toLowerCase().trim();

						if (json.has("installmentOptions")) {
							JSONArray installments = json.getJSONArray("installmentOptions");

							for(int j = 0; j < installments.length(); j++) {
								JSONObject installmentJSON = installments.getJSONObject(j);

								if(installmentJSON.has("option")){
									String text = installmentJSON.getString("option").toLowerCase();

									Integer installment = Integer.parseInt(text.replaceAll("[^0-9]", "").trim());

									if(installmentJSON.has("amount")){				
										Float value = Float.parseFloat(installmentJSON.getString("amount").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

										installmentPriceMap.put(installment, value);
									}
								}
							}
							
							if (cardName.contains("amex") || cardName.contains("americanexpress")) {
								prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
								
							} else if (cardName.contains("visa")) {
								prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
								
							} else if (cardName.contains("mastercard") || cardName.contains("master")) {
								prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
								
							} else if (cardName.contains("diners") || cardName.contains("discover")) {
								prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
								
							} else if (cardName.contains("elo")) {
								prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
							}
						}
					}
				}
			} catch(Exception e){

			}
		}

		return prices;
	}

	/**
	 * Pega o JSON que possui informações sobre as parcelas.
	 * 
	 * @param internalId
	 * @param price
	 * @return
	 */
	private JSONArray crawlPriceFromApi(String internalId, Float price){
		String url = "https://www.bemol.com.br/webapp/wcs/stores/servlet/GetCatalogEntryInstallmentPrice?storeId=10001"
				+ "&langId=-6&catalogId=10001&catalogEntryId="+ internalId +"&nonInstallmentPrice="+ price;

		String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

		int x = json.indexOf("/*");
		int y = json.indexOf("*/", x + 2);

		json = json.substring(x+2, y);


		JSONArray jsonPrice;
		try{
			jsonPrice = new JSONArray(json);
		} catch(Exception e){
			jsonPrice = new JSONArray();
			e.printStackTrace();
		}


		return jsonPrice;
	}

	private JSONObject crawlJsonInfo(String internalId){
		JSONObject jsonInfo = new JSONObject();
		
		String url = "https://www.bemol.com.br/loja/GetCatalogEntryInventoryData?storeId=10001&langId=-6"
				+ "&catalogId=10001&itemId="+ internalId +"&displayAdditionalStores=false&requesttype=ajax";

		String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

		int x = json.indexOf("/*");
		int y = json.indexOf("*/", x + 2);

		json = json.substring(x+2, y);

		try{
			jsonInfo = new JSONObject(json);
		} catch(Exception e){
			jsonInfo = new JSONObject();
			e.printStackTrace();
		}
		
		
		return jsonInfo;
	}
}
