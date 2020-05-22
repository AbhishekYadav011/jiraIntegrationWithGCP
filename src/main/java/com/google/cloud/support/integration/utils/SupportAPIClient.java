/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.support.integration.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloudsupport.v1alpha2.CloudSupport;
import com.google.cloudsupport.v1alpha2.model.SupportAccount;

/**
 * Demonstrate integrations with Google Cloud Support API using Support API SDK.
 * No guarantees of thread safety, performance, etc
 */
public class SupportAPIClient {
	private static String CLOUD_SUPPORT_API_URL;
	private static String CLOUD_SUPPORT_ATTACHMENT_URL;
	private static String CLOUD_SUPPORT_SCOPE;
	private static String SERVICE_ACCOUNT_ID;
	private static String SERVICE_ACCOUNT_PRIVATE_KEY;
	private static String SUPPORT_ACCOUNT_ID;
	private static String CLOUD_PROJECT_ID;

	private static GoogleCredential credential;
	private static CloudSupport supportService;
	private static SupportAccount account;

	private static boolean isInitialized = false;
	private static SupportAPIClient apiClient;

	private SupportAPIClient() {
	}

	static {
		try {
			String propertiesFile = System.getenv("PROPERTIES_FILE");
			if (propertiesFile == null) {
				System.out.println("Please set PROPERTIES_FILE environment variable");
				System.exit(-1);
			}

			Properties prop = loadPropertiesFile(propertiesFile);
			initializeConstants(prop);

			initializeCredentialAndSupportService();
			initializeSupportAccount();
		} catch (Exception e) {
			System.out.println("Exception in SupportAPIClient initialization");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static Properties loadPropertiesFile(String fileName) throws IOException {
		Properties prop = new Properties();
		InputStream in = new FileInputStream(fileName);
		prop.load(in);
		in.close();

		return prop;
	}

	private static void initializeConstants(Properties prop) {
		CLOUD_SUPPORT_API_URL = prop.getProperty("CLOUD_SUPPORT_API_URL");
		CLOUD_SUPPORT_ATTACHMENT_URL = prop.getProperty("CLOUD_SUPPORT_ATTACHMENT_URL");
		CLOUD_SUPPORT_SCOPE = prop.getProperty("CLOUD_SUPPORT_SCOPE");

		SERVICE_ACCOUNT_ID = prop.getProperty("SERVICE_ACCOUNT_ID");
		SERVICE_ACCOUNT_PRIVATE_KEY = prop.getProperty("SERVICE_ACCOUNT_PRIVATE_KEY");
		SUPPORT_ACCOUNT_ID = prop.getProperty("SUPPORT_ACCOUNT_ID");
		//CLOUD_PROJECT_ID = prop.getProperty("CLOUD_PROJECT_ID");
	}

	private static void initializeCredentialAndSupportService() throws GeneralSecurityException, IOException {
		// Service setup
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		credential = new GoogleCredential.Builder().setTransport(httpTransport).setJsonFactory(jsonFactory)
				.setServiceAccountId(SERVICE_ACCOUNT_ID)
				.setServiceAccountPrivateKeyFromP12File(new File(SERVICE_ACCOUNT_PRIVATE_KEY))
				.setServiceAccountScopes(Collections.singleton(CLOUD_SUPPORT_SCOPE)).build();

		credential.refreshToken();
		//System.out.println("Access Token: " + credential.getAccessToken());
		// Main API service is ready to use!
		supportService = new CloudSupport.Builder(httpTransport, jsonFactory, credential).build();
	}

	private static void initializeSupportAccount() throws IOException {
		account = supportService.supportAccounts().get(SUPPORT_ACCOUNT_ID).execute();
		System.out.println("Support Account ID: " + account.getAccountId());
	}

	static CloudSupport getSupportService() {
		return supportService;
	}

	static String getSupportAccountId() {
		return SUPPORT_ACCOUNT_ID;
	}

	/*static String getCloudProjectId() {
		return CLOUD_PROJECT_ID;
	}*/

	static String getUploadAttachmentAPIUrl() {
		return CLOUD_SUPPORT_ATTACHMENT_URL;
	}

	static String getAccessToken() throws IOException {
		credential.refreshToken();
		return credential.getAccessToken();
	}

	public static void main(String[] args) throws Exception {
		String propertiesFile = System.getenv("PROPERTIES_FILE");
		if (propertiesFile == null) {
			System.out.println("Please set PROPERTIES_FILE environment variable");
			return;
		}
	}
}
