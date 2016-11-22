package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

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
			Element elementInternalId = doc.select("input[name=product]").first();
			if(elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim();
			}

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".product-name h3").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().split(":")[1].replace(")", "").trim();
			}

			// Nome
			String name = null;
			Element elementName = doc.select(".product-name h2").first();
			if(elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = doc.select(".button.btn-cart").first();
			if (elementBuyButton == null) {
				available = false;
			}

			// Preço
			Float price = null;
			if(available) {
				Element elementPrice = doc.select(".preco-produto span.price").first();
				if(elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
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
			Element elementPrimaryImage = doc.select(".product-img-box .product-image-zoom img").first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src").replace("674x525/", "");
			}

			// Imagens secundárias
			Element elementProduct = doc.select(".product-essential").first();
			Elements elementImages = elementProduct.select(".product-img-box #products-carousel ul li a");
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
			Element elementDescription = doc.select("#product_tabs_description_contents").first();
			Element elementAdditionalContents = doc.select("#product_tabs_additional_contents").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}
			if (elementAdditionalContents != null) {
				description = description + elementAdditionalContents.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices 
			Prices prices = crawlPrices(doc);
			
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


	private Prices crawlPrices(Document doc){
		Prices prices = new Prices();
		Map<Integer,Float> installmentPriceMap = new HashMap<>();
		
		Elements installments = doc.select(".bloco-formas-pagamento li");
		
		for(Element e : installments){
			String text = e.text().toLowerCase();
			int x = text.indexOf("x");
			
			Integer installment = Integer.parseInt(text.substring(0,x).trim());
			Float value = Float.parseFloat(text.substring(x).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			
			installmentPriceMap.put(installment, value);
		}
		
		if(installmentPriceMap.size() > 0){
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
		}
		
		Element priceBoleto = doc.select(".in-cash").first();
		
		if(priceBoleto != null){
			Float bankTicketPrice = Float.parseFloat(priceBoleto.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			
			if(bankTicketPrice > 0){
				prices.insertBankTicket(bankTicketPrice);
			} else if(installmentPriceMap.size() > 0) {
				prices.insertBankTicket(installmentPriceMap.get(1));
			}
			
		} else if(installmentPriceMap.size() > 0) {
			prices.insertBankTicket(installmentPriceMap.get(1));
		}
		
		return prices;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select(".product-essential").first();
		return (elementProduct != null);
	}
}
