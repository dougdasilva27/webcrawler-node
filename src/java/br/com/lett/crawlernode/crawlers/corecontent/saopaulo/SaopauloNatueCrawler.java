package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;

public class SaopauloNatueCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.natue.com.br/";

	public SaopauloNatueCrawler(Session session) {
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
		List<Product> products = new ArrayList<>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			Map<String, String> paramsMap = new HashMap<String, String>();

			// TODO: Popular paramsMap!! Dica: Olhar Araujo, Emporium...

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(paramsMap.get("pid")));

			// Nome
			Elements element_nome = doc.select("td.titproduto");
			String name = element_nome.text().replace("'", "").trim();

			// Preço
			Elements element_preco = doc.select("td.precoGR");
			Float price = Float.parseFloat(element_preco.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_cat1 = doc.select("div table tr td table tr td table tr td div span"); //TODO: Este selector está muito frágil
			String category1 = element_cat1.text().trim();

			String category2 = "";
			if(paramsMap.containsKey("gondola")) {
				category2 = paramsMap.get("gondola");
				category2 = URLDecoder.decode(category2).replace("+", " ");
			}

			String category3 = "";

			// Imagens
			Elements element_foto = doc.select("td.tdtfoto img");
			String primaryImage = element_foto.get(0).attr("src").trim();
			if(primaryImage != null && !primaryImage.equals("")) primaryImage = "http://www.emporiumsaopaulo.com.br/" + primaryImage;
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";
			String secondaryImages = null;

			// Descrição
			String description = "";

			// Estoque
			Integer stock = null;

			// Marketplace
			Marketplace marketplace = new Marketplace();

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

	private boolean isProductPage(String url) {
		return (url.contains("/produto.asp") && url.contains("pid="));
	}
}