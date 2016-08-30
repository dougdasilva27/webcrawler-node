package br.com.lett.crawlernode.crawlers.fortaleza;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class FortalezaObomvizinhoCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.obomvizinho.com.br/";
	
	public FortalezaObomvizinhoCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

			// Id interno
			String id = (this.session.getUrl().split("/")[4]).replaceAll("[^0-9,]+", "");
			String internalID = Integer.toString(Integer.parseInt(id));

			// Nome
			Elements elementName = doc.select("h2");
			String name = elementName.first().text().trim();

			// Preço
			Elements elementPrice = doc.select(".preco");
			Float price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1 = null; 
			String category2 = null; 
			String category3 = null;

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select("#lightviewContent");
			if (element_foto == null ) {
				primaryImage = "";
			} 
			else {
				primaryImage = element_foto.attr("src").trim();	
			}

			String secondaryImages = null;

			// Descrição
			String description = "";

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(session.getSeedId());
			product.setUrl(session.getUrl());
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
			Logging.printLogTrace(logger, "Not a product page" + session.getSeedId());
		}

		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.obomvizinho.com.br/produto/");
	}

}