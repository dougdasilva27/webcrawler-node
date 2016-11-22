package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class BrasilRogeCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.roge.com.br/";

	public BrasilRogeCrawler(Session session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// InternalId
			String internalId = crawlInternalId(doc);

			// internalPid
			String internalPid = crawlInternalPid(doc);

			// availability
			boolean available = crawlAvailability(doc);

			// name
			String name = crawlName(doc);

			// price
			Float price = crawlPrice(doc);

			// prices
			Prices prices = crawlPrices(doc);

			// categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// description
			String description = crawlDescription(doc);

			// stock
			Integer stock = null;

			// marketplace
			JSONArray marketplace = new JSONArray();

			Product product = new Product();
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
			product.setAvailable(available);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}

	private boolean isProductPage(String originalURL) {
		return (originalURL.startsWith("https://www.roge.com.br/Produtos/ProdutosDetalhe"));
	}

	private String crawlInternalPid(Document doc) {
		return null;
	}

	private Prices crawlPrices(Document doc) {
		return new Prices();
	}

	private String crawlDescription(Document doc) {
		StringBuilder description = new StringBuilder();
		
		Element skuInformation = doc.select("div.informacoes-produto").first();
		if (skuInformation != null) {
			description.append(skuInformation.html());
		}
		
		return description.toString();
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		
		Elements secondaryImagesElements = doc.select("#thumblist li a");
		for (int i = 1; i < secondaryImagesElements.size(); i++) { // the first is the same as the primary image
			String relAttr = secondaryImagesElements.get(i).attr("rel").trim();
			
			int beginIndex = relAttr.indexOf("largeimage:");
			String largeImageSubstring = relAttr.substring(beginIndex, relAttr.length());
			
			int srcIndex = largeImageSubstring.indexOf("src=") + 4; // remove the 'src='
			int endIndex = largeImageSubstring.indexOf(".jpg"); // must append the extension on the final url
			
			String imageURL = largeImageSubstring.substring(srcIndex, endIndex).replaceAll("'", "") + ".jpg";
			
			secondaryImagesArray.put(imageURL);
		}
		
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}

	private String crawlPrimaryImage(Document doc) {
		String primaryImage = null;
		Element primaryImageElement = doc.select(".detalhe-foto .foto-grande a").first();
		if (primaryImageElement != null) {
			primaryImage = primaryImageElement.attr("href").trim();
		}
		return primaryImage;
	}

	private ArrayList<String> crawlCategories(Document doc) {
		ArrayList<String> categories = new ArrayList<String>();		
		return categories;
	}

	private Float crawlPrice(Document doc) {
		return null;
	}

	private String crawlName(Document doc) {
		String name = null;
		Element nameElement = doc.select("#lblProduto").first();
		if (nameElement != null) {
			String nameText = nameElement.text().trim();
			int endIndex = nameText.indexOf("(");
			name = nameText.substring(0, endIndex).trim();
		}
		return name;
	}

	private boolean crawlAvailability(Document doc) {
		return false;
	}

	private String crawlInternalId(Document doc) {
		String internalId = null;
		Element internalIdElement = doc.select("#hCodProduto").first();
		if (internalIdElement != null) {
			internalId = internalIdElement.attr("value").trim();
		}
		return internalId;
	}
	
	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

}
