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

public class SaopauloDrogasilCrawler extends Crawler {
	
	public SaopauloDrogasilCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches();
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			Element elementInternalID = doc.select(".product-details .col-2 .data-table tr .data").first();
			String internalID = null;
			if(elementInternalID != null) {
				internalID = elementInternalID.text();
			}

			// Pid
			String internalPid = internalID;

			// Disponibilidade
			boolean available = true;
			Element elementNotAvailable = doc.select(".product-shop .alert-stock.link-stock-alert a").first();
			if (elementNotAvailable != null) {
				if(elementNotAvailable.attr("title").equals("Avise-me")) {
					available = false;
				}
			}

			// Nome
			Element elementName = doc.select(".product-view .limit.columns .col-1 .product-info .product-name h1").first();
			Element elementBrand = doc.select(".product-view .limit.columns .col-1 .product-info .product-attributes ul .marca").first();
			String name = "";
			if(elementName != null) {
				name = elementName.text().trim();
			}
			if(elementBrand != null) { // adicionar a marca do produto também
				name = name + " " + elementBrand.text().trim();
			}

			// Preço
			Float price = null;
			Element elementSpecialPrice = doc.select(".product-shop .price-info .price-box .special-price").first();
			Element elementPrice = doc.select(".product-shop .price-info .price-box .price").first();
			if(elementSpecialPrice != null) { // está em promoção
				price = Float.parseFloat(elementSpecialPrice.select(".price").text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
			else if(elementPrice != null) { // preço normal sem promoção
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			Elements elementCategories = doc.select(".breadcrumbs ul li");
			ArrayList<String> categories = new ArrayList<String>();
			if(elementCategories.size() > 0) {
				for(int i = 1; i < elementCategories.size() - 1; i++) { // o primeiro e o último elemento estão sendo excluídos porque não são categoria
					Element elementCategorieTmp = elementCategories.get(i).select("a").first();
					if(elementCategorieTmp != null) {
						categories.add(elementCategorieTmp.text().trim());
					}
				}
			}
			String category1 = null; 
			String category2 = null; 
			String category3 = null;
			if(categories.size() > 0) {
				if(categories.size() == 1) {
					category1 = categories.get(0);
				}
				else if(categories.size() == 2) {
					category1 = categories.get(0);
					category2 = categories.get(1);
				}
				else if(categories.size() == 3) {
					category1 = categories.get(0);
					category2 = categories.get(1);
					category3 = categories.get(2);
				}
			}

			// Imagem primária
			Elements elementImages = doc.select(".product-img-box .product-image.product-image-zoom .product-image-gallery img");
			String primaryImage = null;
			Element elementPrimaryImage = elementImages.first();
			if(elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("data-zoom-image");
			}

			// Imagens Secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			if(elementImages.size() > 2) {
				for(int i = 2; i < elementImages.size(); i++) { // a primeira imagem das secundárias é a primária e a segunda do elements é a exibida
					Element elementSecondaryImage = elementImages.get(i);
					if(elementSecondaryImage != null) {
						String secondaryImage = elementSecondaryImage.attr("data-zoom-image");
						secondaryImagesArray.put(secondaryImage);
					}
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("div#details.product-details").first();
			if (elementDescription != null) {
				description = elementDescription.html().trim();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
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


		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
		}

		return products;
	} 
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Elements elementProductShop = document.select(".product-shop");
		Elements elementShippingQuote = document.select(".shipping-quote");
		return (elementProductShop.size() > 0 || elementShippingQuote.size() > 0);
	}

}
