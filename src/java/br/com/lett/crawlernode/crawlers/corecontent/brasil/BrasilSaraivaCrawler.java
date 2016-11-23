package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class BrasilSaraivaCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.saraiva.com.br";
	private final String HOME_PAGE_HTTPS = "https://www.saraiva.com.br";
	
	private final int LARGER_IMAGE_DIMENSION = 550;


	public BrasilSaraivaCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// internalId
			String internalId = crawlInternalId(doc);

			// Pid
			String internalPid = internalId;

			// name
			String name = crawlName(doc);

			// availability
			boolean available = crawlAvailability(doc);

			// price
			Float price = crawlPrice(doc);

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select(".breadcrumbs.productView ol li [itemprop=title]");
			ArrayList<String> categories = new ArrayList<String>();
			for(Element e : elementCategories) {
				if( !e.text().equals("Início") ) {
					categories.add(e.text());
				}
			}
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// primaryImage
			String primaryImage = crawlPrimaryImage(doc);

			// secondaryImages
			String secondaryImages = crawlSecondaryImages(doc);

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".boxDetailCaracter").first();
			if(elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element elementProduct = document.select("section.product-allinfo").first();
		return elementProduct != null;
	}
	
	private String crawlInternalId(Document document) {
		String internalId = null;
		
		Element elementInternalId = document.select(".contentGeral .content .referenc").first();
		if(elementInternalId != null) {
			internalId = elementInternalId.text().substring(elementInternalId.text().indexOf(':') + 1).replace(")", "").trim();
		}
		
		return internalId;
	}
	
	private String crawlName(Document document) {
		String name = null;
		
		Element elementName = document.select("section.product-info h1").first();
		if(elementName != null) {
			name = elementName.ownText().trim();
		}
		
		return name;
	}
	
	private Float crawlPrice(Document document) {
		Float price = null;
		
		boolean skuIsAvailable = crawlAvailability(document);
		
		if(skuIsAvailable) {
			Element elementPrice = document.select(".price_complete_info .old_info div .finalPrice b").first();
			if(elementPrice != null) {
				price = MathCommonsMethods.parseFloat(elementPrice.text());
			}
		}
		
		return price;
	}
	
	private boolean crawlAvailability(Document document) {
		boolean available = true;
		Element elementAlertMe = document.select(".alert_me").first();
		if(elementAlertMe != null) {
			if( !elementAlertMe.attr("style").equals("") ) {
				if( elementAlertMe.attr("style").equals("display: block;") ) {
					available = false;
				}
			} else {
				String style = elementAlertMe.select("#alertme_container").first().attr("style");
				if(style.equals("display: block;")) {
					available = false;
				}
			}
		}
		
		return available;
	}
	
	/**
	 * Crawl an image with a default dimension of 430.
	 * There is a larger image with dimension of 550, but with javascript off
	 * this link disappear. So we modify the image URL and set the dimension parameter
	 * to the desired larger size.
	 * 
	 * Parameter to mody: &l
	 * 
	 * e.g:
	 * original: http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=430&a=-1
	 * larger: http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=550&a=-1
	 * 
	 * @param document
	 * @return
	 */
	private String crawlPrimaryImage(Document document) {
		String primaryImage = null;
		
		// get original image URL
		Element elementPrimaryImage = document.select("div.product-image-center a img").first();
		if(elementPrimaryImage != null) {
			primaryImage = elementPrimaryImage.attr("src");
		}
		
		// modify the dimension parameter
		String biggerPrimaryImage = CommonMethods.modifyParameter(primaryImage, "l", String.valueOf(LARGER_IMAGE_DIMENSION));
		
		return biggerPrimaryImage;
	}
	
	/**
	 * Get all the secondary images URL from thumbs container.
	 * Analogous treatment to that performed on primary image URL must be applied,
	 * so we can get the largest images URL.
	 *  
	 * @param document
	 * @return
	 */
	private String crawlSecondaryImages(Document document) {
		String secondaryImages = null;
		
		Elements elementImages = document.select("section.product-image div.thumbs-container-swiper div#thumbs-images a img");
		JSONArray secondaryImagesArray = new JSONArray();

		for(int i = 1; i < elementImages.size(); i++) { // skip the first because it's the same as the primary image
			String imageURL = elementImages.attr("src").trim();
			String biggerImageURL = CommonMethods.modifyParameter(imageURL, "l", String.valueOf(LARGER_IMAGE_DIMENSION));
			
			secondaryImagesArray.put(biggerImageURL);
		}			
		if (secondaryImagesArray.length() > 0) {
			secondaryImages = secondaryImagesArray.toString();
		}
		
		return secondaryImages;
	}
}
