package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloExtraCrawler extends Crawler {

	public SaopauloExtraCrawler(CrawlerSession session) {
		super(session);
	}
	
	@Override
	public void handleCookiesBeforeFetch() {
	    
		// Criando cookie da loja 21 = São Paulo capital
	    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", "21");
	    cookie.setDomain(".deliveryextra.com.br");
	    cookie.setPath("/");
	    cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
	    this.cookies.add(cookie);
	    
		// Criando cookie simulando um usuário logado
	    BasicClientCookie cookie2 = new BasicClientCookie("ep.customer_logged", "-2143598207");
	    cookie2.setDomain(".deliveryextra.com.br");
	    cookie2.setPath("/");
	    cookie2.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));
	    this.cookies.add(cookie2);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.deliveryextra.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if(session.getUrl().contains("/produto/")) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// internal id
			String internalId = crawlInternalId(doc);

			// internal pid
			String internalPid = internalId;

			// name
			String name = crawlName(doc);

			// price
			Float price = crawlPrice(doc);

			// availability
			boolean available = crawlAvailability(doc);

			// categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			Elements elementSecondaryImages = doc.select(".more-views dl.thumb-list dd a");
			JSONArray secondaryImagesArray = new JSONArray();
			String secondaryImages = "";
			if(elementSecondaryImages.size() > 1) {
				for(int i = 1; i < elementSecondaryImages.size(); i++) { // primeira imagem é a primária
					String tmp = elementSecondaryImages.attr("href");
					if(tmp.equals("#")) {
						Elements imgElement = elementSecondaryImages.select("img");
						if(imgElement.size() > 0) {
							tmp = "http://www.deliveryextra.com.br" + imgElement.attr("src");
							secondaryImagesArray.put(tmp);
						}
					}
				}
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// description
			String description = "";   
			Element element_moreinfo = doc.select("#more-info").first();
			if(element_moreinfo != null) {
				description = description + element_moreinfo.html();
			}

			// stock
			Integer stock = null;

			// marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getUrl());
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

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}

	private String crawlInternalId(Document document) {
		return Integer.toString(Integer.parseInt(session.getUrl().split("/")[4]));
	}

	private String crawlName(Document document) {
		String name = null;
		Element elementName = document.select("article.hproduct h1").first();
		if (elementName != null) {
			name = elementName.text().replace("'", "").trim();
		}
		return name;
	}

	private Float crawlPrice(Document document) {
		Float price = null;
		Elements elementPrice = document.select("article.hproduct > .hproductLeft .price-off");
		if(elementPrice.size() == 0) {
			elementPrice = document.select(".price-detail.sale-detail > .for > .sale-price");
			if (elementPrice.size() == 0) {
				elementPrice = document.select(".box-price .progressiveDiscount-baseValue");
			}
		}
		if (elementPrice.last() != null) {
			price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}

		return price;
	}

	private boolean crawlAvailability(Document document) {
		boolean available = false;
		Element elementAvailable = document.select(".btnComprarProd.productElement").first();
		available = (elementAvailable != null);

		return available;
	}

	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();

		Elements elementsCategory1 = document.select("article.hproduct h2");
		if (elementsCategory1.size() > 1) { // prevent crawling wrong category string when product is unavailable
			Element elementCategory1 = elementsCategory1.first();
			categories.add(elementCategory1.text().trim());
		}

		Elements elementsCategory2 = document.select("article.hproduct h3");
		if (elementsCategory2.size() > 1) { // prevent crawling wrong category string when product is unavailable
			Element elementCategory2 = elementsCategory2.first();
			categories.add(elementCategory2.text().trim());
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		Element elementPrimaryImage = document.select(".box-image .image img").first();
		if (elementPrimaryImage != null) {
			primaryImage = "http://www.deliveryextra.com.br" + elementPrimaryImage.attr("src");
		}
		return primaryImage;
	}

}