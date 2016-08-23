package br.com.lett.crawlernode.crawlers.brasil;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class BrasilDrogariapachecoCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.drogariaspacheco.com.br/";

	public BrasilDrogariapachecoCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// internalId
			Element elementID = doc.select("div.productReference").first();
			String internalID = Integer.toString(Integer.parseInt(elementID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", ""))).trim();

			// internalPid
			String internalPid = null;

			// name
			Elements elementName = doc.select(".buy_box .productName");
			String name = elementName.text().replace("'", "").replace("’", "").trim();

			// price
			Float price = null;
			Element elementPrice = doc.select("strong.skuPrice").first();
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// availability
			boolean available = true;
			Element buyButton = doc.select("a.buy-button").first();
			if (buyButton == null || buyButton.attr("style").replace(" ", "").contains("display:none")) {
				available = false;
			}

			// categories
			Elements elementCategories = doc.select(".bread-crumb li a");
			ArrayList<String> categories = new ArrayList<String>();
			String category1 = "";
			String category2 = "";
			String category3 = "";

			for (Element e : elementCategories) {
				if (!e.text().equals("drogariaspacheco")) {
					categories.add(e.text());
				}
			}

			for (String category : categories) {
				if (category1.isEmpty()) {
					category1 = category;
				} else if (category2.isEmpty()) {
					category2 = category;
				} else if (category3.isEmpty()) {
					category3 = category;
				}
			}

			// images
			String primaryImage = "";
			Elements elementPrimaryImage = doc.select("img#image-main");
			primaryImage = elementPrimaryImage.first().attr("src").trim();

			if (primaryImage.contains("indisponivel.gif")) {
				primaryImage = "";
			}

			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementsecondaryImages = doc.select("ul.thumbs li a");

			if (elementsecondaryImages.size() > 1) {
				for (int i = 1; i < elementsecondaryImages.size(); i++) {
					Element e = elementsecondaryImages.get(i);
					secondaryImagesArray.put(e.attr("rel"));
				}
			}
			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// description
			Elements elementDescription = doc.select("div.productDescription");
			String description = elementDescription.html();

			// stock
			Integer stock = null;

			// marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
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

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element body = document.select("body").first();
		Element elementID = document.select("div.productReference").first();
		return (body.hasClass("produto") && elementID != null);
	}
}