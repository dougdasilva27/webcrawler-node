package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

/**
 * 
 * This crawler uses WebDriver to detect when we have sku
 * variations on the same page.
 * 
 * Sku price crawling notes:
 * 1) The payment options can change between different card brands.
 * 2) The ecommerce share the same cad payment options between sku variations.
 * 3) The cash price (preço a vista) can change between sku variations, and it's crawled from the main page.
 * 4) To get card payments, first we perform a POST request to get the list of all card brands, then we perform
 * one POST request for each card brand.
 * 
 * @author Samir Leao
 *
 */

public class FlorianopolisAngelonieletroCrawler extends Crawler {

	/**
	 * Shared attribute between sku variations
	 */	
	private SharedData sharedData = new SharedData();


	public FlorianopolisAngelonieletroCrawler(Session session) {
		super(session);
		super.config.setFetcher(Fetcher.WEBDRIVER);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		boolean shouldVisit = false;
		shouldVisit = !FILTERS.matcher(href).matches() && (href.startsWith("http://www.angeloni.com.br/eletro/"));

		return shouldVisit;
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

			if (hasVariations(doc)) {
				Logging.printLogDebug(logger, "Multiple variations...");

				Document variationDocument = doc;

				// get all sku options through the webdriver
				List<WebElement> options = this.webdriver.findElementsByCssSelector("div.col-sm-9 input[name=voltagem]");

				for (WebElement option : options) {
					String variation = option.getAttribute("id"); // 220 or 110

					// if the element is loaded we crawl it and append variation on the name
					if (!isLoaded(variationDocument, option)) {

						// click
						Logging.printLogDebug(logger, session, "Clicking on option...");
						this.webdriver.clickOnElementViaJavascript(option);

						// give some time for safety
						Logging.printLogDebug(logger, session, "Waiting 2 seconds...");
						this.webdriver.waitLoad(2000);

						// get the new html and parse
						String html = this.webdriver.findElementByCssSelector("html").getAttribute("innerHTML");
						variationDocument = Jsoup.parse(html);
					}

					// crawl sku
					Product product = crawlProduct(variationDocument);

					// append variation on sku name
					String completeName = product.getName() + " " + variation + "v";
					
					product.setName(completeName);

					products.add(product);
				}


			} else {
				Product product = crawlProduct(doc);
				products.add(product);
			}



		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("http://www.angeloni.com.br/eletro/p/");
	}

	private Product crawlProduct(Document document) {
		Product product = new Product();

		String internalId = crawlInternalId(document);
		String internalPid = crawlInternalPid(document);

		String name = crawlName(document);
		if (sharedData.baseName == null) {
			sharedData.baseName = name;
		} else {
			name = sharedData.baseName;
		}

		Float price = crawlPrice(document);
		Prices prices = crawlPrices(document);
		boolean available = crawlAvailability(document);
		String primaryImage = crawlPrimaryImage(document);
		String secondaryImages = crawlSecondaryImages(document);
		Integer stock = null;
		String description = crawlDescription(document);
		JSONArray marketplace = null;

		ArrayList<String> categories = crawlCategories(document);
		String category1 = getCategory(categories, 0);
		String category2 = getCategory(categories, 1);
		String category3 = getCategory(categories, 2);		

		product.setUrl(session.getOriginalURL());
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

		return product;
	}

	/**
	 * The payment options are the same across variations.
	 * So the same prices object e set to all the crawled products.
	 * 
	 * To crawl the card payment options we must request for the list
	 * of all card brands that can be used. The, for each card brand
	 * we must request for the payment options. They can change between
	 * card brands.
	 *
	 * @param document
	 * @return
	 */
	private Prices crawlPrices(Document document) {
		Prices prices = new Prices();

		boolean isAvailable = crawlAvailability(document);

		if (isAvailable) {

			Float price = crawlPrice(document);
			prices.insertBankTicket(price);

			if (this.sharedData.cardInstallmentsMap == null) {
				this.sharedData.cardInstallmentsMap = crawlCardInstallmentsMap(document);
			} 

			for (String cardBrand : this.sharedData.cardInstallmentsMap.keySet()) {
				prices.insertCardInstallment(cardBrand, this.sharedData.cardInstallmentsMap.get(cardBrand));
			}
		}

		return prices;
	}

