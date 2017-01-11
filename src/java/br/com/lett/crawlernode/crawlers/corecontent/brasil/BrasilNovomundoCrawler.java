package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
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
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilNovomundoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.novomundo.com.br/";

	public BrasilNovomundoCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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
					
					String primaryImage = null;
					String secondaryImages = null;


					// Pegar dados da API
					JSONObject productInfoJSON = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, ("http://www.novomundo.com.br/produto/sku/" + id.trim()), null, null).getJSONObject(0);

					// internal id
					internalId = String.valueOf(productInfoJSON.getInt("Id"));

					// internal pid
					internalPid = crawlInternalPid(doc);

					// name
					String name = crawlName(productInfoJSON);

					// stock
					Integer stock = crawlStock(productInfoJSON);
					
					// availability
					boolean available = crawlAvailability(productInfoJSON);

					int discountBoleto = crawlDiscountBoleto(doc);
					
					// marketplace
					JSONArray marketplace = crawlMarketplace(productInfoJSON, internalId, discountBoleto);

					// price
					Float price = crawlPrice(productInfoJSON);

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

					// Prices 
					Prices prices = crawlPrices(internalId, price, new JSONArray(), discountBoleto);
					
					Product product = new Product();

					product.setUrl(this.session.getOriginalURL());
					product.setInternalId(internalId);
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

				}			

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
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
	
	private String crawlName(JSONObject productInfoJSON) {
		String name = null;
		if (productInfoJSON.has("Name")) {
			name = productInfoJSON.getString("Name");
		}
		return name;
	}
	
	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element internalPidElement = document.select("#___rc-p-id").first();
		if (internalPidElement != null) {
			internalPid = internalPidElement.attr("value");
		}
		return internalPid;
	}

	private boolean crawlAvailability(JSONObject productInfoJSON) {

		// availability info from the field 'Availability'
		Boolean availabilityFromJson = false;
		if (productInfoJSON.has("Availability")) {
			availabilityFromJson = productInfoJSON.getBoolean("Availability");
		}

		// looking if the main seller is the default seller
		JSONArray skuSellersInfo = productInfoJSON.getJSONArray("SkuSellersInformation");
		Boolean isDefaultSeller = false;
		for (int i = 0; i < skuSellersInfo.length(); i++) {
			JSONObject seller = skuSellersInfo.getJSONObject(i);
			String sellerId = seller.getString("SellerId").toLowerCase().trim();
			if (sellerId.equals("1")) {
				isDefaultSeller = seller.getBoolean("IsDefaultSeller"); 
			}
		}

		// cross the two informations
		if (availabilityFromJson && isDefaultSeller) return true;
		return false;
	}

	private JSONArray crawlMarketplace(JSONObject productInfoJSON, String internalId, int discountBoleto) {
		JSONArray marketplace = new JSONArray();
		JSONArray skuSellersInfo = productInfoJSON.getJSONArray("SkuSellersInformation");
		
		for (int i = 0; i < skuSellersInfo.length(); i++) {
			JSONObject seller = skuSellersInfo.getJSONObject(i);
		
			String sellerName = seller.getString("Name").toLowerCase().trim();
			Float sellerPrice = (float)seller.getDouble("Price");
			String sellerId = seller.getString("SellerId");
			
			if (!sellerId.equals("1")) {
				JSONObject partner = new JSONObject();
				partner.put("name", sellerName);
				partner.put("price", sellerPrice);
				
				if(seller.has("IsDefaultSeller")){
					if(seller.getBoolean("IsDefaultSeller")){
						partner.put("prices", crawlPrices(internalId, sellerPrice, marketplace, discountBoleto).getPricesJson());
					}
				}
				
				marketplace.put(partner);
			}

		}

		return marketplace;
	}

	private Float crawlPrice(JSONObject productInfoJSON) {
		Float price = null;
		JSONArray skuSellersInfo = productInfoJSON.getJSONArray("SkuSellersInformation");
		for (int i = 0; i < skuSellersInfo.length(); i++) {
			JSONObject seller = skuSellersInfo.getJSONObject(i);
			String sellerId = seller.getString("SellerId").toLowerCase().trim();
			if (sellerId.equals("1")) {
				if (seller.has("Price")) {
					price = (float)seller.getDouble("Price");
				}
			}
		}

		// when the product is unavailable for the seller, it's price field holds the value 0.0
		if (price != null && Float.compare(price, 0.0f) == 0) price = null; 

		return price;
	}

	private Integer crawlStock(JSONObject productInfoJSON) {
		JSONArray skuSellersInfo = productInfoJSON.getJSONArray("SkuSellersInformation");

		for (int i = 0; i < skuSellersInfo.length(); i++) {
			JSONObject seller = skuSellersInfo.getJSONObject(i);
			if (seller.has("SellerId")) {
				if (seller.getString("SellerId").equals("1")) {
					if (seller.has("AvailableQuantity")) return seller.getInt("AvailableQuantity");
				}
			}
		}

		return null;
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
	
	private Prices crawlPrices(String internalId, Float price, JSONArray marketplace, Integer discountBoleto){
		Prices prices = new Prices();
				
		if(price != null || marketplace.length() > 0){
			String url = "http://campanhas.novomundo.com.br/vtex/productotherpaymentsystems.php?sku=" + internalId + "&d=" + discountBoleto;
			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
			
			Element bankTicketElement = doc.select("#divBoleto .valor").first();
			if(bankTicketElement != null){
				Float bankTicketPrice = Float.parseFloat(bankTicketElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
				
				Float result = (float) (bankTicketPrice - (bankTicketPrice * (discountBoleto.floatValue()/100.0)));
				bankTicketPrice = MathCommonsMethods.normalizeTwoDecimalPlaces(result);
				
				prices.insertBankTicket(bankTicketPrice);
			}
			
			Elements installmentsElements = doc.select("#divCredito table tbody");
			
			for(Element e : installmentsElements){
				Map<Integer,Float> installmentPriceMap = new HashMap<>();
				String cardName = null;
				
				Elements installmentsCard = e.select("tr");
				for(Element i : installmentsCard){
					Element installmentElement = i.select("td.parcelas").first();
					
					if(installmentElement != null){
						String textInstallment = removeAccents(installmentElement.text().toLowerCase());
						Integer installment = null;
						
						if(textInstallment.contains("vista")){
							installment = 1;
							
							if(cardName == null){
								int x = textInstallment.indexOf(" a ");
								cardName = textInstallment.substring(0, x).replaceAll(" ", "");
							}
							
						} else {
							installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
							
							if(cardName == null){
								int x = textInstallment.indexOf((installment.toString()));
								cardName = textInstallment.substring(0, x).replaceAll(" ", "");
							}
						}
						
						Element valueElement = i.select("td:not(.parcelas)").first();
						
						if(valueElement != null){
							Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
							
							if(installment.equals(1)){
								Float result = (float) (value - (value * (discountBoleto.floatValue()/100.0)));
								value = MathCommonsMethods.normalizeTwoDecimalPlaces(result);
							}
							
							installmentPriceMap.put(installment, value);
						}
					}
				}
				
				if(installmentPriceMap.size() > 0 && cardName != null){
					if (cardName.equals("amex")) {
						prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
						
					} else if (cardName.equals("visa")) {
						prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
						
					} else if (cardName.equals("mastercard")) {
						prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
						
					} else if (cardName.equals("diners")) {
						prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
						
					} else if (cardName.equals("americanexpress")) {
						prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
						
					} else if (cardName.equals("elo")) {
						prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
					}
				}
			}
			
		}
		
		return prices;
	}

	private int crawlDiscountBoleto(Document doc){
		int x = 0;
		Elements discount = doc.select(".product-discount-hight-light > p");
		
		if(discount.size() > 0){
			for(Element e : discount){
				String boleto = e.text();
				
				if(boleto.contains("boleto")){
					x = Integer.parseInt(boleto.replaceAll("[^0-9]", ""));
				}
			}
		}
		
		return x;
	}
	
	private String removeAccents(String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = str.replaceAll("[^\\p{ASCII}]", "");
		return str;
	}
}
