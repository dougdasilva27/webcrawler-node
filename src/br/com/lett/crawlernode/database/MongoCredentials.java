package br.com.lett.crawlernode.database;

public class MongoCredentials {
	
	// Insights
	private String mongoInsightsUser;
	private String mongoInsightsPass;
	private String mongoInsightsHost;
	private String mongoInsightsPort;
	private String mongoInsightsDatabase;
	
	// Panel
	private String mongoPanelUser;
	private String mongoPanelPass;
	private String mongoPanelHost;
	private String mongoPanelPort;
	private String mongoPanelDatabase;
	
	// Images
	private String mongoImagesUser;
	private String mongoImagesPass;
	private String mongoImagesHost;
	private String mongoImagesPort;
	private String mongoImagesDatabase;

	public MongoCredentials() {
		super();
	}

	public String getMongoInsightsUser() {
		return mongoInsightsUser;
	}

	public void setMongoInsightsUser(String mongoInsightsUser) {
		this.mongoInsightsUser = mongoInsightsUser;
	}

	public String getMongoInsightsPass() {
		return mongoInsightsPass;
	}

	public void setMongoInsightsPass(String mongoInsightsPass) {
		this.mongoInsightsPass = mongoInsightsPass;
	}

	public String getMongoInsightsHost() {
		return mongoInsightsHost;
	}

	public void setMongoInsightsHost(String mongoInsightsHost) {
		this.mongoInsightsHost = mongoInsightsHost;
	}

	public String getMongoInsightsPort() {
		return mongoInsightsPort;
	}

	public void setMongoInsightsPort(String mongoInsightsPort) {
		this.mongoInsightsPort = mongoInsightsPort;
	}

	public String getMongoInsightsDatabase() {
		return mongoInsightsDatabase;
	}

	public void setMongoInsightsDatabase(String mongoInsightsDatabase) {
		this.mongoInsightsDatabase = mongoInsightsDatabase;
	}

	public String getMongoPanelUser() {
		return mongoPanelUser;
	}

	public void setMongoPanelUser(String mongoPanelUser) {
		this.mongoPanelUser = mongoPanelUser;
	}

	public String getMongoPanelPass() {
		return mongoPanelPass;
	}

	public void setMongoPanelPass(String mongoPanelPass) {
		this.mongoPanelPass = mongoPanelPass;
	}

	public String getMongoPanelHost() {
		return mongoPanelHost;
	}

	public void setMongoPanelHost(String mongoPanelHost) {
		this.mongoPanelHost = mongoPanelHost;
	}

	public String getMongoPanelPort() {
		return mongoPanelPort;
	}

	public void setMongoPanelPort(String mongoPanelPort) {
		this.mongoPanelPort = mongoPanelPort;
	}

	public String getMongoPanelDatabase() {
		return mongoPanelDatabase;
	}

	public void setMongoPanelDatabase(String mongoPanelDatabase) {
		this.mongoPanelDatabase = mongoPanelDatabase;
	}

	public String getMongoImagesUser() {
		return mongoImagesUser;
	}

	public void setMongoImagesUser(String mongoImagesUser) {
		this.mongoImagesUser = mongoImagesUser;
	}

	public String getMongoImagesPass() {
		return mongoImagesPass;
	}

	public void setMongoImagesPass(String mongoImagesPass) {
		this.mongoImagesPass = mongoImagesPass;
	}

	public String getMongoImagesHost() {
		return mongoImagesHost;
	}

	public void setMongoImagesHost(String mongoImagesHost) {
		this.mongoImagesHost = mongoImagesHost;
	}

	public String getMongoImagesPort() {
		return mongoImagesPort;
	}

	public void setMongoImagesPort(String mongoImagesPort) {
		this.mongoImagesPort = mongoImagesPort;
	}

	public String getMongoImagesDatabase() {
		return mongoImagesDatabase;
	}

	public void setMongoImagesDatabase(String mongoImagesDatabase) {
		this.mongoImagesDatabase = mongoImagesDatabase;
	}

	private String getCredentials() { // TODO remove...only to test -- private only
		StringBuilder sb = new StringBuilder();
		
		sb.append("mongoInsightsUser: " + this.mongoInsightsUser + "\n");
		sb.append("mongoInsightsPass: " + this.mongoInsightsPass + "\n");
		sb.append("mongoInsightsHost: " + this.mongoInsightsHost + "\n");
		sb.append("mongoInsightsPort: " + this.mongoInsightsPort + "\n");
		sb.append("mongoInsightsDatabase: " + this.mongoInsightsDatabase);
		
		sb.append("mongoPanelUser: " + this.mongoPanelUser + "\n");
		sb.append("mongoPanelPass: " + this.mongoPanelPass + "\n");
		sb.append("mongoPanelHost: " + this.mongoPanelHost + "\n");
		sb.append("mongoPanelPort: " + this.mongoPanelPort + "\n");
		sb.append("mongoPanelDatabase: " + this.mongoPanelDatabase);
		
		sb.append("mongoImagesUser: " + this.mongoImagesUser + "\n");
		sb.append("mongoImagesPass: " + this.mongoImagesPass + "\n");
		sb.append("mongoImagesHost: " + this.mongoImagesHost + "\n");
		sb.append("mongoImagesPort: " + this.mongoImagesPort + "\n");
		sb.append("mongoImagesDatabase: " + this.mongoImagesDatabase);

		return sb.toString();
	}


}