	/**
	 * 
	 * @param document
	 */
	private Map<String, Map<Integer, Float>> crawlCardInstallmentsMap(Document document) {
		Map<String, Map<Integer, Float>> cardInstallmentsMap = new HashMap<String, Map<Integer, Float>>();

		Set<Card> cards = crawlSetOfCards(document);

		Float price = crawlPrice(document);

		for (Card card : cards) {
			String compatibleCardName = createCompatibleName(card);
			if (compatibleCardName != null) {

				// assemble POST parameters
				String parameters = 
						"cardTypeKey=" + compatibleCardName + 
						"&totalValue=" + price + 
						"&useTheBestInstallment=false";

				// perform request
				Document response = DataFetcher.fetchDocument(
						DataFetcher.POST_REQUEST, 
						session, 
						"http://www.angeloni.com.br/eletro/modais/installmentsRender.jsp", 
						parameters, 
						null);

				Map<Integer, Float> installments = crawlInstallmentsFromPaymentRequestResponse(response);

				cardInstallmentsMap.put(card.toString(), installments);
			}
		}

		return cardInstallmentsMap;
	}

	/**
	 * 
	 * Get all the installments numbers and values from the
	 * content of the POST request to payment methods on a certain
	 * card brand.
	 * 
	 * e.g:
	 * 
	 * 	Nº de parcelas
	 *	á vista
	 *	2 vezes sem juros
	 *	3 vezes sem juros
	 *	4 vezes sem juros
	 *	5 vezes sem juros
	 *
	 *	Valor de cada parcela
	 *	R$ 439,90
	 *	R$ 219,95
	 *	R$ 146,63
	 *	R$ 109,98
	 *	R$ 87,98
	 *
	 * @param document
	 * @return
	 */
	private Map<Integer, Float> crawlInstallmentsFromPaymentRequestResponse(Document document) {
		Map<Integer, Float> installments = new HashMap<Integer, Float>();

		Elements installmentNumberTextElements = document.select("div.numero-parcelas ul li");
		Elements installmentPriceTextElements = document.select("div.valor-parcelas ul li");

		if (installmentNumberTextElements.size() == installmentPriceTextElements.size()) {
			for (int i = 0; i < installmentNumberTextElements.size(); i++) {
				String installmentNumberText = installmentNumberTextElements.get(i).text();
				String installmentPriceText = installmentPriceTextElements.get(i).text();

				List<String> parsedNumbers = MathCommonsMethods.parseNumbers(installmentNumberText);
				if (parsedNumbers.size() == 0) {
					installments.put(1, MathCommonsMethods.parseFloat(installmentPriceText));
				} else {
					installments.put(Integer.parseInt(parsedNumbers.get(0)), MathCommonsMethods.parseFloat(installmentPriceText));
				}
			}
		}

		return installments;
	}

	private String createCompatibleName(Card card) {
		String compatibleName = null;

		if (card == Card.AMEX) compatibleName = "americanExpress";
		else if (card == Card.MASTERCARD) compatibleName = "masterCard";
		else if (card == Card.DINERS) compatibleName = "dinersClub";
		else compatibleName = card.toString();

		return compatibleName;
	}

	private Set<Card> crawlSetOfCards(Document document) {
		Set<Card> cards = new HashSet<Card>();

		// fetch list of cards through a POST request
		String internalId = crawlInternalId(document);
		Document response = DataFetcher.fetchDocument(
				DataFetcher.POST_REQUEST, 
				session, 
				"http://www.angeloni.com.br/eletro/modais/paymentMethods.jsp", 
				"productId=" + internalId, 
				null);

		Elements cardsElements = response.select("div.box-cartao h2");

		for (Element card : cardsElements) {
			String text = card.text().trim().toLowerCase();
			if (text.contains(Card.DINERS.toString())) cards.add(Card.DINERS);
			else if (text.contains(Card.MASTERCARD.toString())) cards.add(Card.MASTERCARD);
			else if (text.contains(Card.VISA.toString())) cards.add(Card.VISA);
			else if (text.contains("americanexpress")) cards.add(Card.AMEX);
			else if (text.contains(Card.HIPERCARD.toString())) cards.add(Card.HIPERCARD);
		}

		return cards;
	}


