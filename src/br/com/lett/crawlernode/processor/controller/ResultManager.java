package br.com.lett.crawlernode.processor.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import au.com.bytecode.opencsv.CSVReader;

import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.kernel.fetcher.Proxy;
import br.com.lett.crawlernode.kernel.models.BrandModel;
import br.com.lett.crawlernode.kernel.models.ClassModel;
import br.com.lett.crawlernode.kernel.models.Market;
import br.com.lett.crawlernode.kernel.models.ProcessedModel;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;
import br.com.lett.crawlernode.processor.base.Information;
import br.com.lett.crawlernode.processor.base.ReplacementMaps;
import br.com.lett.crawlernode.processor.extractors.ExtractorFlorianopolisAngeloni;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ProcessedModelSanitizer;
import br.com.lett.crawlernode.processor.base.Extractor;
import br.com.lett.crawlernode.processor.base.IdentificationLists;

import com.mongodb.client.MongoDatabase;

/**
 * Classe responsável por processar produtos da tabela crawler a fim de transformá-los em
 * produtos legíves para serem encaminhados para a tabela processed
 * @author Doug
 */
public class ResultManager {

	private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

	// Mongo para o SIFT similarity
	private MongoDatabase mongo;

	// Banco de dados, para usar Postgres
	private DatabaseManager db;

	// Mapas de Substituição
	private Map<String, String> unitsReplaceMap;
	private Map<String, String> recipientsReplaceMap;

	// Listas de identificação
	private List<String> recipientsList;
	private List<String> unitsList;
	private List<ClassModel> classModelList;
	private List<BrandModel> brandModelList;

	// Variáveis de controle
	private boolean logActivated;

	// Variáveis de apoio
	private DateFormat isoDateFormat;
	private Map<Integer, String> cityNameInfo;
	private Map<Integer, String> marketNameInfo;
	private List<Market> markets;

	private List<Proxy> proxies;

	// Variável usada no teste de processer de um determinado supermercado
	private ArrayList<Integer> marketid;

	/**
	 * Construtor do ResultManager chamado pelo Crawler
	 * @category Construtor
	 * @author fabricio
	 * @param activateLogging - Define a ativação ou não dos logs
	 */
	public ResultManager(boolean activateLogging, MongoDatabase mongo, DatabaseManager db) throws NullPointerException {
		this.db = db;
		this.mongo = mongo;

		this.initialize(activateLogging);
	}

	//	public void setMongoDatabse(MongoDatabase mongoPanelDatabase) {
	//		this.mongoPanelDatabase = mongoPanelDatabase;
	//	}

	/**
	 * Construtora responsável pela inicialização dos mapas de identificação e substituição
	 * <p>Download de listas do google docs (arquivos csv) : https://docs.google.com/spreadsheets/d/<KEY>/export?gid=0&format=csv<p>
	 * @category Construtor
	 * @author doug
	 * @param activateLogging - Define a ativação ou não dos logs
	 */
	public ResultManager(boolean activateLogging) throws NullPointerException {
		this.initialize(activateLogging);
	}

