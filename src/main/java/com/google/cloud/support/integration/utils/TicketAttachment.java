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
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.google.cloudsupport.v1alpha2.CloudSupport;
import com.google.cloudsupport.v1alpha2.model.Attachment;
import com.google.cloudsupport.v1alpha2.model.StartAttachmentRequest;
import com.google.cloudsupport.v1alpha2.model.StartAttachmentResponse;

public class TicketAttachment {
  private TicketAttachment() {}

  public static Attachment addAttachmentToTicket(String cloudCaseName, 
  	String attachmentFileName, byte[] attachmentContents, int attachmentSize, String mimeType) throws IOException {

    StartAttachmentResponse startAttachmentResponse = startAttachment(SupportAPIClient.getSupportService(), cloudCaseName);

    uploadAttachment(SupportAPIClient.getAccessToken(), SupportAPIClient.getUploadAttachmentAPIUrl(), 
      startAttachmentResponse.getName(), attachmentContents);
    
    return finalizeAttachment(SupportAPIClient.getSupportService(), cloudCaseName, startAttachmentResponse.getName(), 
      attachmentFileName, attachmentSize, mimeType);
  }


  public static List<Attachment> getAttachmentsMetadata(String cloudCaseName) 
      throws IOException {

    List<Attachment> attachmentList =  SupportAPIClient.getSupportService().supportAccounts().cases().
      attachments().list(cloudCaseName).execute().getAttachments();
    if(attachmentList == null)
        return Collections.EMPTY_LIST;
    System.out.println("attachmentList Size: " + attachmentList.size());
    return attachmentList;
  }

  private static StartAttachmentResponse startAttachment(CloudSupport supportService, String cloudCaseName) 
    throws IOException {
    StartAttachmentRequest startAttachmentRequest = new StartAttachmentRequest();
    StartAttachmentResponse startAttachmentResponse = supportService.supportAccounts().cases().
      startAttachment(cloudCaseName, startAttachmentRequest).execute();
    System.out.println("StartAttachmentResponse Name: " + startAttachmentResponse.getName());

    return startAttachmentResponse;
  }
  
  private static void uploadAttachment(String accessToken, String attachmentAPIUrl, String attachmentName, 
  	byte[] attachmentContent) throws IOException {

   // System.out.println("Access Token: " + accessToken);  
    String urlString = attachmentAPIUrl + attachmentName + "?upload_type=media";
 
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    System.out.println("uploadAttachment urlString: " + urlString);
   
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("Accept", "application/json");
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
/*   
    System.out.println("uploadAttachment filesize: " + attachmentContent.length);
    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
    out.write(attachmentContent);
    out.close();
*/
    System.out.println("uploadAttachment filesize: " + attachmentContent.length);
    OutputStream outputStream = conn.getOutputStream();
    DataOutputStream dos = new DataOutputStream(outputStream);
    dos.write(attachmentContent); 
    dos.close();


    System.out.println("conn.getResponseCode(): " + conn.getResponseCode());
    System.out.println("conn.getResponseMessage(): " + conn.getResponseMessage());

    System.out.println("Successfully Uploaded File to: " + attachmentName);
  }

  private static Attachment finalizeAttachment(CloudSupport supportService, String cloudCaseName, 
    String attachmentName, String attachmentFileName, int attachmentSize, String mimeType) throws IOException {

    System.out.println("cloudCaseName: " + cloudCaseName);
    System.out.println("finalizeAttachment Name: " + attachmentName);
    System.out.println("finalizeAttachment attachmentFileName: " + attachmentFileName);
    
    Attachment attachment = new Attachment();
    attachment.setName(attachmentName);
    attachment.setFileName(attachmentFileName);
    attachment.setMimeType(mimeType);
    attachment.setSize(new Long(attachmentSize));

    Attachment createdAttachment = supportService.supportAccounts().cases().
      attachments().create(cloudCaseName, attachment).execute();
    System.out.println("createdAttachment Name: " + createdAttachment);

    return createdAttachment;
  }

  /*Created for downloading attachment*/
  public static String getAttachmentsUrl(String attachmentResourceName) 
	      throws IOException {

	    return SupportAPIClient.getSupportService().supportAccounts().cases().
	      attachments().getAttachmentUrl(attachmentResourceName).execute().getUrl();
	    
	  }
  
  
}