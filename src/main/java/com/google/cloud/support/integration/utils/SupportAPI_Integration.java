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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloudsupport.v1alpha2.CloudSupport;
import com.google.cloudsupport.v1alpha2.model.Attachment;
import com.google.cloudsupport.v1alpha2.model.CloudSupportCase;
import com.google.cloudsupport.v1alpha2.model.Comment;
import com.google.cloudsupport.v1alpha2.model.StartAttachmentRequest;
import com.google.cloudsupport.v1alpha2.model.StartAttachmentResponse;
import com.google.cloudsupport.v1alpha2.model.SupportAccount;


/**
 * Demonstrate integrations with Google Cloud Support API using Support API SDK.
 */
public class SupportAPI_Integration {
  //private static String CLOUD_SUPPORT_API_URL = "https://cloudsupport.googleapis.com/v1alpha2";
  //private static String CLOUD_SUPPORT_ATTACHMENT_URL = "https://cloudsupport.googleapis.com/upload/v1/media/";
  //private static String CLOUD_SUPPORT_SCOPE = "https://www.googleapis.com/auth/cloudsupport";

  // Customer specific config
  //private static String SERVICE_ACCOUNT_ID = "supportapi-integration@anilgcp-project.iam.gserviceaccount.com";
  //private static File SERVICE_ACCOUNT_PRIVATE_KEY = new File("/usr/local/google/home/achintapatla/GCP/anilgcp-project-adbf18307125.p12");
  //private static String SUPPORT_ACCOUNT_ID = "supportAccounts/gcp-sa-1312582817";
  
  private static String CLOUD_SUPPORT_API_URL = "https://cloudsupport.googleapis.com/v1alpha2";
  private static String CLOUD_SUPPORT_ATTACHMENT_URL = "https://cloudsupport.googleapis.com/upload/v1/media/";
  private static String CLOUD_SUPPORT_SCOPE = "https://www.googleapis.com/auth/cloudsupport";
  private static String SERVICE_ACCOUNT_ID;
  private static String SERVICE_ACCOUNT_PRIVATE_KEY;
  private static String SUPPORT_ACCOUNT_ID;

  private static GoogleCredential credential;

  public static void initialize(String fileName) throws IOException {
    Properties prop = loadPropertiesFile(fileName);

    CLOUD_SUPPORT_API_URL = prop.getProperty("CLOUD_SUPPORT_API_URL");
    CLOUD_SUPPORT_ATTACHMENT_URL = prop.getProperty("CLOUD_SUPPORT_ATTACHMENT_URL");
    CLOUD_SUPPORT_SCOPE = prop.getProperty("CLOUD_SUPPORT_SCOPE");

    SERVICE_ACCOUNT_ID = prop.getProperty("SERVICE_ACCOUNT_ID");
    SERVICE_ACCOUNT_PRIVATE_KEY = prop.getProperty("SERVICE_ACCOUNT_PRIVATE_KEY");
    SUPPORT_ACCOUNT_ID = prop.getProperty("SUPPORT_ACCOUNT_ID");
  }

  private static Properties loadPropertiesFile(String fileName) throws IOException {
    Properties prop = new Properties();
    InputStream in = new FileInputStream(fileName);
    prop.load(in);
    in.close();

    return prop;
  }

  private static CloudSupport getSupportService () throws Exception {
    // Service setup
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    credential = new GoogleCredential.Builder()
      .setTransport(httpTransport)
      .setJsonFactory(jsonFactory)
      .setServiceAccountId(SERVICE_ACCOUNT_ID)
      .setServiceAccountPrivateKeyFromP12File(new File(SERVICE_ACCOUNT_PRIVATE_KEY))
      .setServiceAccountScopes(Collections.singleton(CLOUD_SUPPORT_SCOPE))
      .build();

    credential.refreshToken();
    System.out.println("Access Token: " + credential.getAccessToken());  
    // Main API service is ready to use!
    CloudSupport supportService = new CloudSupport.Builder(httpTransport, jsonFactory, credential).build();

    return supportService;
  }


