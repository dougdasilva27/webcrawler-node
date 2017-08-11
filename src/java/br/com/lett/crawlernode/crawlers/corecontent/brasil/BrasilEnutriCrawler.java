package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 11/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilEnutriCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.enutri.com.br/";

	public BrasilEnutriCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = null;
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = crawlAvailability(doc);
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Marketplace marketplace = crawlMarketplace();

			// Creating the product
			Product product = ProductBuilder.create()
					.setUrl(session.getOriginalURL())
					.setInternalId(internalId)
					.setInternalPid(internalPid)
					.setName(name)
					.setPrice(price)
					.setPrices(prices)
					.setAvailable(available)
					.setCategory1(categories.getCategory(0))
					.setCategory2(categories.getCategory(1))
					.setCategory3(categories.getCategory(2))
					.setPrimaryImage(primaryImage)
					.setSecondaryImages(secondaryImages)
					.setDescription(description)
					.setStock(stock)
					.setMarketplace(marketplace)
					.build();

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;

	}

	private boolean isProductPage(Document doc) {
		if (doc.select("input[name=product]").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select("input[name=product]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".product-name h1").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement = document.select("#finalPriceStrong").first();		

		if (salePriceElement != null) {
			price = MathCommonsMethods.parseFloat(salePriceElement.text());
		} 

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select(".product-image > a.cloud-zoom").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("href");
		} 
		
		return primaryImage;
	}

	/**
	 * Quando este crawler foi feito, nao tinha imagens secundarias
	 * 
	 * @param doc
	 * @return
	 */
	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".breadcrumbs li[class^=category] a");
		
		for (int i = 0; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementShortdescription = doc.select(".short-description").first();
		
		if (elementShortdescription != null) {
			description.append(elementShortdescription.html());		
		}
		
		Element elementDescription = doc.select("#product_tabs_description_contents").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		return description.toString();
	}
	
	private boolean crawlAvailability(Document doc) {
		return doc.select(".bt-buy-nova").first() != null;		
	}

	/**
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			installmentPriceMap.put(1, price);
			
			Element bank = doc.select(".box-img .flag strong").first();
			
			if(bank != null) {
				Float discount = MathCommonsMethods.parseFloat(bank.ownText());
				
				// O desconto aparece quando finalizamos a compra, com tudo a porcentagem aparece na page do produto
				if(discount != null) {
					Double value = price - (price * (discount / 100d));
					prices.setBankTicketPrice(MathCommonsMethods.normalizeTwoDecimalPlaces(value.floatValue()));
				} else {
					prices.setBankTicketPrice(price);
				}
			} else {
				prices.setBankTicketPrice(price);
			}
			
			Element installmentsElement = doc.select("#qtyInstallments").first();
			
			if(installmentsElement != null) {
				String text = installmentsElement.text().toLowerCase().trim();
				
				if(text.contains("x")) {
					int x = text.indexOf('x');
					
					String installmentText = text.substring(0, x).replaceAll("[^0-9]", "");
					Float value = MathCommonsMethods.parseFloat(text.substring(x).trim());
					
					if(!installmentText.isEmpty() && installmentText != "1" && value != null) {
						installmentPriceMap.put(Integer.parseInt(installmentText), value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
