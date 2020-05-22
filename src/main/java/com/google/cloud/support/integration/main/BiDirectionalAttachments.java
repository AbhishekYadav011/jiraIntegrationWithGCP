/**
 * 
 */
package com.google.cloud.support.integration.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.cloud.support.integration.utils.TicketAttachment;
import com.google.cloud.support.integration.utils.Utils;
import com.google.cloudsupport.v1alpha2.model.Attachment;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sun.jersey.core.util.Base64;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * @author I334554
 *
 */
public class BiDirectionalAttachments {
	private static final String jql = "project = MCO AND \"GCP Support Case Title\" is not EMPTY AND \"GCP Support Ticket Status\" = \"In progress\"";
	private String username;
	private String password;
	private String jiraUrl;
	private JiraRestClient restClient;
	private static String propertiesFile = System.getenv("PROPERTIES_FILE");

	private BiDirectionalAttachments(String username, String password, String jiraUrl) {
		this.username = username;
		this.password = password;
		this.jiraUrl = jiraUrl;
		this.restClient = getJiraRestClient();

	}

	public static void main(String[] args)
			throws FileNotFoundException, IOException, JSONException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));
		BiDirectionalAttachments myJiraClient = new BiDirectionalAttachments(properties.getProperty("username"),
				properties.getProperty("password"), "https://jira.multicloud.int.sap");

		// Issue issue = myJiraClient.getIssue(issueKey);
		Iterable<Issue> issues = myJiraClient.getAllIssue(jql, 100, 0);
		for (Issue issue : issues) {
			System.out.println(issue.getKey());
			Path targetPath;
			String dirName = properties.getProperty("dirName");
			String gcpSupportCaseNo = "supportAccounts/gcp-sa-1039549927/cases/"+issue.getFieldByName("GCP Support Case Number").getValue().toString().trim();
			System.out.println("Support Case number:" + gcpSupportCaseNo);

			List<Timestamp> jiraAttachmentTimeByReporter = new ArrayList<Timestamp>();
			List<String> jiraAttachmentContent = new ArrayList<String>();
			List<String> jiraAttachmentName = new ArrayList<String>();
			List<Timestamp> jiraAttachmentLastTime = new ArrayList<Timestamp>();

			List<Attachment> gcpAttachmentList = TicketAttachment.getAttachmentsMetadata(gcpSupportCaseNo);
			Multimap<Timestamp, String> gcloudattachmenturlWithTimeBySupportEngineer = ArrayListMultimap.create();
			List<Timestamp> gcpAttachmentLastTime = new ArrayList<Timestamp>();
			List<Timestamp> gcpAttachmentTimeBySupport = new ArrayList<Timestamp>();
			Multimap<Timestamp, String> gcpAttachmentFileNameWithTimeBySupport = ArrayListMultimap.create();

			String Root_URI = "https://jira.multicloud.int.sap/rest/api/2/issue/";
			String issueRestAPIURL = String.valueOf(Root_URI) + issue.getKey() + "?fields=attachment";
			String postAttachmentToJira = String.valueOf(Root_URI) + issue.getKey() + "/attachments";

			RequestSpecification restrequest = RestAssured.given().auth().preemptive()
					.basic(properties.getProperty("username"), properties.getProperty("password"));

			Response response = (Response) restrequest.get(issueRestAPIURL, new Object[0]);
			JSONObject jsonObject = new JSONObject(response.asString().trim());
			JSONArray attachmentArray = jsonObject.getJSONObject("fields").getJSONArray("attachment");
			int attachmentLength = jsonObject.getJSONObject("fields").getJSONArray("attachment").length();

			/* For Getting GCP attachment time and URL */
			if ((gcpAttachmentList != null) && (!gcpAttachmentList.isEmpty())) {
				for (int i = 0; i < gcpAttachmentList.size(); i++) {
					String time = gcpAttachmentList.get(i).getCreateTime().replace("T", " ");
					Timestamp ts = Timestamp.valueOf(time.substring(0, time.length() - 1));
					/* For setting up time to GMT+1 */
					//ts.setTime(ts.getTime() + (1 * 60 * 60) * 1000);
					//System.out.println("Gcp Attachments Time" + ts);

					if (!gcpAttachmentList.get(i).getCreatorEmail()
							.equalsIgnoreCase("gcp-support-int@sap-pal-test.iam.gserviceaccount.com")) {

						gcpAttachmentTimeBySupport.add(ts);
						gcloudattachmenturlWithTimeBySupportEngineer.put(ts,
								TicketAttachment.getAttachmentsUrl(gcpAttachmentList.get(i).getName()));
						gcpAttachmentFileNameWithTimeBySupport.put(ts, gcpAttachmentList.get(i).getFileName());

					} else {
						gcpAttachmentLastTime.add(ts);
					}
				}
			}
			Collections.sort(gcpAttachmentLastTime);
			Collections.sort(gcpAttachmentTimeBySupport);
			
			for(int i=0;i<gcpAttachmentLastTime.size();i++) {
				System.out.println("gcpAttachmentLastTime:"+gcpAttachmentLastTime.get(i));
			}
			
			for(int i=0;i<gcpAttachmentTimeBySupport.size();i++) {
				System.out.println("gcpAttachmentTimeBySupport:"+gcpAttachmentTimeBySupport.get(i));
			}
			
			
			/* For Getting Jira attachment time and URL */
			if (attachmentLength != 0) {
				for (int i = 0; i < attachmentLength; i++) {

					JSONObject jsonObj = attachmentArray.getJSONObject(i);
					String time = jsonObj.getString("created").replace("T", " ");
					Timestamp ts = Timestamp.valueOf(time.substring(0, time.lastIndexOf(".")));
					/* For setting up time to GMT+1 */
					//ts.setTime(ts.getTime() + (1 * 60 * 60) * 1000);

					if (!jsonObj.getJSONObject("author").getString("displayName").contains("GCP Support Engineer")) {
						jiraAttachmentTimeByReporter.add(ts);
						jiraAttachmentName.add(jsonObj.getString("filename"));
						jiraAttachmentContent.add(jsonObj.getString("content"));
					} else {
						jiraAttachmentLastTime.add(ts);
					}

				}
			}
			Collections.sort(jiraAttachmentLastTime);
			//Collections.sort(jiraAttachmentTimeByReporter);
			
			for(int i=0;i<jiraAttachmentLastTime.size();i++) {
				System.out.println("jiraAttachmentLastTime:"+jiraAttachmentLastTime.get(i));
			}
			for(int i=0;i<jiraAttachmentTimeByReporter.size();i++) {
				System.out.println("jiraAttachmentTimeByReporter:"+jiraAttachmentTimeByReporter.get(i));
			}
			System.out.println("Attachment size by Jira usr:" + jiraAttachmentTimeByReporter.size());
			System.out.println("Attachment size by Gcp Support engineer to jira:" + jiraAttachmentLastTime.size());

			/* For Getting Jira attachment to GCP */
			if ((jiraAttachmentTimeByReporter != null) && (!jiraAttachmentTimeByReporter.isEmpty())) {
				for (int i = 0; i < jiraAttachmentTimeByReporter.size(); i++) {

					if ((gcpAttachmentLastTime != null) && (!gcpAttachmentLastTime.isEmpty())) {
						if (jiraAttachmentTimeByReporter.get(i)
								.compareTo(gcpAttachmentLastTime.get(gcpAttachmentLastTime.size() - 1)) > 0) {

							String sourceURL = jiraAttachmentContent.get(i);
							HttpURLConnection conn = (HttpURLConnection) (new URL(sourceURL)).openConnection();
							String userpass = properties.getProperty("username") + ":"
									+ properties.getProperty("password");
							String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes()));
							conn.setRequestProperty("Authorization", basicAuth);
							conn.setRequestMethod("GET");
							conn.setRequestProperty("Accept", "application/json");
							if (conn.getResponseCode() != 200) {
								throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
							}
							String fileName = jiraAttachmentName.get(i).trim();
							System.out.println("Filename of attachment to be uploaded: " + fileName);
							targetPath = new File(dirName + File.separator + fileName).toPath();
							System.out.println("Target path: " + targetPath);
							Files.copy(conn.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

							MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
							String attachmentFileMimeType = mimeTypesMap.getContentType(fileName);
							String attachmentFilePath = dirName+fileName;
							System.out.println("attachmentFilePath:" + attachmentFilePath);
							// ** Add attachment to ticket **//
							byte[] fileContents = Utils.readFileContentsInBytes(attachmentFilePath);
							Attachment attachmentCreated = TicketAttachment.addAttachmentToTicket(gcpSupportCaseNo,
									fileName, fileContents, fileContents.length, attachmentFileMimeType);
							Files.deleteIfExists(targetPath);
						}
					} else {

						String sourceURL = jiraAttachmentContent.get(i);
						HttpURLConnection conn = (HttpURLConnection) (new URL(sourceURL)).openConnection();
						String userpass = properties.getProperty("username") + ":" + properties.getProperty("password");
						String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes()));
						conn.setRequestProperty("Authorization", basicAuth);
						conn.setRequestMethod("GET");
						conn.setRequestProperty("Accept", "application/json");
						if (conn.getResponseCode() != 200) {
							throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
						}
						String fileName = jiraAttachmentName.get(i).trim();
						System.out.println("Filename of attachment to be uploaded: " + fileName);
						targetPath = new File(dirName + File.separator + fileName).toPath();
						System.out.println("Target path: " + targetPath);
						Files.copy(conn.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

						MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
						String attachmentFileMimeType = mimeTypesMap.getContentType(fileName);
						String attachmentFilePath = dirName+fileName;
						System.out.println("attachmentFilePath:" + attachmentFilePath);
						// ** Add attachment to ticket **//
						byte[] fileContents = Utils.readFileContentsInBytes(attachmentFilePath);
						Attachment attachmentCreated = TicketAttachment.addAttachmentToTicket(gcpSupportCaseNo,
								fileName, fileContents, fileContents.length, attachmentFileMimeType);
						Files.deleteIfExists(targetPath);
					}
				}
			}

			/* For Getting GCP attachment to JIRA */
			if ((gcpAttachmentTimeBySupport != null) && (!gcpAttachmentTimeBySupport.isEmpty())) {
				for (int i = 0; i < gcpAttachmentTimeBySupport.size(); i++) {
					if ((jiraAttachmentLastTime) != null && (!jiraAttachmentLastTime.isEmpty())) {
						if (gcpAttachmentTimeBySupport.get(i)
								.compareTo(jiraAttachmentLastTime.get(jiraAttachmentLastTime.size() - 1)) > 0) {
							for (String sourceURL : gcloudattachmenturlWithTimeBySupportEngineer
									.get(gcpAttachmentTimeBySupport.get(i))) {
								System.out.println("Source URL:" + sourceURL);
								for (String gcpAttachmentNameWithExtension : gcpAttachmentFileNameWithTimeBySupport
										.get(gcpAttachmentTimeBySupport.get(i))) {
									System.out.println("File name:" + gcpAttachmentNameWithExtension);
									String extension = gcpAttachmentNameWithExtension
											.substring(gcpAttachmentNameWithExtension.indexOf(".") + 1);
									targetPath = new File(dirName + File.separator + gcpAttachmentNameWithExtension)
											.toPath();
									System.out.println("targetPath: " + targetPath);

									FileUtils.copyURLToFile(new URL(sourceURL),
											new File(dirName + gcpAttachmentNameWithExtension), 50000, 50000);

									File fileUpload = new File(dirName + gcpAttachmentNameWithExtension);
									HttpClient httpClient = HttpClientBuilder.create().build();
									HttpPost postRequest = new HttpPost(postAttachmentToJira);
									String userpass = properties.getProperty("username") + ":"
											+ properties.getProperty("password");
									String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
									postRequest.setHeader("Authorization", basicAuth);
									postRequest.setHeader("X-Atlassian-Token", "nocheck");
									MultipartEntityBuilder entity = MultipartEntityBuilder.create();
									entity.addPart("file", new FileBody(fileUpload));
									postRequest.setEntity(entity.build());
									HttpResponse jiraResponse = null;
									try {
										jiraResponse = httpClient.execute(postRequest);
									} catch (ClientProtocolException f) {
										f.printStackTrace();
									} catch (IOException f) {
										f.printStackTrace();
									}
									if (jiraResponse.getStatusLine().getStatusCode() == 200) {

										if (extension.trim().equalsIgnoreCase("jpg")
												|| extension.trim().equalsIgnoreCase("png")) {
											String attachmentName = "!" + gcpAttachmentNameWithExtension + "!";
											Thread.sleep(2000);
											myJiraClient.addComment(issue, attachmentName.trim());
											Thread.sleep(2000);
											System.out.println("Adding attachment to JIRA Issue passed");
										} else {
											String attachmentName = "[^" + gcpAttachmentNameWithExtension + "]";
											Thread.sleep(2000);
											myJiraClient.addComment(issue, attachmentName.trim());
											Thread.sleep(2000);
											System.out.println("Adding attachment to JIRA Issue passed");
										}
										Files.deleteIfExists(targetPath);
									} else {
										System.out.println("Adding attachment to JIRA Issue failed");
									}
								}
							}
						}
					} else {
						for (String sourceURL : gcloudattachmenturlWithTimeBySupportEngineer
								.get(gcpAttachmentTimeBySupport.get(i))) {
							for (String gcpAttachmentNameWithExtension : gcpAttachmentFileNameWithTimeBySupport
									.get(gcpAttachmentTimeBySupport.get(i))) {
								String extension = gcpAttachmentNameWithExtension
										.substring(gcpAttachmentNameWithExtension.indexOf(".") + 1);
								targetPath = new File(dirName + File.separator + gcpAttachmentNameWithExtension)
										.toPath();
								System.out.println("targetPath: " + targetPath);

								FileUtils.copyURLToFile(new URL(sourceURL),
										new File(dirName + gcpAttachmentNameWithExtension), 50000, 50000);

								File fileUpload = new File(dirName + gcpAttachmentNameWithExtension);
								HttpClient httpClient = HttpClientBuilder.create().build();
								HttpPost postRequest = new HttpPost(postAttachmentToJira);
								String userpass = properties.getProperty("username") + ":"
										+ properties.getProperty("password");
								String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
								postRequest.setHeader("Authorization", basicAuth);
								postRequest.setHeader("X-Atlassian-Token", "nocheck");
								MultipartEntityBuilder entity = MultipartEntityBuilder.create();
								entity.addPart("file", new FileBody(fileUpload));
								postRequest.setEntity(entity.build());
								HttpResponse jiraResponse = null;
								try {
									jiraResponse = httpClient.execute(postRequest);
								} catch (ClientProtocolException f) {
									f.printStackTrace();
								} catch (IOException f) {
									f.printStackTrace();
								}
								if (jiraResponse.getStatusLine().getStatusCode() == 200) {

									if (extension.trim().equalsIgnoreCase("jpg")
											|| extension.trim().equalsIgnoreCase("png")) {
										String attachmentName = "!" + gcpAttachmentNameWithExtension + "!";
										Thread.sleep(2000);
										myJiraClient.addComment(issue, attachmentName.trim());
										Thread.sleep(2000);
										System.out.println("Adding attachment to JIRA Issue passed");
									} else {
										String attachmentName = "[^" + gcpAttachmentNameWithExtension + "]";
										Thread.sleep(2000);
										myJiraClient.addComment(issue, attachmentName.trim());
										Thread.sleep(2000);
										System.out.println("Adding attachment to JIRA Issue passed");
									}
									Files.deleteIfExists(targetPath);
								} else {
									System.out.println("Adding attachment to JIRA Issue failed");
								}
							}
						}
					}
				}
			}
		}
	}

	private JiraRestClient getJiraRestClient() {
		return new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(getJiraUri(), this.username,
				this.password);
	}

	private URI getJiraUri() {
		return URI.create(this.jiraUrl);
	}

	private Iterable<Issue> getAllIssue(String jql, int maxPerQuery, int startIndex) {
		return restClient.getSearchClient().searchJql(jql, maxPerQuery, startIndex, null).claim().getIssues();
	}

	private void addComment(Issue issue, String commentBody) {
		restClient.getIssueClient().addComment(issue.getCommentsUri(), Comment.valueOf(commentBody));
	}
}
