package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilFarmadeliveryCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.farmadelivery.com.br/";

	public BrasilFarmadeliveryCrawler(CrawlerSession session) {
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

			// ID interno
			String internalId = null;
			Element elementID = doc.select("input[name=product]").first();
			if (elementID != null) {
				internalId = Integer.toString(Integer.parseInt(elementID.val()));
			} 

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".std .data-table tbody td").get(1);
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().trim();
			}

			// Nome
			Element elementName = doc.select(".product-name").first();
			String name = elementName.text().replace("'", "").replace("’","").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".price-box .regular-price .price").first();
			if(elementPrice == null) elementPrice = doc.select(".price-box .special-price .price").first();
			if (elementPrice != null) {
				 price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			Element button_unavailable = doc.select("a.btn-esgotado").first();
			if(button_unavailable != null) available = false;

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";
			ArrayList<String> categories = new ArrayList<String>();
			Elements elementCategories = doc.select(".breadcrumbs ul li");

			for(Element e : elementCategories) {
				if(!e.attr("class").equals("home") && !e.attr("class").equals("product")) {
					categories.add(e.select("a").text());
				}
			}

			for(String category : categories) {
				if(category1.isEmpty()) {
					category1 = category;
				} 
				else if(category2.isEmpty()) {
					category2 = category;
				} 
				else if(category3.isEmpty()) {
					category3 = category;
				}
			}

			// Imagens
			String primaryImage = null;
			Element elementPrimaryImage = doc.select(".product-img-box img#image").first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src").trim();
				primaryImage = primaryImage.replace("275x275/", "");
			}

			if(primaryImage != null) {
				if(primaryImage.contains("banner_produto_sem_imagem")) {
					primaryImage = "";
				}
			}

			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementSecondaryImages = doc.select("ul.more-views-list li a");

			if(elementSecondaryImages.size() > 1){
				for(int i = 1; i < elementSecondaryImages.size();i++){
					Element e = elementSecondaryImages.get(i);
					secondaryImagesArray.put(e.attr("href"));
				}

			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			Elements elementDescription = doc.select("div.product-collateral");
			String description = elementDescription.html();			

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
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

		} 
		else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Element elementProduct = doc.select("div.product-view").first();
		return (elementProduct != null && elementProduct != null && !url.contains("/review/"));
	}
}
