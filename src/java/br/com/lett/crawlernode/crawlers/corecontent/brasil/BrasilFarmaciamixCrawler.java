package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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
 * Date: 16/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilFarmaciamixCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.farmaciamix.com.br/";

	public BrasilFarmaciamixCrawler(Session session) {
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
		if (doc.select("#ID_SubProduto").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select("#ID_SubProduto").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element salePriceElement = document.select("p[itemprop=price]").first();	
		Element lowPrice = document.select("span[itemprop=lowPrice]").first();

		if (salePriceElement != null) {
			price = Float.parseFloat(salePriceElement.attr("content"));
		} else if(lowPrice != null) {
			price = Float.parseFloat(lowPrice.attr("content"));
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select("#zoom_01").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("data-zoom-image").trim();

			if (primaryImage.isEmpty()) {
				primaryImage = elementPrimaryImage.attr("src").trim();
			}
		} 
		
		return primaryImage;
	}

	/**
	 * Quando este crawler foi feito não achei imagens secundarias
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
		Elements elementCategories = document.select(".caminho-de-milho li > a");
		
		for (int index = 1; index < elementCategories.size()-1; index++) { 
			String cat = elementCategories.get(index).text().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".col-infos-produto").last();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		Element bula = doc.select(".tab-links li > a").last();
		
		if(bula != null) {
			String url = bula.attr("href");
			
			Document docBula = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
			
			Element shortDescription = docBula.select(".informacoes-bula").last();
			
			if (shortDescription != null) {
				description.append(shortDescription.html());		
			}
			
			Element bulaElement = docBula.select(".col-infos-produto").last();
			
			if (bulaElement != null) {
				description.append(bulaElement.html());		
			}
		}
		
		return description.toString();
	}
	
	private boolean crawlAvailability(Document doc) {
		return doc.select(".BtComprarPgProduto").first() != null;		
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
			prices.setBankTicketPrice(price);
			
			Elements installmentsElements = doc.select(".col-condicoes-and-frete .list-padrao li");
			
			for(Element e : installmentsElements) {
				String text = e.ownText().trim().toLowerCase();
				
				if(text.contains("x")) {
					int x = text.indexOf('x');
					
					String parcel = text.substring(0, x).replaceAll("[^0-9]", "").trim();
					Float value = MathCommonsMethods.parseFloat(text.substring(x));
					
					if(!parcel.isEmpty() && value != null) {
						installmentPriceMap.put(Integer.parseInt(parcel), value);
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
