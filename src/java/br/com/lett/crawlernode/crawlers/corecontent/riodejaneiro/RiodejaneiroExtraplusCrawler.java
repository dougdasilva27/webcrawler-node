package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class RiodejaneiroExtraplusCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.extraplus.com.br/";

	public RiodejaneiroExtraplusCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			String id = this.session.getOriginalURL().split("/")[4];
			String internalID = null;
			if (id != null) {
				internalID = Integer.toString(Integer.parseInt(id.split("-")[0]));
			}

			// Nome
			Elements elementName = doc.select("h1.title-page-product");
			String name = null;
			if (elementName != null) {
				name = elementName.text().replace("'", "").trim();
			}

			// Preço
			Elements elementPrice = doc.select("div.price span");
			Float price = null;
			if(elementPrice != null){
				price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
			// Disponibilidade
			boolean available = true;

			// Categories
			ArrayList<String> categories = this.crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);
			

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.head().getElementsByAttributeValue("property", "og:image");
			if(element_foto.size()>1){
				primaryImage = element_foto.get(1).attr("content").trim();
			}
			else{
				primaryImage = element_foto.first().attr("src").trim();	
			}
			if(primaryImage.contains("produto_sem_foto")) {
				primaryImage = "";
			}

			String secondaryImages = null;

			// Descrição
			Element descriptionElement = doc.select(".description").first();
			String description = "";
			if (descriptionElement != null) {
				description += descriptionElement.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();

			// Prices
			Prices prices = crawlPrices(doc, price);
			
			// create a product
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
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

	private boolean isProductPage(String url) {
		return url.contains("produto");
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		Elements elementCategories = document.select(".breadcrumb a span");
		ArrayList<String> categories = new ArrayList<>();

		for(int i = 1; i < elementCategories.size(); i++) { // starts with index 1 because the first item is the home page
			Element e = elementCategories.get(i);
			String tmp = e.text().toString();

			categories.add(tmp);
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	
	/**
	 * No bank slip payment method in this ecommerce.
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
			
			Element installments = doc.select(".parcel").first();
			
			if (installments != null) {
				Element installmentElement = installments.select("span").first();
				
				if(installmentElement != null) {
					Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));
					
					Element valueElement = installments.select("span").last();
					
					if (valueElement != null) {
						Float value = MathUtils.parseFloat(valueElement.text());
						
						installmentPriceMap.put(installment, value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		}
		
		return prices;
	}
}