	/**
	 * It looks for voltage variations on the radio buttons.
	 * 
	 * @param document
	 * @return
	 */
	private boolean hasVariations(Document document) {
		Elements skuOptions = document.select("#formGroupVoltage input[type=radio]");
		if (skuOptions.size() > 1) return true;
		return false;
	}

	/**
	 * See if the current loaded informations are from the skuOption.
	 * 
	 * @param skuOption
	 * @return
	 */
	private boolean isLoaded(Document document, WebElement skuOption) {
		String currentLoadedInternalId = crawlInternalId(document);
		String optionInternalId = getInternalIdFromOption(skuOption);

		return currentLoadedInternalId.equals(optionInternalId);
	}

	/**
	 * The internalId of the current loaded corresponds to the first parameter of the updateInformations method call.
	 * 
	 * e.g:
	 * updateInformations('3520962','3520917', '/cartridges/DetalhesProduto/DetalhesProduto.jsp', 'product-details','true', '', '');
	 * 
	 * @return
	 */
	private String getInternalIdFromOption(WebElement skuOption) {
		String onclickAttr = skuOption.getAttribute("onclick");

		int firstQuotationMarkIndex = onclickAttr.indexOf("\'") + 1;
		String firstPartExcluded = onclickAttr.substring(firstQuotationMarkIndex, onclickAttr.length()); // 3520962','3520917', '/cartridges/DetalhesProduto/DetalhesProduto.jsp', 'product-details','true', '', '');

		int secondQuotationMarkIndex = firstPartExcluded.indexOf("\'");
		String firstParameter = firstPartExcluded.substring(0, secondQuotationMarkIndex); // 3520962

		return firstParameter;
	}

	private String crawlInternalId(Document document) {
		String internalId = null;

		Element elementInternalId = document.select(".codigo span[itemprop=sku]").first();
		if(elementInternalId != null) {
			internalId = elementInternalId.text().trim();
		}

		return internalId;
	}

	private String crawlName(Document document) {
		String name = null;

		Element elementName = document.select("#titulo h1[itemprop=name]").first();
		if(elementName != null) {
			name = elementName.text();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;

		Element elementPrice = document.select("div#descricao .esquerda .valores .preco-por .microFormatoProduto").first();
		if(elementPrice != null) {
			price = MathCommonsMethods.parseFloat(elementPrice.text());
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = true;

		Element elementAvailable = document.select("div#descricao .esquerda .produto-esgotado").first();
		if(elementAvailable != null) {
			available = false;
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;

		Element elementPrimaryImage = document.select("div#imagem-grande a").first();
		if(elementPrimaryImage != null) {
			primaryImage = "http:" + elementPrimaryImage.attr("href").replace("//", "");
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		String primaryImage = crawlPrimaryImage(document);

		Elements elementsSecondaryImages = document.select("section#galeria .jcarousel-wrapper .jcarousel ul li a");
		int elementsSize = elementsSecondaryImages.size();
		JSONArray secondaryImagesArray = new JSONArray();

		if (elementsSize > 0) {
			for(int i = 0; i < elementsSize; i++) {
				String secondaryImage = "http:" + elementsSecondaryImages.get(i).attr("href").replace("//", "");
				if(secondaryImage != null) {
					if( !secondaryImage.equals(primaryImage) ) {
						secondaryImagesArray.put( secondaryImage );
					}
				}
			}
		}

		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private String crawlDescription(Document document) {
		String description = null;
		Elements elementsDescription = document.select("section#abas .tab-content div[role=tabpanel]:not([id=tab-avaliacoes-clientes])");
		description = elementsDescription.html();

		return description;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("ol.breadcrumb li a[title]:not([title=home]) span[itemprop=title]");

		for (int i = 0; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
			categories.add( elementCategories.get(i).text().trim() );
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
	 * There is no internalPid.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;
		return internalPid;
	}

	/**
	 * Auxiliar class to hold shared information between sku variations.
	 * 
	 * @author Samir Leao
	 *
	 */
	private class SharedData {

		public String baseName;
		public Map<String, Map<Integer, Float>> cardInstallmentsMap;

		public SharedData() {
			this.baseName = null;
			this.cardInstallmentsMap = null;
		}

	}

}


