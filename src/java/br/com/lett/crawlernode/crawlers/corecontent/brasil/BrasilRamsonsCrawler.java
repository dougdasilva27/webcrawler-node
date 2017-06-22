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
 * Date: 22/06/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilRamsonsCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.cotodigital3.com.ar/";

	public BrasilRamsonsCrawler(Session session) {
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

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			Prices prices = crawlPrices(price, doc);
			boolean available = price != null;
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

	private boolean isProductPage(String url) {
		if (url.endsWith("/p")) {
			return true;
		}
		return false;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element internalIdElement = document.select("#ProdutoCodigo").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.val();
		}

		return internalId;
	}

	private String crawlInternalPid(Document doc) {
		String internalPid = null;
		Element pid = doc.select("#liCodigoInterno span").first();
		
		if(pid != null) {
			internalPid = pid.ownText().trim();
		}
		
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.name").first();

		if (nameElement != null) {
			name = nameElement.ownText().trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		String priceText = null;
		Element salePriceElement = document.select(".price.sale strong").first();		

		if (salePriceElement != null) {
			priceText = salePriceElement.ownText();
			price = MathCommonsMethods.parseFloat(priceText);
		} else {
			salePriceElement = document.select("#lblPreco.regular").first();
			if (salePriceElement != null) {
				priceText = salePriceElement.ownText();
				price = MathCommonsMethods.parseFloat(priceText);
			}
		}

		return price;
	}

	private Marketplace crawlMarketplace() {
		return new Marketplace();
	}


	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element elementPrimaryImage = doc.select("a#Zoom1").first();
		
		if(elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("href");
		} 

		if (primaryImage == null || primaryImage.isEmpty() && (elementPrimaryImage != null)) {
			Element elementPrimaryImage2 = elementPrimaryImage.select("img").first();
			
			if (elementPrimaryImage2 != null) {
				primaryImage = elementPrimaryImage2.attr("src").trim();
			}
		}
		
		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select(".thumbs li > a > img");

		for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
			secondaryImagesArray.put( imagesElement.get(i).attr("src").trim() );	
		}

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

		Elements elementCategories = document.select("#breadcrumbs a");
		for (int i = 1; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element elementDescription = doc.select(".content .main-content #description").first();
		Element elementEspecificacion = doc.select(".content .main-content #panCaracteristica").first();
		Element elementDimensions = doc.select(".content .main-content #divSizeTable").first();
		
		if (elementDescription != null) {
			description.append(elementDescription.html());		
		}
		
		if (elementEspecificacion != null) {
			description.append(elementEspecificacion.html());		
		}
		
		if (elementDimensions != null) {
			description.append(elementDimensions.html());		
		}
		
		return description.toString();
	}

	/**
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Float price, Document doc) {
		Prices prices = new Prices();
		
		if (price != null) {
			Map<Integer,Float> installmentPriceMap = new TreeMap<>();
			
			Element vista = doc.select("#lblPrecoAVista.preco-avista").first();
			
			if(vista != null) {
				String priceVistaString = vista.ownText();
				
				if(!priceVistaString.isEmpty()) {
					Float priceVista = MathCommonsMethods.parseFloat(priceVistaString);
					
					prices.setBankTicketPrice(priceVista);
					installmentPriceMap.put(1, priceVista);
				} else {
					prices.setBankTicketPrice(price);
					installmentPriceMap.put(1, price);
				}
			} else {
				prices.setBankTicketPrice(price);
				installmentPriceMap.put(1, price);
			}
	
			Element installments = doc.select("#lblParcelamento").first();
			
			if(installments != null) {
				Element firstInstallment = installments.select("#lblParcelamento1 strong").first();
				
				if(firstInstallment != null) {
					Integer installment = Integer.parseInt(firstInstallment.ownText().replaceAll("[^0-9]", ""));
					
					Element installmentValue = installments.select("#lblParcelamento2 strong").first();
					
					if(installmentValue != null) {
						Float priceInstallment = MathCommonsMethods.parseFloat(installmentValue.ownText());
						
						installmentPriceMap.put(installment, priceInstallment);
					}
				}
				
				Elements secondInstallment = installments.select("#lblOutroParc strong");
				
				if(secondInstallment.size() >= 2) {
					Integer installment = Integer.parseInt(secondInstallment.get(0).text().replaceAll("[^0-9]", ""));
					Float priceInstallment = MathCommonsMethods.parseFloat(secondInstallment.get(1).text());
					
					installmentPriceMap.put(installment, priceInstallment);
				}
			}

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
		}
		
		return prices;
	}

}
