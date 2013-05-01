package com.fredericvauchelles;

import org.apache.maven.plugin.logging.Log;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.auth.oauth2.Credential;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;

public class MavenCredentialStore implements CredentialStore {
	private java.io.File googleDrivePropertiesDirectory;
	private Log logger;

	public MavenCredentialStore(java.io.File aGoogleDrivePropertiesDirectory, Log logger) {
		logger.debug("Initializing MavenCredentialStore");
		this.googleDrivePropertiesDirectory = aGoogleDrivePropertiesDirectory;
		this.logger = logger;
	}

	@Override
	public void delete(String userId, Credential credential) {
		java.io.File propertyFile = new java.io.File(this.googleDrivePropertiesDirectory, "googleDrive." + userId + ".properties");
		logger.debug("Delete token at " + propertyFile.getAbsolutePath());
		propertyFile.delete();
	}

	@Override
	public boolean load(String userId, Credential credential) {
		Properties props = new Properties();
		java.io.File propertyFile = new java.io.File(this.googleDrivePropertiesDirectory, "googleDrive." + userId + ".properties");
		if(propertyFile.exists()) {
			try {
				logger.debug("Reading token at : " + propertyFile.getAbsolutePath());
				props.load(new FileInputStream(propertyFile));
				String accessToken = props.getProperty("accessToken");
				String refreshToken = props.getProperty("refreshToken");
				Long expirationTimeMilliseconds = Long.parseLong(props.getProperty("expirationTimeMilliseconds"));

				if(accessToken != null && refreshToken != null) {
					credential.setAccessToken(accessToken);
					credential.setRefreshToken(refreshToken);
					credential.setExpirationTimeMilliseconds(expirationTimeMilliseconds);
					return true;
				}
				else
					logger.error("Access token or refresh token is not defined at " + propertyFile.getAbsolutePath());
			}
			catch(FileNotFoundException e) {
				logger.error(e);
			}
			catch(IOException e) {
				logger.error(e);
			}
		}
		else
			logger.info("Token not found at : " + propertyFile.getAbsolutePath());
		return false;
	}

	@Override
	public void store(String userId, Credential credential) {
		Properties props = new Properties();
		props.setProperty("accessToken", credential.getAccessToken());
		if(credential.getRefreshToken() != null)
			props.setProperty("refreshToken", credential.getRefreshToken());
		else
			logger.error("Refresh token does not exists in current credential");
		props.setProperty("expirationTimeMilliseconds", credential.getExpirationTimeMilliseconds().toString());
		java.io.File propertyFile = new java.io.File(this.googleDrivePropertiesDirectory, "googleDrive." + userId + ".properties");
		try {
			logger.debug("Store token at " + propertyFile.getAbsolutePath());
			props.store(new FileOutputStream(propertyFile), null);	
		}
		catch(FileNotFoundException e) {
			logger.error(e);
		}
		catch(IOException e){
			logger.error(e);
		}
	}
}