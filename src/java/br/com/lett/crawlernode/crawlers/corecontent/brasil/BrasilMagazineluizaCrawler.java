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
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilMagazineluizaCrawlerUtils;
import br.com.lett.crawlernode.util.Logging;


/**
 * eg:
 * 
 * SKU with marketplace: http://www.magazineluiza.com.br/whisky-vintage-eau-de-toilette-evaflor-100ml-perfume-masculino/p/9843013/pf/pfpm/
 * http://www.magazineluiza.com.br/micro-ondas-midea-liva-mtag4-30l-com-funcao-grill/p/2019941/ed/mond/
 * http://www.magazineluiza.com.br/ar-condicionado-split-midea-inverter-9000-btus-frio-vita-42mkca09m5-autolimpante/p/2121992/ar/arsp/
 * http://www.magazineluiza.com.br/smart-tv-led-48-samsung-full-hd-un48j5200-conversor-digital-wi-fi-2-hdmi-1-usb/p/1933790/et/elit/
 * http://www.magazineluiza.com.br/cafeteira-nespresso-19-bar-inissia-tropical/p/2165203/ep/cane/
 * http://www.magazineluiza.com.br/smart-tv-gamer-led-55-samsung-un55j5500-full-hd-conversor-integrado-3-hdmi-2-usb-wi-fi/p/1933674/et/elit/
 * http://www.magazineluiza.com.br/rack-para-tv-ate-42-1-porta-de-abrir-dj-moveis-america/p/1217778/21/mo/racm/
 * 
 * obs: we couldn't find any URL example with more than one different seller on marketplace.
 * 
 * 
 * 
 * @author Samir Leao
 *
 */
