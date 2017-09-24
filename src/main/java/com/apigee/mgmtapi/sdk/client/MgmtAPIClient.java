package com.apigee.mgmtapi.sdk.client;

import java.nio.charset.Charset;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apigee.mgmtapi.sdk.ConfigurationException;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.apigee.mgmtapi.sdk.core.AppConfig;
import com.apigee.mgmtapi.sdk.model.AccessToken;
import com.apigee.mgmtapi.sdk.service.FileService;
import com.google.gson.Gson;

public class MgmtAPIClient {

	private static final Logger logger = LoggerFactory.getLogger(MgmtAPIClient.class);


	/**
	 * To get the Access Token Management URL, client_id and client_secret needs
	 * to be passed through a config file whose full path is passed as system
	 * property like -DconfigFile.path="/to/dir/config.properties"
	 *
	 * @param username the apigee account username of the developer
	 * @param password the apigee account password of the developer
	 *
	 * @return a valid access token
	 */
	public AccessToken getAccessToken(String username, String password) {
		Environment env = this.getConfigProperties();
		return getAccessToken(env.getProperty("mgmt.login.url"),
				env.getProperty("mgmt.login.client.id"),
				env.getProperty("mgmt.login.client.secret"), username, password);
	}

	/**
	 * To get the Access Token Management URL, client_id and client_secret needs
	 * to be passed through a config file whose full path is passed as system
	 * property like -DconfigFile.path="/to/dir/config.properties"
	 *
	 * @param username the apigee account username of the developer
	 * @param password the apigee account password of the developer
	 * @param mfa      the 2nd factor auth token to use for login
	 *
	 * @return a valid access token
	 */
	public AccessToken getAccessToken(String username, String password, String mfa) {
		Environment env = getConfigProperties();
		if (env == null) {
			logger.error("Config file missing");
			throw new ConfigurationException("Config file missing");
		}
		if (mfa == null || mfa.equals("")) {
			logger.error("mfa cannot be empty");
			throw new ConfigurationException("mfa cannot be empty");
		}
		return getAccessToken(env.getProperty("mgmt.login.mfa.url") + mfa, env.getProperty("mgmt.login.client.id"),
				env.getProperty("mgmt.login.client.secret"), username, password);
	}


	/**
	 * To get Access Token
	 *
	 * @param url           the token url to use
	 * @param clientId      the oauth clientId needed to get a token
	 * @param client_secret the oauth client secret needed to get a token
	 * @param username      the apigee account username of the developer
	 * @param password      the apigee account password of the developer
	 * @param mfa           the 2nd factor auth token to use for login
	 *
	 * @return a valid access token
	 */
	public AccessToken getAccessToken(String url,
									  String clientId,
									  String client_secret,
									  String username,
									  String password,
									  String mfa) {
		return getAccessToken(url + "?mfa_token=" + mfa, clientId, client_secret, username, password);
	}

	/**
	 * To get the Access Token
	 *
	 * @param url           the token url to use
	 * @param clientId      the oauth clientId needed to get a token
	 * @param client_secret the oauth client secret needed to get a token
	 * @param username      the apigee account username of the developer
	 * @param password      the apigee account password of the developer
	 *
	 * @return a valid access token
	 */
	public AccessToken getAccessToken(String url,
									  String clientId,
									  String client_secret,
									  String username,
									  String password) {

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		AccessToken token = new AccessToken();
		ResponseEntity<String> result;

		headers.add("Authorization", "Basic "
				+ new String(Base64.encode((clientId + ":" + client_secret).getBytes()), Charset.forName("UTF-8")));
		headers.add("Content-Type", "application/x-www-form-urlencoded");
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("username", username);
		map.add("password", password);
		map.add("grant_type", "password");
		HttpEntity<Object> request = new HttpEntity<Object>(map, headers);
		result = restTemplate.postForEntity(url, request, String.class);
		if (result.getStatusCode().equals(HttpStatus.OK)) {
			Gson gson = new Gson();
			token = gson.fromJson(result.getBody(), AccessToken.class);

		}

		return token;

	}

	/**
	 * To get the Access Token from Refresh Token
	 *
	 * @param url           the token url to use
	 * @param clientId      the oauth clientId needed to get a token
	 * @param client_secret the oauth client secret needed to get a token
	 * @param refreshToken  a valid refresh token
	 *
	 * @return a newly issued access token (TODO should also return the new refresh token that was issued alongside)
	 */
	public AccessToken getAccessTokenFromRefreshToken(String url, String clientId, String client_secret, String refreshToken) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		AccessToken token = new AccessToken();
		ResponseEntity<String> result = null;
		headers.add("Authorization", "Basic "
				+ new String(Base64.encode((clientId + ":" + client_secret).getBytes()), Charset.forName("UTF-8")));
		headers.add("Content-Type", "application/x-www-form-urlencoded");
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("refresh_token", refreshToken);
		map.add("grant_type", "refresh_token");
		HttpEntity<Object> request = new HttpEntity<Object>(map, headers);
		result = restTemplate.postForEntity(url, request, String.class);
		if (result.getStatusCode().equals(HttpStatus.OK)) {
			Gson gson = new Gson();
			token = gson.fromJson(result.getBody(), AccessToken.class);

		}
		return token;

	}

	/**
	 * Fetch the properties from the property file passed as system argument (-DconfigFile.path)
	 *
	 * @return environment configuration
	 *
	 * @throws ConfigurationException throws exception when the configuration cannot be retrieved
	 */
	public Environment getConfigProperties() {

		if (isBlank(System.getProperty("configFile.path"))) {
			throw new ConfigurationException("Configuration file system property 'configFile.path' is not configured.");
		}

		try {
			AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
			// TODO spring junk in an SDK looks dodgy, needs refactoring
			FileService service = (FileService) context.getBean("fileService");
			Environment env = service.getEnvironment();
			if (env == null) {
				throw new ConfigurationException("Loaded configuration is null.");
			}
			return env;
		} catch (BeansException e) {
			logger.error(e.getMessage(), e);
			throw new ConfigurationException("Something went wrong loading configuration", e);
		}
	}
}