	/**
	 * Função responsável pela inicialização do ResultManager
	 * @author fabricio
	 */
	private void initialize(boolean activateLogging) throws NullPointerException {

		// Busca hora e data atual no sistema
		this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		this.logActivated = activateLogging;

		// Cria as informaçẽos do supermercado
		this.createMarketInfo();

		// Inicializa os mapas de substituição
		this.unitsReplaceMap = new LinkedHashMap<String, String>();
		this.recipientsReplaceMap = new LinkedHashMap<String, String>();

		// Inicializa os mapas de identificação
		this.recipientsList = new ArrayList<String>();
		this.unitsList = new ArrayList<String>();
		this.classModelList = new ArrayList<ClassModel>();
		this.brandModelList = new ArrayList<BrandModel>();

		//Logging.printLogDebug(logger, pid, mode, environment, msg);
		Logging.printLogDebug(logger, "Creating Result Manager and downloading lists sheets from Google Drive");
		String [] nextLine;
		//CSVReader reader;
		String key;
		String value;


		// Cria Modelo de manipulação de Marcas para substituição e identificação
		System.out.print(".");
		try{
			// ResultSet com resultados da consulta das classes
			ResultSet rs = this.db.runSqlConsult(Information.queryForLettBrandProducts);

			// Enquanto houver linhas...
			while(rs.next()){

				// Verificação se a marca deve ser ignorada
				// Exemplo: papel higiênico leve 3 pague 2 PAMPERS
				// LEVE também é uma marca, mas queremos PAMPERS, por isso LEVE não será incluída na nossa lista de marcas
				if(!rs.getBoolean("ignored")){		
					BrandModel aux = null;

					// Procurando se já existe a marca na lista de marcas
					for(BrandModel bm: brandModelList) {
						if(bm.getBrand().equals(rs.getString("denomination"))) {
							aux = bm;
							break;
						}
					}

					// Se não existe, cria um novo BrandModel e o incluí na lista de brandsModelList
					if(aux == null) {
						aux = new BrandModel(rs.getString("denomination"), rs.getString("lett_supplier"));
						aux.putOnList(rs.getString("mistake"));
						brandModelList.add(aux);
					} 
					// Caso já exista uma incidência, apenas adiciona o erro no mapa
					else {
						aux.putOnList(rs.getString("mistake"));
					}
				}
			}
			// Encerra o ResultSet
			rs.close();

			// Trata exceções do ResultSet
		}	catch (Exception e) {
			e.printStackTrace();
		}

		// Completa marcas ou corrige marcas através da lista de marcas	
		for(BrandModel bm : brandModelList) {
			// Automação 1: marcas sem espaço
			// Exemplo: Marca de nome: jack daniels
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          jackdaniels (errado) -> jack daniels (correto)
			if(bm.getBrand().contains(" ")) {
				bm.putOnList(bm.getBrand().replace(" ", ""));

				Pattern space = Pattern.compile(" ");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}

			// Automação 2: marcas com hifén.
			// Exemplo: marca de nome: arco-íris
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          arco íris (errado) -> arco-íris (correto)
			//          arcoíris (errado)  -> arco-íris (correto)
			if(bm.getBrand().contains("-")) {
				bm.putOnList(bm.getBrand().replace("-", ""));
				bm.putOnList(bm.getBrand().replace("-", " "));

				Pattern space = Pattern.compile("-");
				Matcher matcherSpace = space.matcher(bm.getBrand());
				while (matcherSpace.find()) {
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
					bm.putOnList(bm.getBrand().substring(0, matcherSpace.start()) + " " + bm.getBrand().substring(matcherSpace.end(), bm.getBrand().length()));
				}
			}
		}

		// Cria listas de identificação de recipientes
//		System.out.print(".");
//		try {
//			reader = new CSVReader(new BufferedReader(new InputStreamReader(new URL(Information.recipientsListCSV).openStream())));
//			while ((nextLine = reader.readNext()) != null) {
//				key = nextLine[0].toLowerCase();
//				this.recipientsList.add(key);
//			}
//			reader.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		this.recipientsList = IdentificationLists.recipientsList;

		// Cria listas de identificação de unidades
//		System.out.print(".");
//		try {
//			reader = new CSVReader(new BufferedReader(new InputStreamReader(new URL(Information.unitsListCSV).openStream())));
//			while ((nextLine = reader.readNext()) != null) {
//				key = nextLine[0].toLowerCase();
//				this.unitsList.add(key);
//			}
//			reader.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		this.unitsList = IdentificationLists.unitsList;

		// Cria listas de substituição de unidades
//		System.out.print(".");
//		try {
//			reader = new CSVReader(new BufferedReader(new InputStreamReader(new URL(Information.unitsReplaceMapCSV).openStream())));
//			while ((nextLine = reader.readNext()) != null) {
//				key = nextLine[0].toLowerCase();
//				value = nextLine[1].toLowerCase();
//				this.unitsReplaceMap.put(key, value);
//			}
//			reader.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		this.unitsReplaceMap = ReplacementMaps.unitsReplaceMap;

		// Cria listas de substituição de recipientes
//		System.out.print(".");
//		try {
//			reader = new CSVReader(new BufferedReader(new InputStreamReader(new URL(Information.recipientsReplaceMapCSV).openStream())));
//			reader.readNext();
//			while ((nextLine = reader.readNext()) != null) {
//				key = nextLine[0].toLowerCase().trim();
//				value = nextLine[1].toLowerCase().trim();
//				this.recipientsReplaceMap.put(key, value);
//			}
//			reader.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		this.recipientsReplaceMap = ReplacementMaps.recipientsReplaceMap;

		// Cria Modelo de manipulação da classes para torná-las LettClasses
		try{
			// ResultSet com resultados da consulta das classes
			ResultSet rs = this.db.runSqlConsult(Information.queryForLettClassProducts);

			// Enquanto houver linhas...
			while(rs.next()){

				ClassModel aux = null;

				// Procurando se já existe na lista a classe
				for(ClassModel lcm: classModelList) {
					if(lcm.getLettName().equals(rs.getString("denomination"))) {
						aux = lcm;
						break;
					}
				}

				// Se não existe, cria um nome lettClassesModel e adiciona na lista
				if(aux == null) {
					aux = new ClassModel(rs.getString("denomination"));
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
					classModelList.add(aux);
				} 
				// Caso já exista uma incidência, apenas adiciona o erro no mapa
				else {
					aux.putOnMap(rs.getString("mistake"), rs.getString("extra"));
				}
			}
			// Encerra o ResultSet
			rs.close();

			// Trata exceções do ResultSet
		}	catch (Exception e) {
			e.printStackTrace();
		}

		// Completa classes ou corrige classes através da lista de classes	
		for(ClassModel cm : classModelList){

			Pattern space = Pattern.compile(" ");

			// Automação 1: marcas sem espaço
			// Exemplo: Marca de nome: jack daniels
			//          Nós adicionaremos automaticamente com brandsReplaceMap e suas combinações: 
			//          jackdaniels (errado) -> jack daniels (correto)
			if(cm.getLettName().contains(" ")) {

				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", ""))) 	cm.putOnMap(cm.getLettName().replace(" ", ""), "");
				if(!cm.getMistakes().containsKey(cm.getLettName().replace(" ", "-"))) 	cm.putOnMap(cm.getLettName().replace(" ", "-"), "");

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}

			// Automação 2: marcas com hifén.
			// Exemplo: marca de nome: arco-íris
			//          Nós adicionaremos automáticamente com brandsReplaceMap e suas combinações: 
			//          arco íris (errado) -> arco-íris (correto)
			//          arcoíris (errado)  -> arco-íris (correto)
			if(cm.getLettName().contains("-")) {

				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", ""))) 	cm.putOnMap(cm.getLettName().replace("-", ""), "");
				if(!cm.getMistakes().containsKey(cm.getLettName().replace("-", " "))) 	cm.putOnMap(cm.getLettName().replace("-", " "), "");

				Matcher matcherSpace = space.matcher(cm.getLettName());
				while(matcherSpace.find()){
					cm.putOnMap(cm.getLettName().substring(0, matcherSpace.start()) + cm.getLettName().substring(matcherSpace.end(), cm.getLettName().length()), "");
				}
			}
		}
		Logging.printLogDebug(logger, "success!");

		Logging.printLogDebug(logger, "Fetching proxies to download images on Processor...");

		proxies = new ArrayList<Proxy>();


		try {
			String url = "http://api.buyproxies.org/?a=showProxies&pid=40833&key=80069a39926fb5a7cbc4a684092572b0";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {

				proxies.add(new Proxy(
						inputLine.split(":")[0], 
						55555, 
						inputLine.split(":")[2], 
						inputLine.split(":")[3]));

				response.append(inputLine);
			}
			in.close();

			Logging.printLogDebug(logger, proxies.size() + " proxies fetched!");

		} catch (Exception e) {
			System.err.print("error!");
			e.printStackTrace();
		}

	}

