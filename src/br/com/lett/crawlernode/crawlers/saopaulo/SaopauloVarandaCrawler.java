package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloVarandaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.varanda.com.br/";

	public SaopauloVarandaCrawler(CrawlerSession session) {
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

		// Tentando selecionar nome do produto para descobrir se a página que 
		// estamos visitando é uma página de produto.

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

			// Id interno
			Elements element_id = doc.select("input[name=product]");
			String internalID = Integer.toString(Integer.parseInt(element_id.first().val()));

			// Nome
			Elements element_nome = doc.select("div.product-name h1");
			String name = element_nome.text().replace("'", "").trim();

			// Preço
			Elements element_preco = doc.select("div.price-box .price");
			Float price = Float.parseFloat(element_preco.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_cats = doc.select("div.breadcrumbs ul li");
			String category1 = "";
			String category2 = "";
			String category3 = "";

			for(Element e: element_cats) {
				if(e.attr("class").startsWith("category")) {
					if(category1.isEmpty()) {
						category1 = e.text();
					} else if(category2.isEmpty()) {
						category2 = e.text();
					} else if(category3.isEmpty()) {
						category3 = e.text();
					}
				}
			}

			// Imagens
			Elements element_foto = doc.select("div.product-img-box a.prozoom-image");
			String primaryImage = element_foto.get(0).attr("href");
			if(primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) primaryImage = ""; //TODO: Verificar o nome da foto genérica
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

	private boolean isProductPage(Document document) {
		Elements element_nome = document.select("div.product-name h1");
		return element_nome.size() > 0;
	}
}