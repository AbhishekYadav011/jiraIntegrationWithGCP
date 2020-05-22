/**
 * 
 */
package com.google.cloud.support.integration.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.cloud.support.integration.utils.TicketComment;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * @author I334554
 *
 */
public class BiDirectionalComments {

	private static final String jql = "project = MCO AND \"GCP Support Case Title\" is not EMPTY AND \"GCP Support Ticket Status\" = \"In progress\"";
	private String username;
	private String password;
	private String jiraUrl;
	private JiraRestClient restClient;
	private static String propertiesFile = System.getenv("PROPERTIES_FILE");

	private BiDirectionalComments(String username, String password, String jiraUrl) {
		this.username = username;
		this.password = password;
		this.jiraUrl = jiraUrl;
		this.restClient = getJiraRestClient();

	}

	public static void main(String[] args) throws JSONException, IOException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));
		// TODO Auto-generated method stub
		BiDirectionalComments myJiraClient = new BiDirectionalComments(properties.getProperty("username"),
				properties.getProperty("password"), "https://jira.multicloud.int.sap");

		// Issue issue = myJiraClient.getIssue(issueKey);
		Iterable<Issue> issues = myJiraClient.getAllIssue(jql, 100, 0);
		for (Issue issue : issues) {
			System.out.println(issue.getKey());
			String gcpSupportCaseNo = "supportAccounts/gcp-sa-1039549927/cases/"+issue.getFieldByName("GCP Support Case Number").getValue().toString().trim();
			System.out.println("Support Case number:" + gcpSupportCaseNo);

			/* Jira comments by SD ticket reporter */
			List<Comment> jiraComments = myJiraClient.getAllComments(issue.getKey());
			List<Timestamp> jiraIssueCommentTimeByReporter = new ArrayList<Timestamp>();
			List<String> jiraIssueCommentsByReporter = new ArrayList<String>();
			List<Timestamp> jiraIssueLastCommentTime = new ArrayList<Timestamp>();

			/* GCP comments by support Engineer */
			List<com.google.cloudsupport.v1alpha2.model.Comment> gcpCloudSupportPortalComments = TicketComment
					.getTicketComments(gcpSupportCaseNo);

			Multimap<Timestamp, String> gcloudcommentWithTimeBySupportEngineer = ArrayListMultimap.create();
			List<String> gcpCloudSupportPortalCommentsBySupportEngineer = new ArrayList<String>();
			List<Timestamp> gcpCloudCommentTimeBySupportEngineer = new ArrayList<Timestamp>();
			List<Timestamp> gcpCloudLastCommentTime = new ArrayList<Timestamp>();

			String Root_URI = "https://jira.multicloud.int.sap/rest/servicedeskapi/request/";
			String issueRestAPIURL = Root_URI + issue.getKey() + "/comment/";

			RequestSpecification restrequest = RestAssured.given().auth().preemptive()
					.basic(properties.getProperty("username"), properties.getProperty("password"));

			if ((jiraComments != null) && (!jiraComments.isEmpty())) {
				for (Comment c : jiraComments) {
					String commentRestApiURL = issueRestAPIURL + c.getId();
					// System.out.println(commentRestApiURL);

					Response response = restrequest.given().when().get(commentRestApiURL);
					JSONObject jsonObject = new JSONObject(response.asString().trim());
					String commentBody = jsonObject.get("body").toString().trim();
					char firstCharOfComment = commentBody.charAt(0);
					char lastCharOfComment = commentBody.charAt(commentBody.length() - 1);
					
					if (!c.getAuthor().getDisplayName().contains("GCP Support Engineer")) {
						if ((Boolean) (jsonObject.get("public"))) {
							if (!((firstCharOfComment == lastCharOfComment)
									|| (firstCharOfComment == '[' && lastCharOfComment == ']')||(firstCharOfComment == '['))) {
								jiraIssueCommentTimeByReporter.add(new Timestamp(c.getCreationDate().getMillis()));
								jiraIssueCommentsByReporter.add(c.getBody());
								// System.out.println("Finalcheck: " + c.getBody());
							}
						}
					}
					if (c.getAuthor().getDisplayName().contains("GCP Support Engineer")) {
						if (!((firstCharOfComment == lastCharOfComment)
								|| (firstCharOfComment == '[' && lastCharOfComment == ']')||(firstCharOfComment == '['))) {
							jiraIssueLastCommentTime.add(new Timestamp(c.getCreationDate().getMillis()));
						}
					}
				}
			}
			for(int i=0;i<jiraIssueLastCommentTime.size();i++) {
				System.out.println("jiraIssueLastCommentTime:"+jiraIssueLastCommentTime.get(i));
			}
			
            /*For getting comment time by GCP support engineer*/
			if ((gcpCloudSupportPortalComments != null) && (!gcpCloudSupportPortalComments.isEmpty())) {
				for (com.google.cloudsupport.v1alpha2.model.Comment sec : gcpCloudSupportPortalComments) {

					String time = sec.getCreateTime().replace("T", " ");
					Timestamp ts = Timestamp.valueOf(time.substring(0, time.length() - 1));
					/* For setting up time to GMT+1 */
					//ts.setTime(ts.getTime() + (1 * 60 * 60) * 1000);
					//System.out.println(ts);
					if (!sec.getAuthor().equals("Portal User")) {
						gcloudcommentWithTimeBySupportEngineer.put(ts, sec.getText());
						gcpCloudSupportPortalCommentsBySupportEngineer.add(sec.getText());
						gcpCloudCommentTimeBySupportEngineer.add(ts);

					}
					else {
						/* adding last comment time in support portal */
						gcpCloudLastCommentTime.add(ts);
					}
					
				}
				Collections.sort(gcpCloudCommentTimeBySupportEngineer);
				Collections.sort(gcpCloudLastCommentTime);
			}
			for(int i=0;i<gcpCloudCommentTimeBySupportEngineer.size();i++) {
				System.out.println("gcpCloudCommentTimeBySupportEngineer:"+gcpCloudCommentTimeBySupportEngineer.get(i));
			}
			/* For getting comments from JIRA to GCP */
			if ((jiraIssueCommentTimeByReporter != null) && (!jiraIssueCommentsByReporter.isEmpty())) {
				System.out.println("Total comments by Jira reporter" + jiraIssueCommentsByReporter.size());
				for (int i = 0; i < jiraIssueCommentsByReporter.size(); i++) {
					
					if ((gcpCloudLastCommentTime != null) && (!gcpCloudLastCommentTime.isEmpty())) {
						if (jiraIssueCommentTimeByReporter.get(i)
								.compareTo(gcpCloudLastCommentTime.get(gcpCloudLastCommentTime.size() - 1)) > 0) {
							System.out
									.println("jiraIssueCommentTimeByReporter" + jiraIssueCommentTimeByReporter.get(i));
							System.out.println("gcpCloudLastCommentTime"
									+ gcpCloudLastCommentTime.get(gcpCloudLastCommentTime.size() - 1));
							TicketComment.addCommentToTicket(gcpSupportCaseNo, jiraIssueCommentsByReporter.get(i));
							System.out.println("Replied to GCP Ticket");
						}
					} else {
						System.out.println("I am inside else block");
						TicketComment.addCommentToTicket(gcpSupportCaseNo, jiraIssueCommentsByReporter.get(i));
						System.out.println("Replied to GCP Ticket");
					}
				}
			}
			
			/* For getting comments from GCP to JIRA */
			if ((gcpCloudCommentTimeBySupportEngineer != null) && (!gcpCloudCommentTimeBySupportEngineer.isEmpty())) {
				for (int i = 0; i < gcpCloudSupportPortalCommentsBySupportEngineer.size(); i++) {
					
					if ((jiraIssueLastCommentTime != null) && (!jiraIssueLastCommentTime.isEmpty())) {
						if (gcpCloudCommentTimeBySupportEngineer.get(i)
								.compareTo(jiraIssueLastCommentTime.get(jiraIssueLastCommentTime.size() - 1)) > 0) {
							System.out.println("Yes Support engineer comment time is greater than Jira last comment");
							for(String comment:gcloudcommentWithTimeBySupportEngineer.get(gcpCloudCommentTimeBySupportEngineer.get(i))) {
							System.out.println("Comment from GCP to Jira:"+comment);
							Thread.sleep(2000);
							myJiraClient.addComment(issue,comment );
							Thread.sleep(2000);
							System.out.println("Replied to JIRA Ticket");
							}
						}

					} else {
						Thread.sleep(2000);
						myJiraClient.addComment(issue, gcpCloudSupportPortalCommentsBySupportEngineer.get(i));
						Thread.sleep(2000);
						System.out.println("Replied to JIRA Ticket");
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

	private List<Comment> getAllComments(String issueKey) {
		return StreamSupport.stream(getIssue(issueKey).getComments().spliterator(), false).collect(Collectors.toList());
	}

	private Issue getIssue(String issueKey) {
		return restClient.getIssueClient().getIssue(issueKey).claim();
	}

	private void addComment(Issue issue, String commentBody) {
		restClient.getIssueClient().addComment(issue.getCommentsUri(), Comment.valueOf(commentBody));
	}
}
