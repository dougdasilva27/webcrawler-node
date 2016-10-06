package br.com.lett.crawlernode.crawlers.brasil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class BrasilBalaodainformaticaCrawler extends Crawler {

	public BrasilBalaodainformaticaCrawler(CrawlerSession session) {
		super(session);
	}


	@Override
	public boolean shouldVisit() {
		String href =  session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith("http://www.balaodainformatica.com.br") || href.startsWith("https://www.balaodainformatica.com.br"));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if (session.getOriginalURL().startsWith("https://www.balaodainformatica.com.br/Produto/")
				|| session.getOriginalURL().startsWith("http://www.balaodainformatica.com.br/Produto/")
				|| session.getOriginalURL().startsWith("https://www.balaodainformatica.com.br/ProdutoAnuncio/")
				|| session.getOriginalURL().startsWith("http://www.balaodainformatica.com.br/ProdutoAnuncio/")) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalId = null;
			Element elementInternalID = doc.select(".produto-detalhe p").first();
			if (elementInternalID != null) {
				internalId = elementInternalID.ownText().trim();
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementProduct = doc.select("#content-center").first();
			if(elementProduct != null){
				Element element_name = elementProduct.select("#nome h1").first();
				if(element_name != null){
					name = element_name.ownText().replace("'", "").replace("’", "").trim();
				}
			}
			
			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = elementProduct.select("#btnAdicionarCarrinho").first();
			if (elementBuyButton == null) {
				available = false;
			}

			// Preço
			Float price = calculatePrice(elementProduct);

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";
			Element elementCategories = elementProduct.select("h2").first();
			String[] categories = elementCategories.text().split(">");
			for (String c : categories) {
				if (category1.isEmpty()) {
					category1 = c.trim();
				} else if (category2.isEmpty()) {
					category2 = c.trim();
				} else if (category3.isEmpty()) {
					category3 = c.trim();
				}
			}

			// Imagens secundárias e primária
			Elements elementImages = elementProduct.select("#imagens-minis img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			if (elementImages.isEmpty()) {

				Element elementImage = elementProduct.select("#imagem-principal img").first();
				if (elementImage != null) {
					primaryImage = elementImage.attr("src");
				}

			} else {
				for (Element e : elementImages) {
					if (primaryImage == null) {
						primaryImage = e.attr("src").replace("imagem2", "imagem1");
					} else {
						secondaryImagesArray.put(e.attr("src").replace("imagem2", "imagem1"));
					}
				}
			}

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription = doc.select("#especificacoes").first();
			if (elementDescription != null)
				description = elementDescription.html().replace("’", "").trim();

			// Filtragem
			boolean mustInsert = true;

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			if (mustInsert) {

				try {

					Product product = new Product();
					product.setUrl(session.getOriginalURL());
					product.setInternalId(internalId);
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

				} catch (Exception e1) {
					e1.printStackTrace();
				}

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	} 
	
	private Float calculatePrice(Element elementProduct){
		Float price = null;
		Element elementPrice = elementProduct.select("#preco-comprar .avista").first();
		if (elementPrice != null) {
			price = Float.parseFloat(
					elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
		}
		
		if(price != null){
			Elements descontoElement = elementProduct.select("#preco-comprar p");
			
			String descontoString = null;
			
			for(Element e : descontoElement){
				String temp = e.ownText().toLowerCase();
				
				if(temp.contains("desconto")){
					descontoString = temp.trim();
				}
			}
			
			if(descontoString != null){
				int desconto = Integer.parseInt(descontoString.replaceAll("[^0-9]", "").trim());
				
				price = normalizeTwoDecimalPlaces((float) ((price * 100) / (100 - desconto)));
			}
		}
		
		return price;
	}
	
	/**
	 * Round and normalize Double to have only two decimal places
	 * eg: 23.45123 --> 23.45
	 * @param number
	 * @return A rounded Double with only two decimal places
	 */
	public static Float normalizeTwoDecimalPlaces(Float number) {
		BigDecimal big = new BigDecimal(number);
		String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
		
		return Float.parseFloat(rounded);
	}
}
