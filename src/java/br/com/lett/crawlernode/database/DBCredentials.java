package br.com.lett.crawlernode.database;

public class DBCredentials {
	
	private MongoCredentials mongoCredentials;
	private PostgresCredentials postgresCredentials;
	
	public DBCredentials(MongoCredentials mongoCredentials, PostgresCredentials postgresCredentials) {
		this.mongoCredentials = mongoCredentials;
		this.postgresCredentials = postgresCredentials;
	}
	
	public MongoCredentials getMongoCredentials() {
		return this.mongoCredentials;
	}
	
	public PostgresCredentials getPostgresCredentials() {
		return this.postgresCredentials;
	}

}