	// =================== DADOS CRAWLER ======================

	/**
	 * Método responsável por buscar produtos na tabela Processed modificados recentemente que
	 * precisar passar pelo processamento de novo, pois podem ter mudanças.
	 * @author Fabricio
	 * @return Retorna ResultSet com informaçẽos da consuta da tabela Processed
	 * @throws SQLException 
	 */
	private ResultSet fetchModifiedProcessedProductsAsPointer(DateTime date) throws SQLException {		
		ResultSet rs = this.db.runSqlConsult(Information.queryForProcessedProducts + "'" + date.toString("yyyy-MM-dd HH:mm:ss.SSS") + "'");
		return rs;
	}

	/**
	 * Método responsável por buscar produtos na tabela Processed modificados recentemente que
	 * precisar passar pelo processamento de novo, pois podem ter mudanças.
	 * @author Fabricio
	 * @return Retorna ResultSet com informaçẽos da consuta da tabela Processed
	 * @throws SQLException 
	 */
	private ResultSet fetchModifiedProcessedProductsAsPointer(DateTime date, Integer marketId) throws SQLException {		
		ResultSet rs = this.db.runSqlConsult(Information.queryForProcessedProducts + "'" + date.toString("yyyy-MM-dd HH:mm:ss.SSS") + "' AND market = " + marketId);
		return rs;
	}

	/**
	 * Método responsável por buscar produtos de clientes na tabela Processed modificados recentemente que
	 * precisar passar pelo processamento de novo, pois podem ter mudanças.
	 * @author Fabricio
	 * @return Retorna ResultSet com informaçẽos da consuta da tabela Processed
	 * @throws SQLException 
	 */
	private ResultSet fetchClientModifiedProcessedProductsAsPointer(DateTime date, Integer marketId, String market, String city) throws SQLException {		
		String query = "SELECT processed.*, lett.supplier as lett_supplier FROM processed "
				+ "LEFT OUTER JOIN lett ON (processed.internal_id = lett."+ city + "_" + market + "_internal_id AND processed.market = " + marketId + ") "
				+ "WHERE "
				+ "processed.lmt > '" + date.toString("yyyy-MM-dd HH:mm:ss.SSS") + "' AND market = " + marketId + " AND lett_supplier IS NOT NULL";

		return this.db.runSqlConsult(query);
	}

	/**
	 * Método responsável pela busca de produtos na tabela Crawler, funciona como um apontamento para tal tabela com resultados de um supermercado específico
	 * @param market - Supermercado a buscar na tabela Crawler
	 * @return - ResultSet com dados do Crawler referente ao Supermercado
	 * @throws SQLException 
	 */
	private ResultSet fetchMarketsAsPointer() throws SQLException {
		ResultSet rs = this.db.runSqlConsult(Information.queryMarkets);
		return rs;
	}

	// =================== DADOS PROCESSED ======================

