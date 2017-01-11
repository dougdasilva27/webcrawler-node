package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

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

public class BelohorizonteVipfacilCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://vipfacil.com.br/";

	public BelohorizonteVipfacilCrawler(Session session) {
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

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			String internalId = Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("/")[5]));

			// Nome
			Elements elementName = doc.select("span.italico16.red.bold");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("span.italico20.red.bold");
			Float price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Categorias
			String category1;
			String category2;
			String category3;
			Elements element_categories = doc.select("div.top-nome a");
			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			int j = 0;

			for (int i = 0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = null;

			// Imagem primária
			Elements elementPrimaryImage = doc.select("div.foto img");
			String primaryImage = "http://vipfacil.com.br/" + elementPrimaryImage.get(0).attr("src").trim();
			if (primaryImage.contains("produto_sem_foto"))
				primaryImage = "";

			// Imagem seundária
			String secondaryImages = null;

			// Disponibilidade
			boolean available = true;

			// Descrição
			Elements element_descricao = doc.select("span.normal-11.cinza");
			String description = element_descricao.html();

			// Marketplace
			JSONArray marketplace = null;

			// Estoque
			Integer stock = null;

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/produtos/detalhe");
	}
}