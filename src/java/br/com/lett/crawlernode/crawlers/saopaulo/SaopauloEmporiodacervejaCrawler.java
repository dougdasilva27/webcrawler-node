package br.com.lett.crawlernode.crawlers.saopaulo;

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

public class SaopauloEmporiodacervejaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.emporiodacerveja.com.br/";

	public SaopauloEmporiodacervejaCrawler(CrawlerSession session) {
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
			Element element_id = doc.select("span[itemprop=\"productID\"]").first();
			String internalID = Integer.toString(Integer.parseInt(element_id.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));

			// Nome
			Element element_nome = doc.select("h1[itemprop=\"name\"]").first();
			String name = element_nome.text().replace("'","").replace("’","").trim();

			// Preço
			Element element_preco = doc.select("div[itemprop=\"price\"]").last();
			if (element_preco==null) return products;
			Float price = Float.parseFloat(element_preco.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			category1 = "";
			category2 = "";
			category3 = "";

			// Imagens
			Element element_foto = doc.select(".fancyPhoto").first();
			String primaryImage = element_foto.attr("href");
			String secondaryImages = null;

			// Descrição
			String description = ""; 
			Elements element_descricao = doc.select(".clear.row.margin_b_30.3");
			description = element_descricao.first().html().replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
		}
		
		return products;
	} 
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.emporiodacerveja.com.br/cervejas/");
	}
}