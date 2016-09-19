package br.com.lett.crawlernode.crawlers.belohorizonte;



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

public class BelohorizonteSantahelenaCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.santahelenacenter.com.br/principal/";

	public BelohorizonteSantahelenaCrawler(CrawlerSession session) {
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

			// Id interno
			String id = this.session.getUrl().split("/")[7];
			String internalId = Integer.toString(Integer.parseInt(id));

			// Nome
			Elements elementName = doc.select("#titulo-ofertas strong");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select(".preco");
			Float price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			String category1;
			String category2;
			String category3;
			category1 = null;
			category2 = null;
			category3 = null;

			// Imagem primária
			Element elementPrimaryImage = doc.select(".produto-detalhe-img").first();
			String primaryImage = null;
			if (elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src").trim();
			}
			if (primaryImage.contains("produto_sem_foto"))
				primaryImage = "";

			// Imagens secundárias
			String secondaryImages = null;

			// Descrição
			Elements elementDescription = doc.select(".thumb-pruduto-detalhes-titulo-nome");
			String description = elementDescription.html().replace("'", "\"").trim();

			// Marketplace
			JSONArray marketplace = null;

			// Disponibilidade
			boolean available = true;

			// Estoque
			Integer stock = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setSeedId(this.session.getSeedId());
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

	private boolean isProductPage(String url) {
		return url.startsWith("https://www.santahelenacenter.com.br/principal/index.php/produto");
	}
}