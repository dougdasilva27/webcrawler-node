package br.com.lett.crawlernode.crawlers.brasil;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;


public class BrasilBemolCrawler extends Crawler {

	private final String HOME_PAGE_HTTP = "http://www.bemol.com.br/loja/";
	private final String HOME_PAGE_HTTPS = "http://www.bemol.com.br/loja/";

	public BrasilBemolCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// InternalId
			String internalID = null;
			Element elementInternalID = doc.select("input.txtHiddenCantentryId").first();
			if (elementInternalID != null && elementInternalID.val() != null && !elementInternalID.val().isEmpty()) {
				internalID = elementInternalID.val().trim();
			}

			// Pid
			String internalPid = internalID;

			// Name
			Element elementName = doc.select(".product-title-content h1").first();
			String name = elementName.text().replace("’", "").trim();

			// Preço
			Float price = null;

			Element elementPrice = doc.select(".product-title-content .price-blue").first();

			if (elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";

			Elements elementCategories = doc.select(".breadcrumb_links").select("a");
			elementCategories.remove(0); // Tirando 'Home'

			for (Element e : elementCategories) {
				if (category1.isEmpty()) {
					category1 = e.text();
				} else if (category2.isEmpty()) {
					category2 = e.text();
				} else if (category3.isEmpty()) {
					category3 = e.text();
				}
			}

			// Imagem primária
			Elements elementImages = doc.select("#galleria").select("img");

			String primaryImage = null;

			// Imagens secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for (Element e : elementImages) {

				// Tirando o 87x87 para pegar imagem original
				if (primaryImage == null) {
					primaryImage = "http://www.bemol.com.br" + e.attr("src");
				} else {
					secondaryImagesArray.put("http://www.bemol.com.br" + e.attr("src"));
				}

			}

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select(".product_descs").first();
			if (elementDescription != null)
				description = elementDescription.html().trim();

			// Disponibilidade
			Element elementNotAvailable = doc.select("#divProdutoIndisponivel").first();
			boolean available = true;
			if (!elementNotAvailable.attr("style").contains("display:none")) {
				available = false;
			}

			// Estoque
			Integer stock = null;

			// Marketplace
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

	private boolean isProductPage(Document doc) {
		return (doc.select("#product").first() != null);
	}

}
