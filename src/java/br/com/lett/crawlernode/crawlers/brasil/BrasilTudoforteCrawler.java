package br.com.lett.crawlernode.crawlers.brasil;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.CrawlerSessionError;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


public class BrasilTudoforteCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.tudoforte.com.br";

	public BrasilTudoforteCrawler(CrawlerSession session) {
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
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			Element elementProduct = doc.select(".product-actions").first();
			String internalID = null;
			String actionFormComprar = elementProduct.select("#form_comprar").first().attr("action");
			
			List<NameValuePair> params = null;
			try {
				params = URLEncodedUtils.parse(new URI(actionFormComprar), "UTF-8");
			} catch (URISyntaxException e) {
				CrawlerSessionError error = new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTraceString(e));
				session.registerError(error);
			}
			
			for (NameValuePair param : params) {
				if(param.getName().equals("product_id")) {
					internalID = param.getValue(); 
					break;
				}
			}		

			// Nome
			Element elementName = doc.select("h2.product-title").first();
			String name = elementName.text().replace("'","").replace("’","").trim();

			// Preco
			Float price = null;
			Element elementPrice = elementProduct.select("#info_preco .precoAvista").first();
			if(elementPrice == null) {
				price = null;
			} 
			else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = elementProduct.select("#button-buy").first();
			if(elementBuyButton == null) {
				available = false;
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Elements elementCategories = doc.select("div.breadcrumb-wrapper .breadcrumb .breadcrumb-item [title]");

			for(int i = 0; i < elementCategories.size(); i++) {
				if(category1.isEmpty()) {
					category1 = elementCategories.get(i).text();
				} 
				else if(category2.isEmpty()) {
					category2 = elementCategories.get(i).text();
				} 
				else if(category3.isEmpty()) {
					category3 = elementCategories.get(i).text();
				}
			}

			// Imagens
			Elements elementImages = doc.select(".product-images .product-images-thumbs li img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e : elementImages) {
				if(primaryImage == null) {
					primaryImage = e.attr("src").replace("/90_", "/");
				} 
				else {
					secondaryImagesArray.put(e.attr("src").replace("/90_", "/"));
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Description
			String description = "";   
			Element elementDescription = doc.select("div.description .tab-content").first();
			if(elementDescription != null) description = elementDescription.html();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
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

	private boolean isProductPage(String url, Document doc) {
		Element elementProduct = doc.select(".product-actions").first();
		return elementProduct != null;
		
	}
}
