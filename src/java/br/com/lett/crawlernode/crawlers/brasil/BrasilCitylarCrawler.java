package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class BrasilCitylarCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.citylar.com.br/";

	public BrasilCitylarCrawler(CrawlerSession session) {
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

		if ( isProductPage(this.session.getOriginalURL(), doc) ) {

			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String internalID = null;
			Element elementInternalID = doc.select("#ProdutoDetalhesCodigoProduto").first();
			if(elementInternalID != null) {
				internalID = elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();
			}	

			// Nome
			String name = null;
			Element elementName = doc.select("#ProdutoDetalhesNomeProduto h1").first();
			if(elementName != null) {
				name = elementName.text().replace("'","").replace("’","").trim();
				
				Element nameVariation = doc.select(".selectAtributo option[selected]").first();
				
				if(nameVariation != null){
					String textName = nameVariation.text();
					
					if(textName.contains("|")){						
						name = name + " " + textName.split("\\|")[0].trim();;
					} else {
						name = name + " " + textName.trim();
					}
				}
			}

			// Preço
			Float price = null;
			Element elementPrice = doc.select("#ProdutoDetalhesPrecoComprarAgoraPrecoDePreco").first();
			if (elementPrice == null) {
				price = null;
			} else {
				price = Float.parseFloat(
						elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;
			Element elementBuyButton = doc.select("#btnComprar").first();
			if (elementBuyButton == null) {
				available = false;
			}


			// Categories
			ArrayList<String> categories = crawlCategories(doc);
			String category1 = getCategory(categories, 0);
			String category2 = getCategory(categories, 1);
			String category3 = getCategory(categories, 2);
			
			// Imagens
			Elements elementImages = doc.select("#ProdutoDetalhesFotosFotosPequenas").select("a img");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secundarias_array = new JSONArray();

			for (Element e : elementImages) {

				// Tirando o 87x87 para pegar imagem original
				if (primaryImage == null) {
					primaryImage = e.attr("src").replace("/87x87", "");
				} else {
					secundarias_array.put(e.attr("src").replace("/87x87", ""));
				}

			}

			if (secundarias_array.length() > 0) {
				secondaryImages = secundarias_array.toString();
			}

			// Descrição
			String description = "";
			Element element_descricao = doc.select("#ProdutoDescricao").first();
			if (element_descricao != null) {
				description = element_descricao.html().replace("’", "").trim();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			// Prices
			Prices prices = crawlPrices(doc, price);
			
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalID);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document document) {
		Element elementProduct = document.select("#ProdutoDetalhes").first();
		return url.startsWith("http://www.citylar.com.br/Produto/") && (elementProduct != null);
	}
	
	private ArrayList<String> crawlCategories(Document document) {
		ArrayList<String> categories = new ArrayList<String>();
		Elements elementCategories = document.select(".breadcrumbs-itens a");

		for (int i = 0; i < elementCategories.size(); i++) { 
			categories.add( elementCategories.get(i).text().trim() );
		}

		return categories;
	}

	private String getCategory(ArrayList<String> categories, int n) {
		if (n < categories.size()) {
			return categories.get(n);
		}

		return "";
	}

	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentsPriceMap = new HashMap<>();
			Elements parcelas = doc.select("#ProdutoDetalhesParcelamentoJuros p");

			if(parcelas.size() > 0){
				for(Element e : parcelas){
					String parcela = e.text().toLowerCase();

					int x = parcela.indexOf("x");
					int y = parcela.indexOf("r$");

					Integer installment = Integer.parseInt(parcela.substring(0, x).trim());
					Float priceInstallment = Float.parseFloat(parcela.substring(y).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

					installmentsPriceMap.put(installment, priceInstallment);
				}

				prices.insertBankTicket(installmentsPriceMap.get(1));
				prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
				prices.insertCardInstallment(Card.DINERS.toString(), installmentsPriceMap);
				prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentsPriceMap);
				prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentsPriceMap);
				prices.insertCardInstallment(Card.AMEX.toString(), installmentsPriceMap);
			}
		}

		return prices;
	}
}
