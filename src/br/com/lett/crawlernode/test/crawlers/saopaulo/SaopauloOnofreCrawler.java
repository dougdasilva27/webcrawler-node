package br.com.lett.crawlernode.test.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.test.kernel.task.Crawler;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;


/************************************************************************************************************************************************************************************
 * Crawling notes - revision (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 *  
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the URL format.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku.
 * 
 * 7) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples:
 * ex1 (available with sku list html error): http://www.onofre.com.br/nan-soy-formula-infantil-com-ferro-para-lactentes-400g/25811/05
 *
 * Optimizations notes:
 * No optimizations.
 * 
 * Known website errors:
 * 1) Sometimes the selector for sku returns an empty array. This is an error of the page. The crawler looks fot his particular case.
 * It was observed on only one case so far.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloOnofreCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.onofre.com.br/";

	public SaopauloOnofreCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		Elements skuList = crawlSkuList(doc);

		if (skuList.size() > 0) {

			// looking for all products in page
			for (Element element : skuList) {

				// fetch current sku URL
				Document skuDoc = this.fetchSkuURL(element);

				if (skuDoc != null) {
					Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

					// InternalId
					String internalID = crawlInternalId(skuDoc);

					// Pid
					String internalPid = crawlInternalPid(skuDoc);

					// Name
					String name = crawlName(skuDoc);

					if(name != null) {

						// Price
						Float price = crawlMainPagePrice(skuDoc);

						// Availability
						boolean available = crawlAvailability(skuDoc);

						// Categories
						ArrayList<String> categories = crawlCategories(skuDoc); 
						String category1 = getCategory(categories, 0); 
						String category2 = getCategory(categories, 1); 
						String category3 = getCategory(categories, 2);

						// Primary image
						String primaryImage = crawlPrimaryImage(skuDoc);

						// Imagens secundárias
						String secondaryImages = crawlSecondaryImages(skuDoc, primaryImage);

						// Descrição
						String description = crawlDescription(skuDoc);

						// Estoque
						Integer stock = null;

						// Marketplace
						JSONArray marketplace = null;

						Product product = new Product();
						product.setSeedId(session.getSeedId());
						product.setUrl(session.getUrl());
						product.setInternalId(internalID);
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

						products.add(product);

					} 
				} else {
					Logging.printLogTrace(logger, "Not a product page" + session.getSeedId());
				}
			}
		}

		/* **********************************************************
		 * Particular case of html selector error from the website  *
		 * There is no element in the sku list for the current page *
		 ************************************************************/
		else {
			String internalID = crawlInternalId(doc);
			String internalPid = crawlInternalPid(doc);
			String name = crawlName(doc);

			if (name != null) {
				Float price = crawlMainPagePrice(doc);
				boolean available = crawlAvailability(doc);

				ArrayList<String> categories = crawlCategories(doc); 

				String category1 = getCategory(categories, 0); 
				String category2 = getCategory(categories, 1); 
				String category3 = getCategory(categories, 2);
				String primaryImage = crawlPrimaryImage(doc);
				String secondaryImages = crawlSecondaryImages(doc, primaryImage);
				String description = crawlDescription(doc);
				Integer stock = null;
				JSONArray marketplace = null;

				Product product = new Product();
				product.setSeedId(session.getSeedId());
				product.setUrl(session.getUrl());
				product.setInternalId(internalID);
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

				products.add(product);
			}
		}
		
		return products;
	}


	/*******************
	 * General methods *
	 *******************/

	private String crawlInternalId(Document document) {
		String internalId = null;

		Elements elementInternalID = document.select(".main-product-info .main-product-name input[type=hidden]");
		if(elementInternalID.size() > 0) {
			internalId = elementInternalID.first().attr("value");
		}

		return internalId;
	}

	private String crawlInternalPid(Document document) {
		String internalPid = null;
		Element elementInternalPid = document.select(".code.col #cphConteudo_lblCode").first();
		if (elementInternalPid != null) {
			internalPid = elementInternalPid.text().split(":")[1].trim();
		}
		return internalPid;
	}

	private String crawlName(Document document) {
		String name = null;
		Element nameElement = document.select("#lblProductName").first();

		if (nameElement != null) {
			name = nameElement.text().trim();
		}

		return name;
	}

	private Float crawlMainPagePrice(Document document) {
		Float price = null;
		Element elementPrice = document.select("#cphConteudo_lblPrecoPor").first();

		if(elementPrice != null) {
			price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = true;
		Elements elementAvailable = document.select(".product-spec .action-box .buy-area .unavailable");
		if(elementAvailable.first() != null) {
			available = false;
		}

		return available;
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage;
		Element elementPrimaryImage = document.select(".dp_box_produto .jqzoom").first();

		if(elementPrimaryImage == null){
			elementPrimaryImage = document.select("#cphConteudo_imgGrande").first();
			primaryImage = elementPrimaryImage.attr("src");
		} else {
			primaryImage = elementPrimaryImage.attr("href");
		}

		primaryImage = HOME_PAGE + primaryImage.replace("Produto/Normal", "Produto/Super");

		if(primaryImage.contains("imagem_prescricao")) { // only for meds
			primaryImage = "";
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document document, String primaryImage) {
		String secondaryImages = null;

		JSONArray secondaryImagesArray = new JSONArray();
		Elements elementSecondaryImages = document.select(".main-product-image .img-thumbs ul li img");

		if(elementSecondaryImages.size() > 1){
			for(int i = 0; i < elementSecondaryImages.size(); i++) {
				Element e = elementSecondaryImages.get(i);
				if(e.attr("src").replace("Produto/Normal", "Produto/Super").equals(primaryImage.replace("http://www.onofre.com.br/",""))) {

				}
				else{
					secondaryImagesArray.put("http://www.onofre.com.br/" + e.attr("src").replace("Produto/Normal", "Produto/Super"));
				}
			}

		}

		if(secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}

		return secondaryImages;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select("#breadcrumbs a");

		for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
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

	private String crawlDescription(Document document) {
		String description = "";
		Element elementDescription = document.select(".product-details").first(); 
		if(elementDescription != null) {
			description = description + elementDescription.html();
		}

		return description;
	}

	private Elements crawlSkuList(Document document) {
		return document.select(".sku-radio .sku-list li");
	}

	private Document fetchSkuURL(Element sku) {
		Element elementSkuURL = sku.select("a").first();
		String skuUrl = null;
		Document skuDoc = null;

		// get sku URL
		if (elementSkuURL != null) {
			skuUrl = "http://www.onofre.com.br" + elementSkuURL.attr("href").trim();
		}

		// load sku URL
		if (skuUrl != null) {
			skuDoc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, skuUrl, null, null);
		}

		return skuDoc;
	}
} 