	/**
	 * Método responsável por buscar uma linha na tabela processed e retorna ProcessedModel com as informações de um produto específico
	 * @autor Doug
	 * @param internal_id - Id interno de cada supermercado
	 * @param market - Supermercado à ser consultado
	 * @return ProcessModel com os valores da linha com os parametros da consulta
	 */
	private ProcessedModel fetchProcessedProduct(String internal_id, int market) {
		try {

			// Faz consulta na tabela processed de acordo com os parametros recebidos e os retém em um ResultSet
			ResultSet rs = this.db.runSqlConsult(Information.queryForSelectProcessedProduct_part1 + internal_id + Information.queryForSelectProcessedProduct_part2 + market);
			ProcessedModel p = null;


			// Enquanto houver próxima linha...
			while (rs.next()) {

				JSONObject digitalContent;
				try 					{ 	digitalContent = new JSONObject(rs.getString("digital_content"));
				} catch (Exception e) 	{	digitalContent = null; }

				JSONObject changes;
				try 					{ 	changes = new JSONObject(rs.getString("changes"));
				} catch (Exception e) 	{	changes = null; }

				JSONArray similars;
				try 					{ 	similars = new JSONArray(rs.getString("similars"));
				} catch (Exception e) 	{	similars = null; }

				JSONArray behaviour;
				try 					{ 	behaviour = new JSONArray(rs.getString("behaviour"));
				} catch (Exception e) 	{	behaviour = null; }

				JSONArray marketplace;
				try 					{ 	marketplace = new JSONArray(rs.getString("marketplace"));
				} catch (Exception e) 	{	marketplace = null; }

				Integer actual_stock;
				try 					{ 	actual_stock = rs.getInt("stock"); if(actual_stock == 0) actual_stock = null;
				} catch (Exception e) 	{	actual_stock = null; }

				// Salva resultados de cada coluna em um ProcessModel
				p = new ProcessedModel(rs.getLong("id"), rs.getString("internal_id"), rs.getString("internal_pid"), rs.getString("original_name"), rs.getString("class"), rs.getString("brand"),
						rs.getString("recipient"), rs.getDouble("quantity"), rs.getInt("multiplier"), rs.getString("unit"), 
						rs.getString("extra"), rs.getString("pic"), rs.getString("secondary_pics"), rs.getString("cat1"),
						rs.getString("cat2"), rs.getString("cat3"), rs.getString("url"), rs.getInt("market"), 
						rs.getString("ect"), rs.getString("lmt"), rs.getString("lat"), rs.getString("lrt"), rs.getString("lms"), rs.getString("status"), changes,
						rs.getString("original_description"), rs.getFloat("price"), 
						digitalContent, rs.getLong("lett_id"), similars, rs.getBoolean("available"), rs.getBoolean("void"), actual_stock, behaviour, marketplace);
				break;
			}
			rs.close();
			// ProcessModel pode ser nulo caso não encontre resultados na tabela processor
			return p;
		}
		// Caso algo ocorra errado o retorno será nulo
		catch (SQLException e) {
			Logging.printLogError(logger, "Problem fetching processed product with internal_id=" + internal_id + " and market=" + market);
			Logging.printLogError(logger, e.getMessage());

			return null;
		}
	}

	/**
	 * Método responsável pelo preparo da persistência de dados, ou seja, prepara para o envio de dados para o banco.<br>
	 * Também é responsável por definir se será inserida ou atualizada uma linha.
	 * @author Doug
	 * @param pm - ProcessedModel à ser enviado para o banco de dados
	 * @throws SQLException 
	 * @category Conexão
	 */
	private void saveProcessedModel(ProcessedModel pm) throws SQLException {

		// Prepara O PocessedModel para persistência
		ProcessedModelSanitizer.prepareToPersist(pm);

		// Caso já exista o produto é atualizada a linha no banco de dados processed
		if (pm.getId() != null) {
			this.db.runSqlExecute("UPDATE processed SET "
					+ "internal_id=" + pm.getInternalId() + ","
					+ "void = false," // void é sempre false. Se tiver que ser true, é o history que o fará.
					+ "original_name=" + pm.getOriginalName() + ","
					+ "original_description=" + pm.getOriginalDescription() + ","
					+ "class=" + pm.get_class() + ","
					+ "brand=" + pm.getBrand() + ","
					+ "recipient=" + pm.getRecipient() + ","
					+ "quantity=" + pm.getQuantity() + ","
					+ "multiplier=" + pm.getMultiplier() + ","
					+ "unit=" + pm.getUnit() + ","
					+ "extra=" + pm.getExtra() + ","
					+ "pic=" + pm.getPic() + ","
					+ "secondary_pics=" + pm.getSecondary_pics() + ","
					+ "price=" + pm.getPrice() + ","
					+ "cat1=" + pm.getCat1() + ","
					+ "cat2=" + pm.getCat2() + ","
					+ "cat3=" + pm.getCat3() + ","
					+ "url=" + pm.getUrl() + ","
					+ "market=" + pm.getMarket() + ","
					+ "ect=" + pm.getEct() + ","
					+ "lmt=" + pm.getLmt() + ","
					+ "changes=" + pm.getChanges() + ", "
					+ "digital_content='" + pm.getDigitalContent() + "', "
					+ "behaviour='" + pm.getBehaviour() + "' "
					+ "WHERE id=" + pm.getId());
		} else {
			// Não teremos mais este caso. Quem cria processed agora é o crawler. O processor só atualiza.
		}
	}

