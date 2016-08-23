package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

/************************************************************************************************************************************************************************************
 * Crawling notes (05/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. On pages with multiple variations of sku
 * we get the url for each sku inside the element that selects the sku variation.
 * 
 * 2) During images crawling, we must preppend the seller domain to the image src attribute, to complete the URL.
 * 
 * 3) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 4) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 6) The sku page identification is done simply looking the URL format.
 * 
 * 7) When a product is unavailable, its price is not shown. But the crawler doesn't consider this
 * as a global rule. It tries to crawl the price the same way in both cases.
 * 
 * 8) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 9) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (multiple variations): http://www.webcontinental.com.br/produto/cervejeira-consul-mais-82l-titanium-110v-czd12at-17088
 * ex2 (no variations): http://www.webcontinental.com.br/produto/forno-a-gas-de-embutir-brastemp-clean-77l-inox-220v-boa61arrna-14982
 * ex3 (no variations, unavailable): http://www.webcontinental.com.br/produto/ar-condicionado-lg-split-libero-artcool-inverter-22000-btus-quente-frio-espelhado-220v-as-w242crz1-15357 
 * ex4 (variations, one is unavailable): http://www.webcontinental.com.br/produto/lavadora-de-roupas-automatica-electrolux-turbo-economia-13kg-branca-220v-ltd13-16784
 *
 * Optimizations notes:
 * 
 * 1) When crawling multiple variations, the crawler tries to identify if the current sku is the default selected 
 * when the first fetching was done, and if its URL is already parsed in the document passed to the extractInformation method.
 * If it's the case, then one fetching per product with variations can be spared.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilWebcontinentalCrawler extends Crawler {

	private final String SELLER_URL = "http://www.webcontinental.com.br/";
	
	private final String HOME_PAGE = "http://www.webcontinental.com.br/";

	public BrasilWebcontinentalCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			/* **************************************
			 * crawling data of multiple variations *
			 ****************************************/
			if ( hasSkuVariations(doc) ) {

				Logging.printLogDebug(logger, session, "Crawling multiple variations of a product...");

				Elements skus = crawlSkuOptions(doc);

				for (Element sku : skus) {

					// get sku url
					String skuURL = sku.attr("data-urlproduct").trim();

					// crawl sku
					if ( !isLoaded(sku, doc) ) {
						Logging.printLogDebug(logger, session, "The current sku page is not loaded. Will fetch its page...");
						
						Document skuDocument = fetchSkuPage(sku);
						products.add(crawlSku(skuURL, this.session.getSeedId(), skuDocument));
					} else {
						Logging.printLogDebug(logger, session, "The current sku page is already loaded by page fetcher. Will not fetch its page again...");
						products.add(crawlSku(skuURL, this.session.getSeedId(), doc));
					}										
				}

			}

			/* *******************************************
			 * crawling data of only one product in page *
			 *********************************************/
			else {
				Logging.printLogDebug(logger, session, "Crawling only one product...");
				
				// crawl sku data
				products.add(crawlSku(this.session.getUrl(), this.session.getSeedId(), doc));
			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	



	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		if ( url.startsWith("http://www.webcontinental.com.br/produto/") ) return true;
		return false;
	}


	/************************************
	 * Multiple products identification *
	 ************************************/

	private boolean hasSkuVariations(Document document) {
		Elements skus = document.select(".box-variation ul li input");

		if (skus.size() > 1) return true;
		return false;
	}


	/*********************************
	 * Multiple product page methods *
	 *********************************/

	private Elements crawlSkuOptions(Document document) {
		return document.select(".box-variation ul li input");
	}

	private Document fetchSkuPage(Element sku) {
		String skuURL = sku.attr("data-urlproduct").trim();
		Document skuDocument = Jsoup.parse( DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, skuURL, null, null) );

		return skuDocument;
	}
	
	private boolean isLoaded(Element sku, Document document) {
		Element currentLoadedOption = document.select(".box-variation input[type=hidden]").first();
		if (currentLoadedOption != null) {
			String currentLoadedId = currentLoadedOption.attr("data-idcurrent");
			String skuId = sku.attr("value");
			
			if (currentLoadedId != null && skuId != null) {
				if (currentLoadedId.equals(skuId)) return true;
				return false;
			}
		}
		
		return false;
	}


	/*******************
	 * General methods *
	 *******************/

	private Product crawlSku(String url, String seedId, Document document) {

		// InternalId
		String internalId = crawlInternalId(document);

		// InternalPid
		String internalPid = crawlInternalPid(document);

		// Name
		String name = crawlName(document);

		// Price
		Float price = crawlPrice(document);

		// Disponibilidade
		boolean available = crawlAvailability(document);

		// Categories
		ArrayList<String> categories = crawlCategories(document);
		String category1 = getCategory(categories, 0);
		String category2 = getCategory(categories, 1);
		String category3 = getCategory(categories, 2);

		// Primary image
		String primaryImage = crawlPrimaryImage(document);

		// Secondary images
		String secondaryImages = crawlSecondaryImages(document);

		// Description
		String description = crawlDescription(document);

		// Estoque
		Integer stock = null;

		// Marketplace
		JSONArray marketplace = null;

		Product product = new Product();
		product.setSeedId(seedId);
		product.setUrl(url);
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

	private String crawlInternalId(Document document) {
		String internalId = null;
		Element internalIdElement = document.select(".ajaxPr").first();

		if (internalIdElement != null) {
			internalId = internalIdElement.attr("data-prid").trim();
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select(".destaqueProduto .produto-nome").first();

		if (nameElement != null) {
			name = nameElement.text().replace("'", "").replace("’", "").trim();
		}

		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Element priceElement = document.select(".box-preco-produto .valor-por .valor-avista").first();

		if (priceElement != null) {
			price = Float.parseFloat(priceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		Element notifyMeElement = document.select(".msg-produto-indisponivel").first();

		if (notifyMeElement != null) return false;
		return true;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements categorieElements = document.select("#migalha div a span");

		for (int i = 1; i < categorieElements.size(); i++) { // skiping the first element because its a link to home page
			categories.add( categorieElements.get(i).text().trim() );
		}		

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlDescription(Document document) {
		String description = "";
		Element descriptionSpecElement = document.select("#descricao-produtos .box-desc-produto.descricao-aba section#descricao .box-interno").first();

		if (descriptionSpecElement != null) {
			description = descriptionSpecElement.html();
		}

		return description;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element primaryImageElement = document.select(".imagemDestaque a").first();

		if (primaryImageElement != null) {
			primaryImage = SELLER_URL + primaryImageElement.attr("href").trim();
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();

		Elements imagesElement = document.select("ul#carrossel li img");
		for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is the primary image
			String image = SELLER_URL + imagesElement.get(i).attr("src").trim();
			image = image.replaceAll("/false/", "/true/");
			secondaryImagesArray.put(image);
		}

		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

}