  static void supportAPITests(CloudSupport supportService) {
    try {
      // Each call will look something like this:
      SupportAccount account = supportService.supportAccounts().get(SUPPORT_ACCOUNT_ID).execute();
      System.out.println("Support Account ID: " + account.getAccountId());


      CloudSupportCase cloudCase = createCase(SUPPORT_ACCOUNT_ID, supportService);
      //Comment commentCreated = createComment(supportService, cloudCase, "Test Comment from Support API");


      String attachmentFilePath = "/usr/local/google/home/achintapatla/GCP/Cloud_API/testAttachment.txt";
      Attachment attachment =  createAttachment(supportService, cloudCase, attachmentFilePath);

      String caseName = cloudCase.getName();//"supportAccounts/gcp-sa-1312582817/cases/16168886";//16108876";
      getAttachmentList(supportService, caseName);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static CloudSupportCase  createCase(String supportAccountId, CloudSupport supportService) throws IOException {
    List<String> ccList = new ArrayList<String>();
    ccList.add("anil@anilgcp.com");

    CloudSupportCase supportCase = new CloudSupportCase();
    supportCase.setDisplayName("Test Case from API");
    supportCase.setProjectId("anilgcp-project");
    supportCase.setState("NEW");
    supportCase.setPriority("P3");
    supportCase.setCcAddresses(ccList);
    supportCase.setTestCase(true);

    CloudSupportCase cloudCase = supportService.supportAccounts().cases().create(supportAccountId, supportCase).execute();
    System.out.println("Created Case Id: " + cloudCase.getName());

    return cloudCase;

  }

  private static Comment createComment(CloudSupport supportService, CloudSupportCase cloudCase, String commentString) 
    throws IOException {
      Comment comment = new Comment();
      comment.setText(commentString);

      return supportService.supportAccounts().cases().comments().create(cloudCase.getName(), comment).execute();
  }
  
  private static Attachment createAttachment(CloudSupport supportService, CloudSupportCase cloudCase, String attachmentFilePath) 
    throws Exception {
    //TODO testAttachment
    String fileName = "testAttachment.txt";
    StartAttachmentResponse startAttachmentResponse = startAttachment(supportService, cloudCase);
    uploadAttachment(startAttachmentResponse.getName(), attachmentFilePath);
    return postAttachment(supportService, cloudCase.getName(), startAttachmentResponse.getName(), fileName);
    //return postAttachment(supportService, cloudCase, "supportAccounts/gcp-sa-1312582817/cases/16168825/attachments/10597094001", attachmentFilePath, fileName);
  }

  private static StartAttachmentResponse startAttachment(CloudSupport supportService, CloudSupportCase cloudCase) 
    throws IOException {
      StartAttachmentRequest startAttachmentRequest = new StartAttachmentRequest();
      StartAttachmentResponse startAttachmentResponse = supportService.supportAccounts().cases().
        startAttachment(cloudCase.getName(), startAttachmentRequest).execute();
      System.out.println("StartAttachmentResponse Name: " + startAttachmentResponse.getName());

      return startAttachmentResponse;
  }
  
  private static void uploadAttachment(String attachmentName, String filePath) throws Exception {
      credential.refreshToken();
      String accessToken = credential.getAccessToken();
      System.out.println("Access Token: " + accessToken);  

      String urlString = CLOUD_SUPPORT_ATTACHMENT_URL + attachmentName + "?upload_type=media";
 
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      System.out.println("uploadAttachment urlString: " + urlString);
   
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Authorization", "Bearer " + accessToken);
      conn.setRequestProperty("Accept", "application/json");
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
   
      /*
      String data =  readFileContents(filePath);
      System.out.println("uploadAttachment filesize: " + data.length());
      OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
      out.write(data);
      out.close();
      */
      byte[] data = Utils.readFileContentsInBytes(filePath);
      System.out.println("uploadAttachment filesize: " + data.length);
      OutputStream outputStream = conn.getOutputStream();
      DataOutputStream bos = new DataOutputStream(outputStream);
      bos.write(data); 
      bos.close();

      System.out.println("conn.getResponseCode(): " + conn.getResponseCode());
      System.out.println("conn.getResponseMessage(): " + conn.getResponseMessage());

      System.out.println("Successfully Uploaded File to: " + attachmentName);
  }

  private static String readFileContents(String filePath) throws Exception {
      InputStream inputStream = new FileInputStream(filePath);
      System.out.println("Available bytes from the file :"+inputStream.available());
      byte[] bytes = new byte[inputStream.available()];

      StringBuffer sb = new StringBuffer();
      while (inputStream.read(bytes) != -1) {
        sb.append(new String(bytes, StandardCharsets.UTF_8));
      }

      return sb.toString();
  }

  private static Attachment postAttachment(CloudSupport supportService, String cloudCaseName, 
    String attachmentName, String fileName) throws Exception {
      System.out.println("cloudCaseName: " + cloudCaseName);
      System.out.println("postAttachment Name: " + attachmentName);
      System.out.println("postAttachment fileName: " + fileName);
      Attachment attachment = new Attachment();
      attachment.setName(attachmentName);
      attachment.setFileName(fileName);
      attachment.setMimeType("text/html");
      attachment.setSize(new Long(5));

      Attachment createdAttachment = supportService.supportAccounts().cases().
        attachments().create(cloudCaseName, attachment).execute();
      System.out.println("createdAttachment Name: " + createdAttachment);

      return createdAttachment;
  }

  private static List<Attachment> getAttachmentList(CloudSupport supportService, String caseName) throws Exception {

      java.util.List<Attachment> attachmentList = supportService.supportAccounts().cases().
        attachments().list(caseName).execute().getAttachments();
      System.out.println("attachmentList: " + attachmentList);

      return attachmentList;
  }  

  public static void main(String[] args) throws Exception {
    
    String propertiesFile = System.getenv("PROPERTIES_FILE");
    if(propertiesFile == null) {
      System.out.println("Please set PROPERTIES_FILE environment variable");
      return;
    }

    initialize(propertiesFile);
    supportAPITests(getSupportService());

  }
}
