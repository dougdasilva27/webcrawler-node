package br.com.lett.crawlernode.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

public class CustomCredentialsProvider implements AWSCredentialsProvider {
	
	private String accessKey;
	private String secretKey;
	
	public CustomCredentialsProvider(String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	@Override
	public AWSCredentials getCredentials() {
		return new BasicAWSCredentials(accessKey, secretKey);
	}

	@Override
	public void refresh() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
