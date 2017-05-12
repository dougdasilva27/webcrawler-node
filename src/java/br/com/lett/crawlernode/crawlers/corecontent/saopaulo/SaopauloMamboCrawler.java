package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;


public class SaopauloMamboCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.mambo.com.br/";

	public SaopauloMamboCrawler(Session session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String internalId = crawlInternalId(doc);
			String name = crawlName(doc);
			Float price = crawlPrice(doc);
			boolean available = crawlAvailability(doc);

			// Categorias
			Elements element_categories = doc.select(".bread-crumb ul li"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";

			int j=0;
			for(int i=0; i < element_categories.size(); i++) {

				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;

			}
			category1 = cat[1];
			category2 = cat[2];

			if(element_categories.size()>3){
				category3 = cat[3];
			} 
			else {
				category3 = null;
			}

			String primaryImage = crawlPrimaryImage(doc);
			String secondaryImages = crawlSecondaryImages(doc);
			String description = crawlDescription(doc);
			Integer stock = null;
			JSONArray marketplace = null;
			Prices prices = crawlPrices(internalId, price);
			
			Product product = new Product();
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element element_id = document.select(".hidden-sku-default").first();
		return element_id != null;
	}
	
	private String crawlDescription(Document document) {
		Element elementDescription = document.select(".productDescription").first();
		if (elementDescription != null) {
			return elementDescription.html().replace("'","").replace("’","").trim();
		}
		return null;
	}
	
	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element elementId = doc.select(".hidden-sku-default").first();
		if (elementId != null) {
			internalId = Integer.toString(Integer.parseInt(elementId.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
		}
		
		return internalId;
	}
	
	private String crawlName(Document doc) {
		String name = null;
		Element elementName = doc.select(".productName").first();
		if (elementName != null) {
			name = elementName.text().replace("'","").replace("’","").trim();
		}
		
		return name;
	}
	
	private Float crawlPrice(Document doc) {
		Element elementPrice = doc.select(".skuBestPrice").last();
		Float price = null;
		if (elementPrice != null) { 
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}
		
		return price;
	}
	
	/**
	 * Extract availability information of the sku
	 * @param doc
	 * @return tru if sku is available or false otherwise
	 */
	private boolean crawlAvailability(Document doc) {
		if (crawlPrice(doc) == null) return false;
		return true;
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element image = doc.select("#image a").first();

		if (image != null) {
			primaryImage = image.attr("href");
		} else {
			image = doc.select("#image img").first();
			

			if (image != null) {
				primaryImage = image.attr("src");
			}
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements images = doc.select(".thumbs li a");


		for (int i = 1; i < images.size(); i++) {//starts with index 1, because the first image is the primary image

			String urlImage;
			urlImage = images.get(i).attr("zoom").trim();
			if (urlImage == null || urlImage.isEmpty()) {
				urlImage = images.get(i).attr("rel");
			}
			if (urlImage != null) {
				secondaryImagesArray.put(urlImage);    
			}

		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}
	
	/**
	 * To crawl this prices is required access a api
	 * Api return this:
	 * 
	 *	Mastercard à vista	R$ 7.443,90
	 *	Mastercard 2 vezes sem juros	R$ 3.721,95
	 *	Mastercard 3 vezes sem juros	R$ 2.481,30
	 *	Mastercard 4 vezes sem juros	R$ 1.860,97
	 *	Mastercard 5 vezes sem juros	R$ 1.488,78
	 * 
	 * No bank slip payment method in this market
	 * @param internalId
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(String internalId, Float price) {
		Prices prices = new Prices();

		if(price != null){
			String url = "http://www.mambo.com.br/productotherpaymentsystems/" + internalId;

			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

			Element bank = doc.select("#ltlPrecoWrapper em").first();
			if (bank != null) {
				prices.setBankTicketPrice(Float.parseFloat(bank.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim()));
			}

			Elements cardsElements = doc.select("#ddlCartao option");

			for (Element e : cardsElements) {
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