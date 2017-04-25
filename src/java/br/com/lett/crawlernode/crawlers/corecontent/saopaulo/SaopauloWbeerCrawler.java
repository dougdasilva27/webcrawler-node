package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloWbeerCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.wbeer.com.br/";

	public SaopauloWbeerCrawler(Session session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			String id = this.session.getOriginalURL().split("/")[6];
			String internalID = Integer.toString(Integer.parseInt(id.replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));

			// Nome
			Element element_nome = doc.select("span[itemprop=\"name\"]").first();
			String name = element_nome.text().replace("'","").replace("’","").trim();

			// Preço
			Element element_preco = doc.select("div[itemprop=\"price\"]").last();
			if (element_preco==null) return products;
			Float price = Float.parseFloat(element_preco.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1 = null;
			String category2 = null; 
			String category3 = null;

			// Imagens
			Elements element_foto = doc.select("img[itemprop=\"image\"]");
			String primaryImage = element_foto.attr("src");
			primaryImage = "https://www.wbeer.com.br/" + primaryImage;
			String secondaryImages = null;

			// Descrição
			String description = "";
			Elements element_descricao = doc.select(".descricao");
			description = element_descricao.first().html().replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element element_nome = document.select("span[itemprop=\"name\"]").first();
		return element_nome != null;
	}
}