	/**
	 * 
	 * Extrai informações a partir dos campos "original_" do ProcessModel, e por fim
	 * salva os dados recebidos dentro do ProcessModel a ser retornado.
	 * 
	 * @author Fabricio
	 * @category Manipulação
	 * @param cm Recebe valores do Crawler e os transfere para o ProcessModel
	 * @return pm Retorna processModel com valores do Crawler 
	 */
	public ProcessedModel processProduct(ProcessedModel pm, CrawlerSession session) {	
		Logging.printLogDebug(logger, session, "Processing product in ResultManager...");

		// Previne o conteúdo de extra ser nulo
		if (pm.getExtra() == null) pm.setExtra("");

		// Tratamento de exceção na definição de supermercado, no caso de valores inválidos ou nulos o supermercado usado será o 1 FlorianopolisAngeloni por padrão
//		try {

			// Polimorfismo da classe extractor, aqui será definido qual extractor será utilizado
//			extractor = Class.forName("br.com.lett.processor." +
//					this.cityNameInfo.get(pm.getMarket()) + 
//					".Extractor" + 
//					Character.toUpperCase(this.cityNameInfo.get(pm.getMarket()).charAt(0)) +
//					this.cityNameInfo.get(pm.getMarket()).substring(1) +
//					Character.toUpperCase(this.marketNameInfo.get(pm.getMarket()).charAt(0)) +
//					marketNameInfo.get(pm.getMarket()).substring(1)).newInstance();
		
		
			Extractor extractor = new ExtractorFlorianopolisAngeloni();
			
//		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//			Logging.printLogError(logger, "Processor not defined for " + this.marketNameInfo.get(pm.getMarket()) + ". Using FlorianopolisAngeloni as default.");
//			extractor = new ExtractorFlorianopolisAngeloni();
//		} catch (NullPointerException e) {
//			Logging.printLogError(logger, "Market " + pm.getMarket() + " not included on postgres market table yet. Using FlorianopolisAngeloni as default.");
//			extractor = new ExtractorFlorianopolisAngeloni();
//		}

		// Coerção do objeto 'extractor' como Extractor para definição de atributos da classe
		((Extractor) extractor).setAttrs(logActivated, 
				brandModelList,
				unitsReplaceMap, 
				recipientsReplaceMap,
				recipientsList,
				unitsList,
				classModelList);

		// Aciona o método 'extract' de acordo com cada supermecado
		pm = ((Extractor) extractor).extract(pm);

		// Se em modo clients, atualizando campos de monitoramento de conteúdo digital
//		if(mode.equals(Controller.MODE_INSIGHTS) || mode.equals(Controller.MODE_PLACEHOLDER)) this.updateDigitalContent(pm); // TODO
		if (Main.executionParameters.getMode().equals(ExecutionParameters.MODE_INSIGHTS)) {
			this.updateDigitalContent(pm, session);
		}

		if (logActivated) Logging.printLogDebug(logger, "\n---> Final result:");

		if (logActivated) Logging.printLogDebug(logger, pm.toString());

		return pm;
	}

