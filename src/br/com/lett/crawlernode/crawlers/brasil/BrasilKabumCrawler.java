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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class BrasilKabumCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.kabum.com.br";

	public BrasilKabumCrawler(CrawlerSession session) {
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

			// internalId
			String internalID = null;
			Element elementInternalID = doc.select(".boxs .links_det").first();
			if(elementInternalID != null) {
				String text = elementInternalID.ownText();
				internalID = text.substring(text.indexOf(':')+1).trim();
			}

			Element elementProduct = doc.select("#pag-detalhes").first();

			// internalPid
			String internalPid = null;

			// name
			String name = null;
			Element elementName = null;
			if (elementProduct != null) {
				elementName = elementProduct.select("#titulo_det h1").first();
			}
			if (elementName != null) {
				name = elementName.text().replace("'","").replace("’","").trim();
			}

			// price
			Float price = null;
			Element elementPrice = elementProduct.select(".box_preco .preco_desconto span").first();
			if(elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// availability
			boolean available = true;
			Element elementAvailability = elementProduct.select(".disponibilidade img").first();			
			if(elementAvailability == null || !elementAvailability.attr("alt").equals("produto_disponivel")) {
				available = false;
			}

			// categories
			String category1 = "";
			String category2 = "";
			String category3 = "";

			Elements elementCategories = elementProduct.select(".boxs .links_det a");
			for(Element e : elementCategories) {
				if(category1.isEmpty()) {
					category1 = e.text().replace(">", "").trim();
				} 
				else if(category2.isEmpty()) {
					category2 = e.text().replace(">", "").trim();
				} 
				else if(category3.isEmpty()) {
					category3 = e.text().replace(">", "").trim();
				}
			}

			// images
			Elements elementImages = elementProduct.select("#imagens-carrossel li img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			for(Element e : elementImages) {
				if(primaryImage == null) {
					primaryImage = e.attr("src").replace("_p.", "_g.");
				} else {
					secondaryImagesArray.put(e.attr("src").replace("_p.", "_g."));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// description
			String description = "";   
			Element elementDescription = doc.select(".tab_").first();
			if(elementDescription != null) description = elementDescription.html().replace("’","").trim();

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

	private boolean isProductPage(String url) {
		return (url.startsWith("https://www.kabum.com.br/produto/") || url.startsWith("http://www.kabum.com.br/produto/"));
	}
}
