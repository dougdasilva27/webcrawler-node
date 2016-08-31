package br.com.lett.crawlernode.test.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.task.Crawler;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;

public class SaopauloSantaluziaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.santaluzia.com.br/";

	public SaopauloSantaluziaCrawler(CrawlerSession session) {
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
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getUrl().split(",product,")[1].split(",")[0]));

			// Nome
			Elements element_nome = doc.select("#ctl00_ContentSite_lblProductDsName");
			String name = element_nome.text().replace("'", "").trim();

			// Preço
			Elements element_preco = doc.select("span.price");
			Float price = Float.parseFloat(element_preco.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_cats = doc.select("span.tits_conteudo_cms a");
			String category1 = element_cats.get(1).text().trim();
			String category2 = element_cats.get(2).text().trim();
			String category3 = "";

			// Imagens
			Elements element_foto = doc.select(".img_detalhe_produto img");
			String primaryImage = element_foto.get(0).attr("src");
			if(primaryImage != null && !primaryImage.equals("")) primaryImage = "http://www.santaluzia.com.br/" + primaryImage;
			if(primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) primaryImage = ""; //TODO: Verificar o nome da foto genérica
			String secondaryImages = null;

			// Descrição
			String description = "";

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setSeedId(this.session.getSeedId());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains(",product,");
	}
}