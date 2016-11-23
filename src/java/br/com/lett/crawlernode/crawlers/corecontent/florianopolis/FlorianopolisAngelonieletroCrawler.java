package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class FlorianopolisAngelonieletroCrawler extends Crawler {

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

					// if the element is loaded we crawl it and append variation on the name
					if (isLoaded(variationDocument, option)) {

						// if the sku is already loaded we crawl it normally and append
						// the variation on the sku name
						Product product = crawlProduct(variationDocument);
						
						String appendedName = product.getName() + " " + option.getAttribute("id") + "v";
						product.setName(appendedName);
						
						products.add(product);

					} 
					
					// click on the option and crawl the variation
					else {

						// click
						Logging.printLogDebug(logger, session, "Clicking on option...");
						this.webdriver.clickOnElementViaJavascript(option);

						// give some time for safety
						Logging.printLogDebug(logger, session, "Waiting 2 seconds...");
						this.webdriver.waitLoad(2000);
						
						// get the new html and parse
						String html = this.webdriver.findElementByCssSelector("html").getAttribute("innerHTML");
						variationDocument = Jsoup.parse(html);

						// crawl sku
						Product product = crawlProduct(variationDocument);
						products.add(product);
					}					
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
		Float price = crawlPrice(document);
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
	 * The same as the internal id.
	 * 
	 * @param document
	 * @return
	 */
	private String crawlInternalPid(Document document) {
		String internalPid = null;

		Element elementInternalId = document.select(".codigo span[itemprop=sku]").first();
		if(elementInternalId != null) {
			internalPid = elementInternalId.text().trim();
		}

		return internalPid;
	}

}
