package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

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

public class FortalezaSaoluizCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.saoluizdelivery.com.br/";

	public FortalezaSaoluizCrawler(Session session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			Elements element_visit = doc.select(".form_buy nobr");
			if(element_visit == null ) return products;

			//				Elements element_id = doc.select(".lstReference dd");
			//				int idInterno = Integer.parseInt( element_id.last().text().toString().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			String internalID = "1";

			// Nome
			Elements elementName = doc.select(".titProduto");
			String name = elementName.text().trim();

			// Preço
			Elements elementPrice = doc.select("div#spanPrecoPor strong");
			Float price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements element_categories = doc.select("#breadcrumbs li"); 
			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			int j=0;

			for(int i=0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			if(element_categories.size()==4){
				category1 = cat[1];
				category2 = cat[2];
				category3 = null;
			} 
			else {
				category1 = cat[1];
				category2 = null;
				category3 = null;
			}

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select(".big_photo_container a");
			primaryImage = element_foto.first().attr("href").trim();	
			if(primaryImage.contains("indisponivel.gif")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			Elements element_descricao = doc.select("#box_caracteristicas");
			String description = element_descricao.html().trim();

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

	private boolean isProductPage(Document document) {
		Element span = document.select(".lstReference").first();
		return (span != null);
	}

}