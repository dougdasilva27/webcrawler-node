package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

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

			// Disponibilidade
			boolean available = false;

			// Categoria
			String category1 = "";
			String category2 = "";
			String category3 = "";

			
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			if (secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			Element elementProduct = doc.select("#content-center").first();
			if(elementProduct != null){
				Element element_name = elementProduct.select("#nome h1").first();
				if(element_name != null){
					name = element_name.ownText().replace("'", "").replace("’", "").trim();
				}

				Element elementBuyButton = elementProduct.select("#btnAdicionarCarrinho").first();
				if (elementBuyButton != null) {
					available = true;
				}

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
			}

			// Preço Boleto
			Float priceBank = crawlPriceBank(elementProduct);
			
			// Preço
			Float price = calculatePrice(elementProduct, priceBank);

			// Prices
			Prices prices = crawlPrices(price, priceBank);
			
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

				} catch (Exception e1) {
					e1.printStackTrace();
				}

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	} 

	private Float calculatePrice(Element elementProduct, Float priceBank){
		Float price = null;

		if(elementProduct != null){

			if(priceBank != null){
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

					price = MathCommonsMethods.normalizeTwoDecimalPlaces((float) ((priceBank * 100) / (100 - desconto)));
				}
			}
		}

		return price;
	}

	/**
	 * To crawl installment is acessed a api 
	 * this api return a json like this:
	 * 
	 {
		"success":true,
		"parcelas":[
			{
				"cardBrand":"visa",
				"quantity":1,
				"amount":12.7080,
				"amountFormatado":"R$ 12,71",
				"totalAmount":12.71,
				"totalAmountFormatado":"R$ 12,71",
				"interestFree":true
			},
			{
				"cardBrand":"visa",
				"quantity":2,
				"amount":7.06,
				"amountFormatado":"R$ 7,06",
				"totalAmount":14.12,
				"totalAmountFormatado":"R$ 14,12",
				"interestFree":true
			}
		]
	 }
	 * 
	 * @param price
	 * @param priceBank
	 * @return
	 */
	private Prices crawlPrices(Float price, Float priceBank){
		Prices prices = new Prices();
		
		if (price != null) {
			prices.insertBankTicket(priceBank);
			
			String url = "http://www.balaodainformatica.com.br/PagSeguro/GetInstallmentsByValue";
			String payload = "value=" + price.toString().replace(".", ",");		
			
			String op = DataFetcher.fetchString(DataFetcher.POST_REQUEST, session, url, payload, cookies);
			
			JSONObject jsonInstallments;
			try {
				jsonInstallments = new JSONObject(op);
			} catch (JSONException e) {
				jsonInstallments = new JSONObject();
			}
			
			if (jsonInstallments.has("parcelas")) {
				Map<Integer,Float> installmentPriceMap = new HashMap<>();
				JSONArray arrayInstallments = jsonInstallments.getJSONArray("parcelas");
				
				for (int i = 0; i < arrayInstallments.length(); i++) {
					JSONObject jsonInstallment = arrayInstallments.getJSONObject(i);
					
					if (jsonInstallment.has("quantity")) {
						Integer installment = jsonInstallment.getInt("quantity");
						
						if (jsonInstallment.has("amount")) {
							Double valueDouble = jsonInstallment.getDouble("amount");
							
							Float value = MathCommonsMethods.normalizeTwoDecimalPlaces(valueDouble.floatValue());
							installmentPriceMap.put(installment, value);
						}
					}
				}
				
				prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			}
		}
		return prices;
	}
	
	private Float crawlPriceBank(Element elementProduct){
		Float price = null;
		
		if(elementProduct != null){
			Element elementPrice = elementProduct.select("#preco-comprar .avista").first();
			if (elementPrice != null) {
				price = Float.parseFloat(
						elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}
		}
		
		return price;
	}
}
