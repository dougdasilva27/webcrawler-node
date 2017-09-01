package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (15/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) InternalId is in url, because in webranking is the unique id found.
 * 
 * 8) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * 9) There is no secondary image for this market
 * 
 * Examples:
 * ex1 (available): http://loja.paguemenos.com.br/alimento-infantil-nestle-iogurte-original-120-g-387195.aspx/p
 * ex2 (unavailable): http://loja.paguemenos.com.br/alimento-infantil-nestle-frutas-tropicais-120g-4922.aspx/p
 *
 * Optimizations notes:
 * No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class PaguemenosCrawler {

	public static List<Product> extractInformation(Document doc, Logger logger, Session session) throws Exception {
		List<Product> products = new ArrayList<>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

			/* ***********************************
			 * crawling data of only one product *
			 *************************************/

			String internalId = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);
			Float price = crawlMainPagePrice(doc);
			boolean available = price != null;
			CategoryCollection categories = crawlCategories(doc);
			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			Map<String, Float> marketplaceMap = crawlMarketplace(doc);
			Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
			Prices prices = crawlPrices(doc, price);
			
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
			Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
		}
		
		return products;
	}

	private static boolean isProductPage(Document doc) {
		return doc.select("#info-product").first() != null;
	}

	private static String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select("#liCodigoInterno span[itemprop=identifier]").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.text().trim();			
		}

		return internalId;
	}

	private static String crawlInternalPid(Document document) {
		String internalPid = null;
		Element pid = document.select("#ProdutoCodigo").first();
		
		if(pid != null) {
			internalPid = pid.val();
		}
		
		return internalPid;
	}

	private static String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("h1.name.fn").first();

		if (nameElement != null) {
			name = nameElement.text().toString().trim();
		}

		return name;
	}

	private static Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element mainPagePriceElement = document.select("#lblPrecoPor strong").first();

		if (mainPagePriceElement != null) {
			price = Float.parseFloat( mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
		}

		return price;
	}

	private static Map<String, Float> crawlMarketplace(Document document) {
		return new HashMap<>();
	}

	private static Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
		return new Marketplace();
	}

	private static String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".collum.images #hplAmpliar").first();

		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();

			if(primaryImage.equals("#")){ //no image for product
				return null;
			}
		}

		return primaryImage;
	}

	private static String crawlSecondaryImages(Document document) {
		String secondaryImages = null;

		return secondaryImages;
	}

	/**
	 * @param document
	 * @return
	 */
	private static CategoryCollection crawlCategories(Document document) {
		CategoryCollection categories = new CategoryCollection();
		Elements elementCategories = document.select("#breadcrumbs span a[href] span");
		
		for (int i = 1; i < elementCategories.size(); i++) { 
			String cat = elementCategories.get(i).ownText().trim();
			
			if(!cat.isEmpty()) {
				categories.add( cat );
			}
		}

		return categories;
	}

	private static String crawlDescription(Document document) {
		String description = "";
		Element descriptionElement = document.select("#panCaracteristica").first();

		if (descriptionElement != null) {
			description = description + descriptionElement.html();
		}

		return description;
	}

	/**
	 * In this market has no bank slip payment method
	 * @param doc
	 * @param price
	 * @return
	 */
	private static Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();
		
		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();
			installmentPriceMap.put(1, price);
			
			Element installments = doc.select("#lblParcelamento").first();
			
			if(installments != null){
				Element installmentElement = installments.select("#lblParcelamento1 > strong").first();
				
				if(installmentElement != null) {
					Integer installment = Integer.parseInt(installmentElement.text().replaceAll("[^0-9]", ""));
					
					Element valueElement = installments.select("#lblParcelamento2 > strong").first();
					
					if(valueElement != null) {
						Float value = MathCommonsMethods.parseFloat(valueElement.text());
						
						installmentPriceMap.put(installment, value);
					}
				}
			}
			
			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
		}
		
		return prices;
	}
}
