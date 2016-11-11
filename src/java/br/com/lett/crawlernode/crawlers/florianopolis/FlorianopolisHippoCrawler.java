package br.com.lett.crawlernode.crawlers.florianopolis;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class FlorianopolisHippoCrawler extends Crawler {

	public FlorianopolisHippoCrawler(Session session) {
		super(session);
	}

	private final String HOME_PAGE = "http://www.hippo.com.br/";

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();            
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("/")[4]));

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".infos .nome .cod").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().split("\\.")[1].trim();
			}

			// Nome
			String name = null;
			Element elementName = doc.select("p.nome").first();
			if (elementName != null) {
				name = elementName.text().replace("'", "").trim();
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select("div.preco_comprar div.valores .valor").first();
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			}

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_cat1 = doc.select(".breadcrumb a");
			String category1 = (element_cat1.size() >= 3) ? element_cat1.get(2).text().trim() : "";

			Elements element_cat2 = doc.select(".breadcrumb a");
			String category2 = (element_cat2.size() >= 4) ? element_cat2.get(3).text().trim() : "";

			Elements element_cat3 = doc.select(".breadcrumb a");
			String category3 = (element_cat3.size() >= 5) ? element_cat3.get(4).text().trim() : "";

			// Imagens
			Elements element_foto = doc.select("div.img a");
			String primaryImage = (element_foto.size() > 0) ? element_foto.get(0).attr("href") : "";
			primaryImage = primaryImage.replace("/./", "http://www.hippo.com.br/");
			if(primaryImage.contains("produto_default_grande.jpg")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = "";
			Element elementDescription = doc.select("div.accordion--body").first();
			if (elementDescription != null) {
				description = description + elementDescription.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		return url.contains("/produto/") && doc.select("p.nome").first() != null;
	}
}