	/**
	 * Método responsável por testar o processamento de todos os produtos de um único supermercado.
	 * Exibe número de marcas, classes, recipientes, unidades estraídas pelo Processor.
	 * @author doug
	 * @param market - Supermercado a ser testado
	 * @param saveProcessedModel - Define se salva os dados na tabela processed ou não
	 * @throws SQLException
	 * @category Manipulação
	 * 
	 */
//	public void processProducts(String city, String market, boolean test) throws SQLException{
//
//		// Variável para medir tempo...
//		long start = System.currentTimeMillis();		
//		ResultSet rs = null;
//
//
//		// Popular lista de ids de supermercado
//		List<Integer> marketIdsToPersist = new ArrayList<Integer>();
//
//		rs = this.fetchMarketsAsPointer();
//
//		while(rs.next()) {
//			if(city != null && market == null) {
//				if(rs.getString("city").equals(city)) marketIdsToPersist.add(rs.getInt("id"));
//			} else {
//				if(rs.getString("city").equals(city) && rs.getString("name").equals(market)) marketIdsToPersist.add(rs.getInt("id"));
//			}
//		}
//
//		Logging.printLogDebug(logger, "Lista de ids de markets que vou processar: " + marketIdsToPersist);
//
//		// Processando um supermercado por vez
//
//		// Contadores com o resultado de contéudos processados
//		int countBrand = 0, countClass = 0, countRecipient = 0, countQtd = 0, countUnit = 0, countMult = 0, count = 0;
//
//		for(int marketId: marketIdsToPersist) {
//
//			rs = this.fetchModifiedProcessedProductsAsPointer(new DateTime().minusDays(1), marketId);
//
//			// Enquanto houver linhas, faça...
//			while (rs.next()) {
//
//				JSONObject digitalContent;
//				try 					{ 	digitalContent = new JSONObject(rs.getString("digital_content"));
//				} catch (Exception e) 	{	digitalContent = null; }
//
//				JSONObject changes;
//				try 					{ 	changes = new JSONObject(rs.getString("changes"));
//				} catch (Exception e) 	{	changes = null; }
//
//				JSONArray similars;
//				try 					{ 	similars = new JSONArray(rs.getString("similars"));
//				} catch (Exception e) 	{	similars = null; }
//
//				JSONArray behaviour;
//				try 					{ 	behaviour = new JSONArray(rs.getString("behaviour"));
//				} catch (Exception e) 	{	behaviour = null; }
//
//				JSONArray marketplace;
//				try 					{ 	marketplace = new JSONArray(rs.getString("marketplace"));
//				} catch (Exception e) 	{	marketplace = null; }
//
//				Integer actual_stock;
//				try 					{ 	actual_stock = rs.getInt("stock"); if(actual_stock == 0) actual_stock = null;
//				} catch (Exception e) 	{	actual_stock = null; }
//
//				// Transfere os valores de cada coluna de cada linha para um Crawler model
//				ProcessedModel pm = new ProcessedModel(rs.getLong("id"), rs.getString("internal_id"), rs.getString("internal_pid"), rs.getString("original_name"), rs.getString("class"), rs.getString("brand"),
//						rs.getString("recipient"), rs.getDouble("quantity"), rs.getInt("multiplier"), rs.getString("unit"), rs.getString("extra"), rs.getString("pic"), rs.getString("secondary_pics"), rs.getString("cat1"),
//						rs.getString("cat2"), rs.getString("cat3"), rs.getString("url"), rs.getInt("market"), rs.getString("ect"), rs.getString("lmt"), rs.getString("lat"), rs.getString("lrt"), rs.getString("lms"), rs.getString("status"),
//						changes, rs.getString("original_description"),
//						rs.getFloat("price"), digitalContent, rs.getLong("lett_id"), similars, rs.getBoolean("available"), rs.getBoolean("void"), actual_stock, behaviour, marketplace);
//
//
//				if (logActivated) Logging.printLogDebug(logger, "\n\n\n\n--------------------------------------------------------------------------------------------------------------");
//				if (logActivated) Logging.printLogDebug(logger, "INICIANDO PROCESSAMENTO DE PRODUTO: " + pm.getOriginalName());
//
//				// Através do método processProduct minera os valores para popular/atualizar o ProcessedModel
//				ProcessedModel pm_new = this.processProduct(pm);
//
//				if (logActivated) Logging.printLogDebug(logger, "SALVANDO PRODUTO:");
//
//				// Insere ou atualiza o ProcessedModel no banco de dados
//				this.saveProcessedModel(pm_new);
//				if (logActivated) Logging.printLogDebug(logger, "success!");
//				if (logActivated) Logging.printLogDebug(logger, "--------------------------------------------------------------------------------------------------------------");
//
//				// Verifica a quantidade atributos extraídos do CrawlerModel
//				if(pm.getBrand()==null) countBrand++;
//				if(pm.get_class()==null || pm.get_class().isEmpty())  countClass++;					
//				if(pm.getRecipient()!=null) countRecipient++;
//				if(pm.getMultiplier()!=1) countMult++;
//				if(pm.getQuantity()!= null) countQtd++;
//				if(pm.getUnit()!=null) countUnit++;
//				count++;
//			}
//
//		}
//
//		// Mostra resultados 
//		Logging.printLogDebug(logger, "Products tested: " + count +
//				"\nBrands not found found: " + countBrand +
//				"\nClass not found: " + countClass +
//				"\nRecipients found: " + countRecipient +
//				"\nQuantities found: " + countQtd +
//				"\nUnits found: " + countUnit +
//				"\nMultiplier found: " + countMult +
//				"\nExecution time: " + ((System.currentTimeMillis() - start)/(1000*60.0)) + "min");
//
//	}

