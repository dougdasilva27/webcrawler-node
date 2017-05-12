package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import models.Prices;

public class SaopauloAraujoCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.araujo.com.br/";

	public SaopauloAraujoCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select("input#___rc-p-id").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.attr("value").trim();
			}

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements elementCategories = doc.select(".bread-crumb li");
			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";

			int j = 0;
			for(int i=0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = "";

			// Descrição
			String description = "";

			try {

				Element[] sections = new Element[]{
						doc.select(".product-sku-info-wrapper .row").first(),
						doc.select("td.Informe-Ministerio-Saude-01").first(),
						doc.select(".product-tabs").first(),
				};
				for(Element e: sections) {if(e != null) description = description + e.html(); }
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// Pegar produtos dentro da url
			Elements elementProduct = doc.select(".skuList");
			
			for (int i=0; i < elementProduct.size(); i++) {

				Element produto = elementProduct.get(i);

				String internalID = "";
				Float price = null;

				// Disponibilidade
				boolean available = true;
				Element elementBuyButton = produto.select(".buy-button").first();
				Element elementNotifyMe = produto.select(".portal-notify-me-ref").first();

				if(elementBuyButton != null) {

					available = true;

					// ID interno
					String url_id = elementBuyButton.attr("href");
					String params = url_id.split("\\?")[1];
					Map<String, String> paramsMap = new HashMap<String, String>();
					for(String s: params.split("&")) {
						try {
							paramsMap.put(s.split("=")[0], s.split("=")[1]);
						} catch (Exception e) { }
					}

					internalID = paramsMap.get("sku");

					// Preço
					Elements elementPrice = produto.select(".preco strong");
					price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

				} else if(elementNotifyMe != null) {

					String line_id = produto.select("script").html();

					Pattern pattern = Pattern.compile("(\\{'sku': )(.*?)(, 'strings')");
					Matcher matcher = pattern.matcher(line_id);

					while (matcher.find()) {	            	        
						internalID = matcher.group(2);
					}

					available = false;

				} else {
					Logging.printLogError(logger, session, "Não encontrei nem botão de compra nem de avise-me!");
					break;
				}

				// Nome
				Elements elementName = produto.select(".nomeSku");
				String name = elementName.text().trim();

				// Imagem primária
				Elements elementPrimaryImage = produto.select(".imageSku img");
				String primaryImage = elementPrimaryImage.attr("src").replace("-65-65/", "-1000-1000/");

				// Imagens secundárias através da API da Araújo
				String secondaryImages = null;
				JSONArray secundaryImagesArray = new JSONArray();

				JSONArray extraInfo = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, ("http://www.araujo.com.br/produto/sku/" + internalID), null, null);

				// Estoque
				Integer stock = crawlStock(extraInfo);
				
				if(extraInfo != null && extraInfo.length() > 0 && extraInfo.getJSONObject(0) != null && extraInfo.getJSONObject(0).has("Images")) {

					JSONArray extraInfoArrayImages = extraInfo.getJSONObject(0).getJSONArray("Images");

					for(int r = 0; r < extraInfoArrayImages.length(); r++) {

						JSONArray img = extraInfoArrayImages.getJSONArray(r);

						for(int z = 0; z < img.length(); z++) {

							if(img.getJSONObject(z).getString("Path").contains("-1000-1000")) {

								if(img.getJSONObject(z).getBoolean("IsMain")) {
									primaryImage = img.getJSONObject(z).getString("Path").trim();
								} 
								else {
									secundaryImagesArray.put(img.getJSONObject(z).getString("Path").trim());
								}

							}

						}

					}

				}

				if(secundaryImagesArray.length() > 0) {
					secondaryImages = secundaryImagesArray.toString();
				}

				// Marketplace
				Marketplace marketplace = new Marketplace();

				// Prices
				Prices prices = crawlPrices(internalID, price);
				
				// Creating the product
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
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Elements elementProduct = doc.select(".skuList");
		return (!url.contains("qd-buy-button") && elementProduct.size() > 0);
	}
	
	private Integer crawlStock(JSONArray jsonSkuArray){
		Integer stock = null;
		
		if(jsonSkuArray.length() > 0){
			JSONObject jsonSku = jsonSkuArray.getJSONObject(0);
			if(jsonSku.has("SkuSellersInformation")){
				JSONObject sku = jsonSku.getJSONArray("SkuSellersInformation").getJSONObject(0);
				
				if(sku.has("AvailableQuantity")){
					stock = sku.getInt("AvailableQuantity");
				}
			}
		}
		
		return stock;
	}

	private Prices crawlPrices(String internalId, Float price){
		Prices prices = new Prices();

		if(price != null){
			String url = "http://www.araujo.com.br/productotherpaymentsystems/" + internalId;

			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

			Element bank = doc.select("#ltlPrecoWrapper em").first();
			if(bank != null){
				prices.setBankTicketPrice(Float.parseFloat(bank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim()));
			}

			Elements cardsElements = doc.select("#ddlCartao option");

			for(Element e : cardsElements){
				String text = e.text().toLowerCase();

				if (text.contains("visa")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
					
				} else if (text.contains("mastercard")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
					
				} else if (text.contains("diners")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
					
				} else if (text.contains("american") || text.contains("amex")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);	
					
				} else if (text.contains("hipercard") || text.contains("amex")) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);	
					
				} else if (text.contains("credicard") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);
					
				} else if (text.contains("elo") ) {
					Map<Integer,Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
					prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
					
				}
			} 


		}

		return prices;
	}

	private Map<Integer,Float> getInstallmentsForCard(Document doc, String idCard){
		Map<Integer,Float> mapInstallments = new HashMap<>();

		Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
		for(Element i : installmentsCard){
			Element installmentElement = i.select("td.parcelas").first();

			if(installmentElement != null){
				String textInstallment = installmentElement.text().toLowerCase();
				Integer installment = null;

				if(textInstallment.contains("vista")){
					installment = 1;					
				} else {
					installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
				}

				Element valueElement = i.select("td:not(.parcelas)").first();

				if(valueElement != null){
					Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

					mapInstallments.put(installment, value);
				}
			}
		}

		return mapInstallments;
	}

}
