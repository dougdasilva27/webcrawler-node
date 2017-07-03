package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilIbyteCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ibyte.com.br/";

	public BrasilIbyteCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product>  extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementId = doc.select(".view-sku").first();
			if (elementId != null && elementId.text().contains(":")) {
				internalId = elementId.text().split(":")[1].replace(")", "").trim();
			}

			// Pid
			String internalPid = null;
			Element elementPid = doc.select("input[name=product]").first();
			if(elementPid != null) {
				internalPid = elementPid.attr("value").trim();
			}
			
			// Nome
			String name = null;
			Element elementName = doc.select(".product-name h1").first();
			if(elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			boolean available = false;
			Element elementBuyButton = doc.select(".back-in-stock-form.avise_me").first();
			if (elementBuyButton == null) {
				available = true;
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".regular-price .price").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumbs li a");
			for (int i = 1; i < elementCategories.size()-1; i++) {
				String c = elementCategories.get(i).text().trim();
				if (category1.isEmpty()) {
					category1 = c;
				} else if (category2.isEmpty()) {
					category2 = c;
				} else if (category3.isEmpty()) {
					category3 = c;
				}
			}

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select(".product-image .cloud-zoom").first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("href");
			}

			// Imagens secundárias
			Elements elementImages = doc.select("#galeria div a");
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element image : elementImages) {
				if( !image.attr("href").equals(primaryImage) ) {
					secondaryImagesArray.put(image.attr("href"));
				}
			}			
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#descricao").first();
			Element elementAdditionalContents = doc.select("#atributos").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}
			if (elementAdditionalContents != null) {
				description = description + elementAdditionalContents.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();
			
			// Prices 
			Prices prices = crawlPrices(doc, price);
			
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
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}


	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		Map<Integer,Float> installmentPriceMap = new HashMap<>();
		
		if(price != null) {
			Elements installments = doc.select(".formas li #simularParcelamento tr");
			
			for(Element e : installments){
				Elements values = e.select("td");
				
				if(values.size() > 1) {				
					Integer installment = Integer.parseInt(values.first().ownText().replaceAll("[^0-9]", ""));
					Float value = Float.parseFloat(values.get(1).ownText().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
					
					installmentPriceMap.put(installment, value);
				}
			}
			
			if(installmentPriceMap.size() < 1){
				installmentPriceMap.put(1, price);
			}
			
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			
			Element priceBoleto = doc.select(".boletoBox .price").first();
			
			if(priceBoleto != null){
				Float bankTicketPrice = Float.parseFloat(priceBoleto.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
				
				if(bankTicketPrice > 0){
					prices.setBankTicketPrice(bankTicketPrice);
				} else {
					prices.setBankTicketPrice(price);
				}
			} else {
				prices.setBankTicketPrice(price);
			}
		}
		
		return prices;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select(".view-sku").first();
		return (elementProduct != null);
	}
}