	/**
	 * Atualiza informações de conteúdo digital no objeto Processed.
	 * 
	 * @author Fabricio
	 * @param pm - ProcessModel recebido no instante da execução
	 */
	private void updateDigitalContent(ProcessedModel pm, CrawlerSession session) {  
		Logging.printLogDebug(logger, session, "Updating digital content...");
		
		if(pm.getDigitalContent() == null) { pm.setDigitalContent(new JSONObject()); }

		// 0) Lendo informações desejadas pelo fornecedor (digital_content na tabela Lett)
		JSONObject lett_digital_content = new JSONObject();

		// 		0.1) A partir do ID Lett, lemos as informações
		try {
			
			ResultSet rs = this.db.runSqlConsult("SELECT digital_content FROM lett WHERE id = " + pm.getLettId());
			
			while(rs.next()) {
				lett_digital_content = new JSONObject(rs.getString("digital_content"));
			}
			
		} catch (Exception e) { 
			
		}

		// 1) Avaliando imagem
		JSONObject pic = new JSONObject();
		try {pic = pm.getDigitalContent().getJSONObject("pic");} catch (Exception e) { }

		//		1.1) Contando imagens
		pic.put("count", DigitalContentAnalyser.imageCount(pm));

		// 		1.2) Avaliando imagem primária
		JSONObject pic_primary = new JSONObject();
		if(pic.has("primary") ) pic_primary = pic.getJSONObject("primary");

		//		1.2.0) Baixando imagem primária armazenada na Amazon

		String primaryImageAmazonKey = "product-image" + 
				"/" +
				this.cityNameInfo.get(pm.getMarket()) + 
				"/" + 
				this.marketNameInfo.get(pm.getMarket()) + 
				"/" + 
				pm.getInternalId() + 
				"/1-original.jpg";

		String desiredPrimaryImageAmazonKey = "product-image" + 
				"/" +
				"lett" +
				"/" + 
				pm.getLettId() + 
				"/1-original.jpg";
		
		Logging.printLogDebug(logger, session, "Fetching image from Amazon...");
		File primaryImage = Information.fetchImageFromAmazon(primaryImageAmazonKey);
		
		Logging.printLogDebug(logger, session, "Fetching md5 from Amazon...");
		String desiredPrimaryMd5 = Information.fetchMd5FromAmazon(desiredPrimaryImageAmazonKey);

		String primaryMd5 = null;

		try {
			FileInputStream fis = new FileInputStream(primaryImage);
			primaryMd5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			fis.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}		

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");

		// Se o md5 for nulo, então limpamos e adicionamos a marcação de sem imagem
		if(primaryMd5 == null) {
			pic_primary = new JSONObject();

			pic_primary.put("status", "no-image");
			pic_primary.put("verified_by", "crawler_" + nowISO);

		} else {

			if(pic_primary.has("md5") && pic_primary.get("md5").equals(primaryMd5)) {
				// Imagem não mudou, então mantemos os parâmetros que já estavam antes
			} 

			// Imagem mudou
			else {

				// Capturando as dimensões da nova imagem primária
				pic_primary.put("dimensions", DigitalContentAnalyser.imageDimensions(primaryImage));

				// Capturando similaridade da nova imagem primária usando o NaiveSimilarityFinder
				pic_primary.put("similarity", DigitalContentAnalyser.imageSimilarity(primaryImage, Information.fetchImageFromAmazon(desiredPrimaryImageAmazonKey)));

				// Calculando similaridade da nova imagem primária usando o SIFT
				JSONObject similaritySiftResult = null;
				try {
					similaritySiftResult = DigitalContentAnalyser.similaritySIFT(mongo, db, primaryMd5, pm.getLettId(), desiredPrimaryMd5);
				} catch (Exception e) {
					e.printStackTrace();
				}				

				pic_primary.put("similarity_sift", similaritySiftResult);

				// updating md5 of the new primary image
				pic_primary.put("md5", primaryMd5);

				// Tentando comparar imagem com a referência e antecipar o Match
				if(pic_primary.getDouble("similarity") == 1) { // match
					pic_primary.put("status", "match");
				} else { // not verified
					pic_primary.put("status", "not-verified");
				}

				pic_primary.put("verified_by", "crawler_" + nowISO);

			} 

		}

		//		1.2.4) Deletando imagens locais
		if(primaryImage != null) {
			primaryImage.delete();
		}
		//if(desiredPrimaryImage != null) 	desiredPrimaryImage.delete();

		pic.put("primary", pic_primary);

		// 	1.3) Avaliando imagem secundária
		JSONObject pic_secondary = new JSONObject();
		if(pic.has("secondary") ) {
			pic_secondary = pic.getJSONObject("secondary");
		}

		Integer secondary_reference_count = 0;

		try {
			secondary_reference_count = lett_digital_content.getJSONObject("pic").getInt("secondary");
		} catch (JSONException e) { }

		if(secondary_reference_count == 0) { // no-reference tem precedência sobre no-image
			pic_secondary.put("status", "no-reference");
		} 
		else if (pic.getInt("count") <= 1) {
			pic_secondary.put("status", "no-image");
		}
		else if(pic.getInt("count")-1 >= secondary_reference_count) {
			pic_secondary.put("status", "complete");
		} 
		else {
			pic_secondary.put("status", "incomplete");
		}

		pic.put("secondary", pic_secondary);

		pm.getDigitalContent().put("pic", pic);


		// 2) Avaliando regras de nomeclatura
		JSONArray name_rules_results = new JSONArray();

		// 		2.1) Lendo regras de nomeclatura definidas no objeto lett
		JSONArray name_rules_desired = new JSONArray();
		if(lett_digital_content.has("name_rules") ) name_rules_desired = lett_digital_content.getJSONArray("name_rules");

		// 		2.2) Para cada regra, ver se é satisfeita ou não

		for(int i=0; i<name_rules_desired.length(); i++) {
			JSONObject rule = name_rules_desired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(pm.getOriginalName(), rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") 		&& !rule.isNull("section")) 	r.put("section", rule.getString("section"));
			if(rule.has("type") 	 	&& !rule.isNull("type")) 		r.put("type", rule.getString("type"));
			if(rule.has("condition") 	&& !rule.isNull("condition")) 	r.put("condition", rule.getInt("condition"));

			name_rules_results.put(r);
		}

		pm.getDigitalContent().put("name_rules_results", name_rules_results);


		// 3) Avaliando regras de descrição
		JSONArray description_rules_results = new JSONArray();

		// 		3.1) Lendo regras de descrição definidas no objeto lett
		JSONArray description_rules_desired = new JSONArray();
		if(lett_digital_content.has("description_rules") ) description_rules_desired = lett_digital_content.getJSONArray("description_rules");

		// 		3.2) Para cada regra, ver se é satisfeita ou não

		for(int i=0; i<description_rules_desired.length(); i++) {
			JSONObject rule = description_rules_desired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(pm.getOriginalDescription(), rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") 		&& !rule.isNull("section")) 	r.put("section", rule.getString("section"));
			if(rule.has("type") 	 	&& !rule.isNull("type")) 		r.put("type", rule.getString("type"));
			if(rule.has("condition") 	&& !rule.isNull("condition")) 	r.put("condition", rule.getInt("condition"));

			description_rules_results.put(r);
		}

		pm.getDigitalContent().put("description_rules_results", description_rules_results);


		// 4) Criando resumo de regras
		JSONObject rules_results = new JSONObject();
		rules_results.put("name", true); // Assumindo nome como OK
		rules_results.put("description", new JSONObject()); // Preparando lugar para sections da description

		// 		4.1) Ver se ocorreu alguma regra de nome que não foi satisfeita
		for(int i=0; i<name_rules_results.length(); i++) {
			if(!name_rules_results.getJSONObject(i).getBoolean("satisfied")) {
				// Marca como não-satisfeita e sai do loop
				rules_results.put("name", false);
				break;
			}
		}


		// 		4.2) Ver se ocorreu alguma regra de descrição em alguma section que não foi satisfeita
		for(int i=0; i<description_rules_results.length(); i++) {

			// Se nenhuma regra da section foi avaliada ainda, assumimos como OK
			if(!rules_results.getJSONObject("description").has(description_rules_results.getJSONObject(i).getString("section"))){
				rules_results.getJSONObject("description").put(description_rules_results.getJSONObject(i).getString("section"), true);
			}

			if(!description_rules_results.getJSONObject(i).getBoolean("satisfied")) {
				// Marca como não-satisfeita				
				rules_results.getJSONObject("description").put(description_rules_results.getJSONObject(i).getString("section"), false);
			}
		}



		for(int i=0; i<description_rules_desired.length(); i++) {
			JSONObject rule = description_rules_desired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(pm.getOriginalDescription(), rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") 		&& !rule.isNull("section")) 	r.put("section", rule.getString("section"));
			if(rule.has("type") 	 	&& !rule.isNull("type")) 		r.put("type", rule.getString("type"));
			if(rule.has("condition") 	&& !rule.isNull("condition")) 	r.put("condition", rule.getInt("condition"));

			description_rules_results.put(r);
		}

		pm.getDigitalContent().put("rules_results", rules_results);
	}

	/**
	 * Método responsável por buscar informações sobre cidade e supermercado
	 * @author Doug
	 * @throws SQLException 
	 * @category Setters e Getters
	 */
	private void createMarketInfo() {
		this.cityNameInfo = new HashMap<Integer, String>();
		this.marketNameInfo = new HashMap<Integer, String>();
		this.marketid = new ArrayList<Integer>();

		try {
			
			ResultSet rs = this.db.runSqlConsult("SELECT * FROM market");
			
			while(rs.next()) {
				
				// Mapa de informaçẽos da cidade
				this.cityNameInfo.put(rs.getInt("id"), rs.getString("city"));

				// Mapa de informaçẽos do Supermercado
				this.marketNameInfo.put(rs.getInt("id"), rs.getString("name"));

				// Contém ids dos supermercados para teste do processer
				this.marketid.add(rs.getInt("id"));
				
			}
		} catch (SQLException e) {
			Logging.printLogError(logger, "Error fetching market info on postgres!");
			Logging.printLogError(logger, e.getMessage());
		}
	}

	
	public List<Integer> getMarketid() {
		return this.marketid;
	}

	public Map<Integer, String> getCityNameInfo() {
		return this.cityNameInfo;
	}

	public Map<Integer, String> getMarketNameInfo() {
		return this.marketNameInfo;
	}

}
