package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SaopauloTudosaudavelCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.tudosaudavel.com/";

	public SaopauloTudosaudavelCrawler(Session session) {
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

			// Id interno
			Element element_id = doc.select(".section > div").last();
			String internalID = Integer.toString(Integer.parseInt(element_id.attr("id").trim().replaceAll("[^0-9,]+", "")));

			// Nome
			Elements element_nome = doc.select("h2[itemprop=\"name\"]");
			String name = element_nome.text().replace("'", "").trim();

			// Preço
			Elements element_preco = doc.select("meta[itemprop=\"price\"]");
			Float price = Float.parseFloat(element_preco.first().attr("content").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_categories = doc.select(".posted_in a"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[6];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			cat[4] = "";
			cat[5] = "";

			int j=0;
			for(int i=0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = cat[3];

			// Imagens
			Elements element_foto = doc.select("a[itemprop=\"image\"]");
			String primaryImage = element_foto.get(0).attr("href").trim();
			if(primaryImage != null && !primaryImage.equals("")) primaryImage = "http:" + primaryImage;
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";
			String secondaryImages = null;

			// Descrição
			String description = "";  
			Element element_descricao = doc.select("#tab-description ul").first();
			if(element_descricao != null)	description = description + element_descricao.html().replace("'","").replace("’","").trim();

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
		return url.startsWith("http://www.tudosaudavel.com/produto/");
	}
}