public class BrasilMagazineluizaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.magazineluiza.com.br/";

	public BrasilMagazineluizaCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			JSONObject skuJsonInfo = BrasilMagazineluizaCrawlerUtils.crawlFullSKUInfo(doc);

			/*
			 * Id interno -- obtido a partir do id do sku apendado com o full id. O full id será
			 * apendado no início do tratamento de cada caso de produto (produto com variação 
			 * e sem variação) 
			 */
			String internalId = null;
			Element elementInternalId = doc.select("small[itemprop=productID]").first();
			if(elementInternalId != null){
				int begin = elementInternalId.text().indexOf(".com") + 4;
				internalId = elementInternalId.text().substring(begin).replace(")", "").trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select("h1[itemprop=name]").first();
			if (elementName != null) {
				name = elementName.text();
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementsCategories = doc.select(".container-bread-crumb-detail.bread-none-line ul li[typeof=v:Breadcrumb] a");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element categorie : elementsCategories) {
				String cat = categorie.text();
				if (!cat.equals("magazineluiza.com")) {
					categories.add(cat);
				}
			}
			for (String category : categories) {
				if (category1.isEmpty()) {
					category1 = category;
				} else if (category2.isEmpty()) {
					category2 = category;
				} else if (category3.isEmpty()) {
					category3 = category;
				}
			}

			// Imagens
			String primaryImage = null;
			String secondaryImages = null;
			Elements elementsImages = doc.select(".container-little-picture ul li a");
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementsImages) {
				if( !e.attr("rel").isEmpty() ) {
					String image = parseImage(e.attr("rel"));
					if (primaryImage == null) {
						primaryImage = image;
					} else {
						secondaryImagesArray.put(image);
					}
				}
			}
			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}
			
			if(primaryImage == null) {
				Element primaryImageElement = doc.select(".img-product-out-of-stock img").first();
				
				if(primaryImageElement != null) {
					primaryImage = primaryImageElement.attr("src");
				}
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = new JSONArray();

			JSONArray skus = new JSONArray();
			if(skuJsonInfo.has("details")){
				skus = skuJsonInfo.getJSONArray("details");
			}
			
			/* ******************
			 * Only one product *
			 * ******************/

			if (skus.length() == 1 && !BrasilMagazineluizaCrawlerUtils.hasVoltageSelector(skus)) {
				
				// append extra in the name
				JSONObject sku = skus.getJSONObject(0);
				if (sku.has("voltage")) {
					name = name + " - " + sku.getString("voltage");
				}
				else if (sku.has("color")) {
					name = name + " - " + sku.getString("color");
				}
				else if (sku.has("size")) {
					name = name + " - " + sku.getString("size").replace("&#34;", "");
				}

				// availability
				boolean available = true;
				Element elementAvailable = doc.select(".container-btn-buy").first();
				if(elementAvailable == null) {
					available = false;
				} else {
					Elements elementsMarketPlace = doc.select(".market-place-delivery .market-place-delivery__seller--big");
					boolean magazineIsSelling = false;
					for(Element e : elementsMarketPlace) {
						if(e.text().equals("Magazine Luiza")) {
							magazineIsSelling = true;
							break;
						}
					}
					if(magazineIsSelling) available = true;
					else available = false;
				}

				Float price = null;
				if (available) {
					Double priceDouble = skuJsonInfo.getDouble("salePrice");
					price = priceDouble.floatValue();
				}

				Element idForPriceElement = doc.select(".buy-option").first();
				String idForPrice = null;
				if(idForPriceElement != null){
					idForPrice = idForPriceElement.attr("value");
				}
				
				Prices prices = crawlPrices(idForPrice, price);
				
				// marketplace
				Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
				if (marketplaceName != null) {
					String sellerName = marketplaceName.text().toLowerCase().trim();
					if (!sellerName.equals("magazine luiza")) {
						Double priceDouble = skuJsonInfo.getDouble("salePrice");
						Float sellerPrice = priceDouble.floatValue();

						JSONObject seller = new JSONObject();
						seller.put("name", sellerName);
						seller.put("price", sellerPrice);
						seller.put("prices", crawlPrices(idForPrice, sellerPrice).toJSON());

						available = false;
						price = null;
						prices = new Prices();
						
						marketplace.put(seller);
					}
				}
				
				// Descrição
				String description = "";
				Element elementDescription = doc.select(".factsheet-main-container").first();
				if (elementDescription != null) {
					description = description + elementDescription.html();
				}

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

			/* ***********************
			 * More than one product *
			 *************************/
			else {

				// se chegamos até aqui, significa que temos mais de um produto na URL
				// esse caso deve ser sub-dividido em outros casos.
				// Primeiro: seletor de voltagem - nesse caso os outros skus na página não possuem URLs diferentes
				// 			 e o preço dos dois produtos é o mesmo.
				// Segundo: Os outros produtos da página possuem URLs diferentes e nesse caso o crawler deve capturar
				//			apenas o produto 'default' daquela página. Os outros serão eventualmente descobertos pelo descobridor.


				// analyze if we have variations of the same sku or if they are different skus
				if (BrasilMagazineluizaCrawlerUtils.hasVoltageSelector(skus)) { // seletor de voltagem o preço é o mesmo e não há url diferente para os skus e capturamos normalmente
					for(int i = 0; i < skus.length(); i++) {

						JSONObject sku = skus.getJSONObject(i);

						// internalId
						String internalIdsecondPart = sku.getString("sku");
						String variationInternalId = internalId + "-" + internalIdsecondPart;

						// internalPid
						String variationInternalPid = internalId;

						// name
						String variationName = name;
						if (sku.has("voltage")) {
							variationName = variationName + " - " + sku.getString("voltage");
						}
						else if (sku.has("size")) {
							variationName = variationName + " - " + sku.getString("size").replace("&#34;", "");
						}
						else if (sku.has("color")) {
							variationName = variationName + " - " + sku.getString("color");
						}

						// availability
						boolean available = false;
						if (BrasilMagazineluizaCrawlerUtils.hasOptionSelector(internalIdsecondPart, doc)) {
							available = true;
						}

						Float price = null;
						if (available) {
							Double priceDouble = skuJsonInfo.getDouble("salePrice");
							price = priceDouble.floatValue();
						}
						
						Prices prices = crawlPrices(internalIdsecondPart, price);
						
						// marketplace
						Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
						if (marketplaceName != null) {
							String sellerName = marketplaceName.text().toLowerCase().trim();
							if (!sellerName.equals("magazine luiza")) {
								Float sellerPrice = crawlPriceVariation(doc);

								JSONObject seller = new JSONObject();
								seller.put("name", sellerName);
								seller.put("price", sellerPrice);
								seller.put("prices", crawlPrices(internalIdsecondPart, sellerPrice).toJSON());

								available = false;
								price = null;
								prices = new Prices();
								
								marketplace.put(seller);
							}
						}
						
						// Descrição -- tem uma para cada variacao -- usando uma API do magazineluiza para pegar a descricao correspondente
						String descriptionURL = "http://www.magazineluiza.com.br/produto/ficha-tecnica/" + internalIdsecondPart + "/";
						String description = DataFetcher.fetchString("GET", session, descriptionURL, null, null);
						
						Product product = new Product();
						product.setUrl(this.session.getOriginalURL());
						product.setInternalId(variationInternalId);
						product.setInternalPid(variationInternalPid);
						product.setName(variationName);
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
				
				// cai no caso de produto sem variação
				else if (BrasilMagazineluizaCrawlerUtils.skusWithURL(doc)) { // if there are others skus with different URLs per each one
					String selectedValue = BrasilMagazineluizaCrawlerUtils.selectCurrentSKUValue(doc);
					JSONObject detail = BrasilMagazineluizaCrawlerUtils.getSKUDetails(selectedValue, skuJsonInfo);
					
					// internalId
					//String internalIdsecondPart = detail.getString("sku");
					//String variationInternalId = internalId + "-" + internalIdsecondPart;

					// internalPid
					String variationInternalPid = internalId;

					// name
					String variationName = name;
					if (detail.has("voltage")) {
						variationName = variationName + " - " + detail.getString("voltage");
					}
					else if (detail.has("size")) {
						variationName = variationName + " - " + detail.getString("size").replace("&#34;", "");
					}
					else if (detail.has("color")) {
						variationName = variationName + " - " + detail.getString("color");
					}

					// availability
					boolean available = skuJsonInfo.getBoolean("stockAvailability");

					// price
					Float price = null;
					if (available) {
						Double priceDouble = skuJsonInfo.getDouble("salePrice");
						price = priceDouble.floatValue();
					}

					Element idForPriceElement = doc.select(".buy-option[checked]").first();
					String idForPrice = null;
					if(idForPriceElement != null){
						idForPrice = idForPriceElement.attr("value");
					}
					
					Prices prices = crawlPrices(idForPrice, price);
					
					// marketplace
					Element marketplaceName = doc.select(".market-place-delivery .market-place-delivery__seller--big").first();
					if (marketplaceName != null) {
						String sellerName = marketplaceName.text().toLowerCase().trim();
						if (!sellerName.equals("magazine luiza")) {
							Float sellerPrice = crawlPriceVariation(doc);

							JSONObject seller = new JSONObject();
							seller.put("name", sellerName);
							seller.put("price", sellerPrice);
							seller.put("prices", crawlPrices(idForPrice, sellerPrice).toJSON());

							available = false;
							price = null;
							prices = new Prices();
							
							marketplace.put(seller);
						}
					}
					
					// Descrição
					String description = "";
					Element elementDescription = doc.select(".factsheet-main-container").first();
					if (elementDescription != null) {
						description = description + elementDescription.html();
					}

					Product product = new Product();
					product.setUrl(this.session.getOriginalURL());
					product.setInternalId(internalId);
					product.setInternalPid(variationInternalPid);
					product.setName(variationName);
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

	private String parseImage(String text) {
		int begin = text.indexOf("largeimage:") + 11;
		String img = text.substring(begin);
		img = img.replace("\'", " ").replace('}', ' ').trim();

		return img;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("/p/"));
	}

	private Float crawlPriceVariation(Document document) {
		Float price = null;
		Element elementPrice = document.select("#productDiscountPrice").first();
		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.attr("value"));
		}

		return price;
	}

	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			String urlPrices = "http://www.magazineluiza.com.br/produto/"+ internalId +"/preco.json";
			
			Map<Integer, Float> installmentsPriceMap = new HashMap<>();
			
			JSONObject jsonPrices = new JSONObject();
			
			try{
				String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlPrices, null, cookies);
				jsonPrices = new JSONObject(json);
			} catch(Exception e){
				
			}
			
			if(jsonPrices.has("payload")){
				JSONObject payload = jsonPrices.getJSONObject("payload");
				
				if(payload.has("data")){
					JSONObject data = payload.getJSONObject("data");
					
					if(data.has("product")){
						JSONObject product = data.getJSONObject("product");
						
						if(product.has("bestPaymentOption")){
							JSONArray installments = product.getJSONArray("bestPaymentOption");
							
							for(int i = 0; i < installments.length(); i++){
								JSONObject installment = installments.getJSONObject(i);
								
								if(installment.has("number")){
									Integer parcela = installment.getInt("number");
									
									if(installment.has("value")){
										Float value = Float.parseFloat(installment.getString("value"));
										
										installmentsPriceMap.put(parcela, value);
									}
								}
							}
						}
					}
				}
			}
			
			//preço uma vez no cartão é igual ao do boleto.
			if(installmentsPriceMap.containsKey(1)){
				prices.setBankTicketPrice(installmentsPriceMap.get(1));
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
		}
		
		return prices;
	}

}