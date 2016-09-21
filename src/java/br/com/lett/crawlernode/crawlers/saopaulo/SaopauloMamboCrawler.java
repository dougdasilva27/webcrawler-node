package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;


public class SaopauloMamboCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.mambo.com.br/";

	public SaopauloMamboCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			String internalId = crawlInternalId(doc);

			// name
			String name = crawlName(doc);

			// price
			Float price = crawlPrice(doc);

			// availability
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

			// primary image
			String primaryImage = crawlPrimaryImage(doc);

			// secondary images
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = "";
			Elements element_descricao = doc.select(".productDescription");
			description = element_descricao.first().html().replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getUrl());
			product.setInternalId(internalId);
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
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
		}

		return primaryImage;
	}

	private String crawlSecondaryImages(Document doc) {
		String secondaryImages = null;
		JSONArray secondaryImagesArray = new JSONArray();
		Elements images = doc.select(".thumbs li a");


		for (int i = 1; i < images.size(); i++) {//starts with index 1, because the first image is the primary image

			String urlImage = null;
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
}