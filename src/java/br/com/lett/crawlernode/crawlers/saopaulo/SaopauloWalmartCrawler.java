package br.com.lett.crawlernode.crawlers.saopaulo;

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
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloWalmartCrawler extends Crawler {

	public SaopauloWalmartCrawler(CrawlerSession session) {
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
		List<Product> products = new ArrayList<Product>();

		if(session.getOriginalURL().startsWith("http://www.walmart.com.br/produto/") || session.getOriginalURL().startsWith("https://www.walmart.com.br/produto/") || (session.getOriginalURL().endsWith("/pr"))) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Nome
			Elements elementName = doc.select("h1[itemprop=\"name\"]");
			String name = elementName.text().replace("'","").replace("’","").trim();

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

			// Imagens
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secundaryImagesArray = new JSONArray();

			Elements element_fotos = doc.select("#wm-pictures-carousel a");

			if(!element_fotos.isEmpty()){
				for(int i=0; i<element_fotos.size();i++){
					Element e = element_fotos.get(i);

					String img_url = null;

					if(e.attr("data-zoom") != null && !e.attr("data-zoom").isEmpty()) {
						img_url = e.attr("data-zoom");
					} else if(e.attr("data-normal") != null && !e.attr("data-normal").isEmpty()) {
						img_url = e.attr("data-normal");
					} else if(e.attr("src") != null && !e.attr("src").isEmpty()) {
						img_url = e.attr("src");
					}

					if(img_url != null && !img_url.startsWith("http")) {
						img_url = "http:" + img_url;
					}

					if(primaryImage == null) {
						primaryImage = img_url;
					} else {
						secundaryImagesArray.put(img_url);
					}

				}

			}

			if(secundaryImagesArray.length() > 0) {
				secondaryImages = secundaryImagesArray.toString();
			}

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

				String productId = productsListInfo.getJSONObject(p).get("skuId").toString();

				String productCustomName = productsListInfo.getJSONObject(p).getString("name");

				// Estoque
				Integer stock = null;

				// Price
				Float price = null;

				// Availability
				boolean available = false;

				// availability, price and marketplace
				Map<String, Float> marketplaceMap = this.extractMarketplace(productId, internalPid);
				JSONArray marketplace = new JSONArray();
				for (String partnerName : marketplaceMap.keySet()) {
					if (partnerName.equals("walmart")) { // se o walmart está no mapa dos lojistas, então o produto está disponível
						available = true;
						price = marketplaceMap.get(partnerName);
					} else { // se não for o walmart, insere no json array de lojistas
						JSONObject partner = new JSONObject();
						partner.put("name", partnerName);
						partner.put("price", marketplaceMap.get(partnerName));

						marketplace.put(partner);
					}
				}
				
				//Prices
				Prices prices = crawlPrices(internalPid, price);

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

	private Map<String, Float> extractMarketplace(String productId, String internalPid) {
		Map<String, Float> marketplace = new HashMap<String, Float>();

		// Fazendo request da página com informações de lojistas
		Document infoDoc = fetchMarketplaceInfoDocMainPage(productId);
		Elements sellers = infoDoc.select(".content-wrapper");
		
		for(Element e : sellers){
			Element nameElement = e.select(".seller-name a").first();
			
			if(nameElement != null){
				String name = nameElement.ownText().trim().toLowerCase();
				Float price = null;
				
				Element priceElement = e.select(".product-price").first();
				
				if(priceElement != null){
					price = Float.parseFloat(priceElement.attr("data-sellprice").trim());
				}
				
				marketplace.put(name, price);
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

					Element priceElement = e.select(".payment-price").first();
					Float price = null;

					if(priceElement != null){
						price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
					}

					marketplace.put(name, price);
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


	private Prices crawlPrices(String internalPid, Float price){
		Prices p = new Prices();
		Map<Integer, Float> installmentPriceMap = new HashMap<>();
		
		//preço principal é o mesmo preço do boleto
		p.insertBankTicket(price);
		
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
			
			p.insertCardInstallment(Prices.VISA, installmentPriceMap);
		}
		
		return p;
	}
}
