package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
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

public class SaopauloWalmartCrawler extends Crawler {

	public SaopauloWalmartCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith("http://www.walmart.com.br/") || href.startsWith("https://www.walmart.com.br/"));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if(session.getOriginalURL().startsWith("http://www.walmart.com.br/produto/") || session.getOriginalURL().startsWith("https://www.walmart.com.br/produto/") || (session.getOriginalURL().endsWith("/pr"))) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Nome
			Elements elementName = doc.select("h1.product-name");
			String name = null;
			if(elementName != null) {
				name = elementName.text().replace("'","").replace("’","").trim();
			}

			// Pid
			String[] tokens = session.getOriginalURL().split("/");
			String internalPid = tokens[tokens.length-2].trim();

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb li"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";

			int j=0;
			for(int i=0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = cat[3];

			// Primary image
			String primaryImage = crawlPrimaryImage(doc);

			// Secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".product-description").first(); 
			Element elementCharacteristics = doc.select("#product-characteristics-container").first(); 
			Element elementDimensions = doc.select(".product-dimensions").first(); 
			if(elementDescription != null) 	description = description + elementDescription.html();
			if(elementCharacteristics != null) description = description + elementCharacteristics.html();
			if(elementDimensions != null) 		description = description + elementDimensions.html();

			// Pegar produtos dentro da url
			JSONObject dataLayer;
			JSONArray productsListInfo = new JSONArray();

			Elements scriptTags = doc.getElementsByTag("script");
			for (Element tag : scriptTags){                
				for (DataNode node : tag.dataNodes()) {
					if(tag.html().trim().startsWith("var dataLayer = ") && tag.html().trim().contains("dataLayer.push(")) {

						dataLayer = new JSONObject(
								node.getWholeData().split(Pattern.quote("dataLayer.push("))[1] +
								node.getWholeData().split(Pattern.quote("dataLayer.push("))[1].split(Pattern.quote(");"))[0]
								);

						productsListInfo = dataLayer.getJSONArray("trees").getJSONObject(0).getJSONObject("skuTree").getJSONArray("options");

						if(productsListInfo.length() == 0) {
							productsListInfo.put(new JSONObject("{\"name\":\"\",\"skuId\":" + dataLayer.getJSONArray("trees").getJSONObject(0).get("standardSku") + "}"));
						}

					}
				}        
			}

			for(int p = 0; p < productsListInfo.length(); p++) {

				JSONObject jsonProducts = productsListInfo.getJSONObject(p);

				String productId = null;
				if(jsonProducts.has("skuId")){
					productId = jsonProducts.get("skuId").toString();
				}

				String productCustomName = null;
				if(jsonProducts.has("name")){
					productCustomName = jsonProducts.getString("name");
				}

				// Price
				Float price = null;

				// Availability
				boolean available = false;

				// Document Marketplace
				// Fazendo request da página com informações de lojistas
				Document infoDoc = fetchMarketplaceInfoDocMainPage(productId);

				// availability, price and marketplace
				Map<String, Prices> marketplaceMap = extractMarketplace(productId, internalPid, infoDoc);
				JSONArray marketplace = new JSONArray();
				for (String partnerName : marketplaceMap.keySet()) {
					if (partnerName.equals("walmart")) { // se o walmart está no mapa dos lojistas, então o produto está disponível
						available = true;
						JSONObject prices = marketplaceMap.get(partnerName).getRawCardPaymentOptions("visa");

						if(prices.has("1")) {
							Double priceDouble = marketplaceMap.get(partnerName).getRawCardPaymentOptions("visa").getDouble("1");
							price = MathCommonsMethods.normalizeTwoDecimalPlaces(priceDouble.floatValue());
						}
					} else { // se não for o walmart, insere no json array de lojistas
						JSONObject partner = new JSONObject();
						partner.put("name", partnerName);
						partner.put("price", marketplaceMap.get(partnerName).getBankTicketPrice());

						partner.put("prices", marketplaceMap.get(partnerName).getPricesJson());

						marketplace.put(partner);
					}
				}

				// Estoque
				Integer stock = crawlStock(infoDoc);

				//Prices
				Prices prices = crawlPrices(internalPid, price, marketplaceMap);

				Product product = new Product();

				product.setUrl(session.getOriginalURL());
				product.setInternalId(productId);
				product.setInternalPid(internalPid);
				product.setName(name + " " + productCustomName);
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;

		Elements imagesElements = doc.select("#wm-pictures-carousel a");

		if (!imagesElements.isEmpty()) {
			Element e = imagesElements.first();
			String imgUrl = null;

			if (e.attr("data-zoom") != null && !e.attr("data-zoom").isEmpty()) {
				imgUrl = e.attr("data-zoom");
			} else if(e.attr("data-normal") != null && !e.attr("data-normal").isEmpty()) {
				imgUrl = e.attr("data-normal");
			} else if(e.attr("src") != null && !e.attr("src").isEmpty()) {
				imgUrl = e.attr("src");
			}

			if (imgUrl != null && !imgUrl.startsWith("http")) {
				imgUrl = "http:" + imgUrl;
			}

			primaryImage = imgUrl;
		} else { // this case occurs when the product is discontinued, but it's info are still displayed
			Element mainImageElement = doc.select(".buybox-column.aside.discontinued .main-picture").first();
			if (mainImageElement != null) {
				primaryImage = "https:" + mainImageElement.attr("src");
			}
		}

		return primaryImage;
	}
	
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElements = doc.select("#wm-pictures-carousel a");

		if (!imagesElements.isEmpty()) {
			for (int i = 1; i < imagesElements.size(); i++) {
				Element e = imagesElements.get(i);

				String imgUrl = null;

				if (e.attr("data-zoom") != null && !e.attr("data-zoom").isEmpty()) {
					imgUrl = e.attr("data-zoom");
				} else if (e.attr("data-normal") != null && !e.attr("data-normal").isEmpty()) {
					imgUrl = e.attr("data-normal");
				} else if (e.attr("src") != null && !e.attr("src").isEmpty()) {
					imgUrl = e.attr("src");
				}

				if (imgUrl != null && !imgUrl.startsWith("http")) {
					imgUrl = "http:" + imgUrl;
				}

				secondaryImagesArray.put(imgUrl);
			}
		}

		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}

	private Map<String, Prices> extractMarketplace(String productId, String internalPid, Document infoDoc) {
		Map<String, Prices> marketplace = new HashMap<>();

		Elements sellers = infoDoc.select(".product-sellers-list-item");

		for(Element e : sellers){
			Element nameElement = e.select(".seller-name .name").first();

			if(nameElement != null){
				String name = nameElement.ownText().trim().toLowerCase();
				Float price = null;

				Integer installment = null;
				Float value = null;

				Prices prices = new Prices();
				Map<Integer,Float> installmentPriceMap = new HashMap<>();

				Element priceElement = e.select(".product-price .product-price-value").first();

				if(priceElement != null){
					price = MathCommonsMethods.parseFloat(priceElement.text().trim());
					installmentPriceMap.put(1, price);

//					Element bankTicket = e.select(".sticker-promotional .sticker-image img").first();
//
//					if(bankTicket != null) {
//						String[] tokens = bankTicket.attr("src").split("-");
//						String discountString = tokens[tokens.length-1].replaceAll("[^0-9]", "").trim();
//
//						if(!discountString.isEmpty()) {
//							Integer discount = Integer.parseInt(discountString);
//							Float bankTicketPrice = (float) (price - (price * (discount.floatValue()/100.0)));
//							prices.insertBankTicket(MathCommonsMethods.normalizeTwoDecimalPlaces(bankTicketPrice));
//						} else {
//							prices.insertBankTicket(price);
//						}
//					} else {
						prices.insertBankTicket(price);
//					}
				}

				Element installmentElement = e.select(".product-price-installment").first();
				String priceInstallmentAmount = installmentElement.attr("data-price-installment-amount");

				if(installmentElement != null && !priceInstallmentAmount.isEmpty()){
					installment = Integer.parseInt(priceInstallmentAmount);

					Element valueElement = installmentElement.select(".product-price-price").first();

					if(valueElement != null) {
						value = MathCommonsMethods.parseFloat(valueElement.text());

						installmentPriceMap.put(installment, value);
					}
				}
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

				marketplace.put(name, prices);
			}
		}

		Element moreSellers = infoDoc.select(".more-sellers-link").first();

		if(moreSellers != null) {
			Document docMarketPlaceMoreSellers = fetchMarketplaceInfoDoc(productId, internalPid);

			Elements marketplaces = docMarketPlaceMoreSellers.select(".sellers-list tr:not([class])");

			for (Element e : marketplaces) {	

				//Name
				Element nameElement = e.select("td span[data-seller]").first();

				if(nameElement != null){
					String name = nameElement.text().trim().toLowerCase();


					Integer installment = null;
					Float value = null;

					Prices prices = new Prices();
					Map<Integer,Float> installmentPriceMap = new HashMap<>();

					Element priceElement = e.select(".payment-price").first();
					Float price = null;

					if(priceElement != null){
						price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
						installmentPriceMap.put(1, price);
						prices.insertBankTicket(price);
					}

					Element installmentElement = e.select(".payment-installment-amount").first();

					if(installmentElement != null){
						installment = Integer.parseInt(installmentElement.ownText().trim().replaceAll("[^0-9]", ""));

						Element installmentValueElement = e.select(".payment-installment-price").first();

						if(installmentValueElement != null){
							value = Float.parseFloat(installmentValueElement.ownText().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

							installmentPriceMap.put(installment, value);
							prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
						}
					}

					marketplace.put(name, prices);
				}

			}
		}

		return marketplace;
	}

	private Document fetchMarketplaceInfoDoc(String productId, String pid) {
		String infoUrl = "https://www.walmart.com.br/xhr/sellers/sku/"+ productId +"?productId="+ pid;					
		String fetchResult = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, infoUrl, null, null);

		return Jsoup.parse(fetchResult);
	}

	private Document fetchMarketplaceInfoDocMainPage(String productId) {
		String infoUrl = "https://www.walmart.com.br/xhr/sku/buybox/"+ productId +"/?isProductPage=true";					
		String fetchResult = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, infoUrl, null, null);

		return Jsoup.parse(fetchResult);
	}


	private Prices crawlPrices(String internalPid, Float price, Map<String,Prices> marketplaces){
		Prices p = new Prices();
		Map<Integer, Float> installmentPriceMap = new HashMap<>();

		if(marketplaces.containsKey("walmart")) {
			Prices prices = marketplaces.get("walmart");
			p.insertBankTicket(prices.getBankTicketPrice());		

			if(price != null){
				//preço principal é o mesmo preço de 1x no cartão
				installmentPriceMap.put(1, price);

				String urlInstallmentPrice = "https://www.walmart.com.br/produto/installment/1,"+ internalPid +","+ price +",VISA/"; 
				Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlInstallmentPrice, null, cookies);
				Elements installments = doc.select(".installment-table tr:not([id])");

				for(Element e : installments){
					Element parc = e.select("td.parcelas").first();

					if(parc != null){
						Integer installment = Integer.parseInt(parc.text().replaceAll("[^0-9]", "").trim());

						Element parcValue = e.select(".valor-parcela").first();

						if(parcValue != null){
							Float installmentValue = Float.parseFloat(parcValue.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

							installmentPriceMap.put(installment, installmentValue);
						}
					}
				}

				p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			}
		}

		return p;
	}

	private Integer crawlStock(Document infoDoc){
		Integer stock = null;
		Element stockWalmart = infoDoc.select("#buybox-Walmart").first();

		if(stockWalmart != null){
			if(stockWalmart.hasAttr("data-quantity")){
				stock = Integer.parseInt(stockWalmart.attr("data-quantity"));
			}
		}

		return stock;
	}
}
