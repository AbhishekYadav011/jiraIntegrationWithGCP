/**
 * 
 */
package com.google.cloud.support.integration.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.cloud.support.integration.utils.CloudSupportCaseBuilder;
import com.google.cloud.support.integration.utils.TicketAttachment;
import com.google.cloud.support.integration.utils.TicketCreator;
import com.google.cloud.support.integration.utils.Utils;
import com.google.cloudsupport.v1alpha2.model.Attachment;
import com.google.cloudsupport.v1alpha2.model.CloudSupportCase;
import com.sun.jersey.core.util.Base64;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * @author I334554
 *
 */
public class CreateGcpSupportTicket {

	private static final String jql = "project = MCO AND \"GCP Support Case Title\" is not EMPTY AND \"GCP Support Ticket Status\" = \"Pending Creation\"";
	private String username;
	private String password;
	private String jiraUrl;
	private static String propertiesFile = System.getenv("PROPERTIES_FILE");
	private JiraRestClient restClient;

	private CreateGcpSupportTicket(String username, String password, String jiraUrl) {
		this.username = username;
		this.password = password;
		this.jiraUrl = jiraUrl;
		this.restClient = getJiraRestClient();

	}

	public static void main(String[] args) throws JSONException, IOException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));
		CreateGcpSupportTicket myJiraClient = new CreateGcpSupportTicket(properties.getProperty("username"),
				properties.getProperty("password"),
				"https://jira.multicloud.int.sap");

		// Issue issue = myJiraClient.getIssue(issueKey);
		Iterable<Issue> issues = myJiraClient.getAllIssue(jql, 100, 0);
		for (Issue issue : issues) {
			System.out.println(issue.getKey());
			Path targetPath;
			String dirName=properties.getProperty("dirName");
			String attachmentApi = "https://jira.multicloud.int.sap/rest/api/2/issue/";
			String issueRestAPIURL = attachmentApi + issue.getKey() + "?fields=attachment";

			List<String> jiraAttachmentContent = new ArrayList<String>();
			List<String> jiraAttachmentName = new ArrayList<String>();
			
			String title=issue.getFieldByName("GCP Support Case Title").getValue().toString();
			System.out.println("Issue Title:" + title);
			
			String projectId = issue.getFieldByName("GCP Project ID").getValue().toString();
			System.out.println("Project ID:" + projectId);

			String json = issue.getFieldByName("GCP Component").getValue().toString();
			JSONObject obj = new JSONObject(json);
			String component = obj.getString("value").trim();
			System.out.println("component: " + component);

			String priority = issue.getPriority().getName();
			System.out.println(priority);

			String ticketDesc = issue.getDescription().toString();
			System.out.println(issue.getDescription());

			/* Getting attachment from JIRA issue */

			RequestSpecification request = RestAssured.given().auth().preemptive().basic(properties.getProperty("username"),
					properties.getProperty("password"));

			Response response = request.get(issueRestAPIURL);
			JSONObject jsonObject = new JSONObject(response.asString().trim());
			// System.out.println(jsonObject.getJSONObject("fields").getJSONArray("attachment"));
			JSONArray attachmentArray = jsonObject.getJSONObject("fields").getJSONArray("attachment");
			int attachmentLength = jsonObject.getJSONObject("fields").getJSONArray("attachment").length();
			System.out.println("Total Attachment in JIRA Ticket: " + attachmentLength);

			/** For downloading Attachment at workspace **/
			if (attachmentLength != 0) {
				for (int i = 0; i < attachmentLength; i++) {
					JSONObject jsonObj = attachmentArray.getJSONObject(i);
					jiraAttachmentName.add(jsonObj.getString("filename"));
					// System.out.println("Attachment Name: " + jiraAttachmentName.get(i));

					jiraAttachmentContent.add(jsonObj.getString("content"));
					// System.out.println("Attachment URL: " + jiraAttachmentContent.get(i));
					try {
						String sourceURL = jiraAttachmentContent.get(i);

						HttpURLConnection conn = (HttpURLConnection) new URL(sourceURL).openConnection();
						String userpass = properties.getProperty("username") + ":" + properties.getProperty("password");
						String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
						conn.setRequestProperty("Authorization", basicAuth);
						conn.setRequestMethod("GET");
						conn.setRequestProperty("Accept", "application/json");
						if (conn.getResponseCode() != 200) {
							throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
						}
						// String fileName = sourceURL.substring(sourceURL.lastIndexOf('/') + 1,
						// sourceURL.length());
						String fileName = jiraAttachmentName.get(i).trim();
						System.out.println("Filename of attachment to be uploaded: " + fileName);
						targetPath = new File(dirName + File.separator + fileName).toPath();
						System.out.println("Target path: " + targetPath);
						Files.copy(conn.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

						// Files.deleteIfExists(targetPath);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}

			}
			CloudSupportCase ticketCreated = new CreateGcpSupportTicket().createTicket(projectId,title, ticketDesc, priority,
					component);
			System.out.println("CloudSupportCase Created: " + ticketCreated.getName());

			if (attachmentLength != 0) {
				for (int i = 0; i < attachmentLength; i++) {
					String fileName = jiraAttachmentName.get(i).trim();
					System.out.println("File is getting uploaded:" + fileName);
					targetPath = new File(dirName + File.separator + fileName).toPath();

					MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
					String attachmentFileMimeType = mimeTypesMap.getContentType(fileName);
					String attachmentFilePath = dirName+fileName;
					System.out.println("attachmentFilePath:" + attachmentFilePath);
					/** Add attachment to ticket **/
					byte[] fileContents = Utils.readFileContentsInBytes(attachmentFilePath);
					Attachment attachmentCreated = TicketAttachment.addAttachmentToTicket(ticketCreated.getName(),
							fileName, fileContents, fileContents.length, attachmentFileMimeType);
					Files.deleteIfExists(targetPath);
				}
			}
			System.out.println("Ticket is empty or not :" + ticketCreated.isEmpty());
			if (!ticketCreated.isEmpty()) {
				myJiraClient.updateCustomField(issue.getKey(), "customfield_11813", StringUtils.substring(ticketCreated.getName(), ticketCreated.getName().lastIndexOf("/")+1));
				Thread.sleep(4000);
				myJiraClient.updateGCPTicketStatus(issue.getKey(), "customfield_11709", "value", "In progress");
				Thread.sleep(4000);
				System.out.println("Ticket Updated successfully");
			}
		}
	}

	private CloudSupportCase createTicket(String projectId,String title, String ticketDesc, String priority, String component)
			throws IOException {
		CloudSupportCaseBuilder ticketBuilder = new CloudSupportCaseBuilder(title,ticketDesc, projectId, component);
		ticketBuilder.setPriority(priority);
		CloudSupportCase supportCase = ticketBuilder.setTestCaseFlag(true).build();
		CloudSupportCase ticketCreated = TicketCreator.createTicket(supportCase);

		return ticketCreated;
	}

	private void updateCustomField(String issueKey, String customField, String Value) {
		IssueInputBuilder builder = new IssueInputBuilder();
		builder.setFieldValue(customField, Value);
		final IssueInput issueInput = builder.build();
		try {
			restClient.getIssueClient().updateIssue(issueKey, issueInput);
		} catch (Exception e11) {
			e11.printStackTrace();
		}

	}

	private void updateGCPTicketStatus(String issueKey, String customField, String key, String value) {
		IssueInputBuilder updateInputBuilder = new IssueInputBuilder();
		updateInputBuilder.setFieldValue(customField, ComplexIssueInputFieldValue.with(key, value));
		final IssueInput issueInput = updateInputBuilder.build();
		try {
			restClient.getIssueClient().updateIssue(issueKey, issueInput);
		} catch (Exception e11) {
			e11.printStackTrace();
		}

	}

	private JiraRestClient getJiraRestClient() {
		return new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(getJiraUri(), this.username,
				this.password);
	}

	private CreateGcpSupportTicket() {

	}

	private URI getJiraUri() {
		return URI.create(this.jiraUrl);
	}

	private Iterable<Issue> getAllIssue(String jql, int maxPerQuery, int startIndex) {
		return restClient.getSearchClient().searchJql(jql, maxPerQuery, startIndex, null).claim().getIssues();
	}

}
