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
 * Date: 14/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogariaprimusCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.drogariaprimus.com.br/";

	public BrasilDrogariaprimusCrawler(Session session) {
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
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc, internalId);
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
		if (doc.select("#detalhe_produto").first() != null) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;

		Element internalIdElement = doc.select(".corpo_conteudo article[itemid]").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("itemid");
		}

		return internalId;
	}
	
	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pdi = doc.select("#produto_cod_ref").first();
		
		if(pdi != null) {
			internalPid = pdi.ownText().trim();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1[itemprop=name]").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document, String internalId) {
		Float price = null;
		Element salePriceElement = document.select("meta[itemprop=price]").first();		

		if (salePriceElement != null) {
			price = Float.parseFloat(salePriceElement.attr("content"));
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select("#product_gallery li > a").first();
		
		if(elementPrimaryImage != null ) {
			primaryImage = elementPrimaryImage.attr("href");
			
			if(!primaryImage.startsWith("http:")) {
				primaryImage = "http:" + primaryImage;
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
	 * No momento que o crawler foi feito não foi achado
	 * produtos com categorias
	 * @param document
	 * @return
	 */
	private CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select(".ondeestou a > span");
		
		for (Element e : elementCategories) { 
			String cat = e.ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".texto11").last();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		return description.toString();
	}
	
	private boolean crawlAvailability(Document doc) {
		return doc.select("#produto_notificacao[style=display:block;]").first() == null;		
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

			Element aVista = doc.select(".forma_pagto #vl_avista b").first();
			
			if(aVista != null) {
				Float priceBankTicket = MathCommonsMethods.parseFloat(aVista.ownText());
				
				if(priceBankTicket != null) {
					prices.setBankTicketPrice(priceBankTicket);
				}
			}
			
			Elements installmentsElement = doc.select(".forma_pagto span b");
			
			if(installmentsElement.size() > 1) {
				String parcel = installmentsElement.get(0).ownText().replaceAll("[^0-9]", "").trim();
				String value = installmentsElement.get(1).ownText();
				
				if(value.contains(".")) {
					value = value.replaceAll("[^0-9.]", "").trim();
				} else {
					value = value.replaceAll("[^0-9,]", "").replace(",", ".").trim();
				}
				
				if(!parcel.isEmpty() && value != null) {
					installmentPriceMap.put(Integer.parseInt(parcel), Float.parseFloat(value));
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
