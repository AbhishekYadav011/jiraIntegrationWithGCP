/**
 * 
 */
package com.google.cloud.support.integration.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.codehaus.jettison.json.JSONException;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.cloud.support.integration.utils.TicketReader;
import com.google.cloud.support.integration.utils.TicketUpdate;
import com.google.cloudsupport.v1alpha2.model.CloudSupportCase;

/**
 * @author I334554
 *
 */
public class BiDirectionalStatus {
	private static final String jql = "project = MCO AND \"GCP Support Case Title\" is not EMPTY AND \"GCP Support Ticket Status\" = \"In progress\"";
	private String username;
	private String password;
	private String jiraUrl;
	private JiraRestClient restClient;
	private static String propertiesFile = System.getenv("PROPERTIES_FILE");

	private BiDirectionalStatus(String username, String password, String jiraUrl) {
		this.username = username;
		this.password = password;
		this.jiraUrl = jiraUrl;
		this.restClient = getJiraRestClient();

	}
	public static void main(String[] args) throws FileNotFoundException, IOException, JSONException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));
		BiDirectionalStatus myJiraClient = new BiDirectionalStatus(properties.getProperty("username"),
				properties.getProperty("password"), "https://jira.multicloud.int.sap");

		// Issue issue = myJiraClient.getIssue(issueKey);
		Iterable<Issue> issues = myJiraClient.getAllIssue(jql, 100, 0);
		for (Issue issue : issues) {
			System.out.println(issue.getKey());
			int resolveTicketIssueTransactionID = 0;
			String gcpSupportCaseNo = "supportAccounts/gcp-sa-1039549927/cases/"+issue.getFieldByName("GCP Support Case Number").getValue().toString().trim();
			System.out.println("Support Case number:" + gcpSupportCaseNo);
			String jiraticketpriority = issue.getPriority().getName().trim();
			System.out.println("priority:"+jiraticketpriority);
			String jiraIssueStatus = issue.getStatus().getName().trim();
			System.out.println("Jira Issue Status:"+jiraIssueStatus);
			/*String json = issue.getFieldByName("GCP Support Case Priority").getValue().toString();
			JSONObject obj = new JSONObject(json);
			String gcpSupportPriorityInJiraTicket = obj.getString("value").trim();
			System.out.println("GCP Support Case Priority in JIRA:" + gcpSupportPriorityInJiraTicket);*/
			
			CloudSupportCase ticketRead= TicketReader.getTicket(gcpSupportCaseNo);
			String gcpSupportPriority=ticketRead.getPriority().trim();
			System.out.println("GCP Support Case Priority :" + gcpSupportPriority);
			
			
			/*Setting priority of GCP Support ticket*/
			if(!jiraticketpriority.equalsIgnoreCase(gcpSupportPriority))
			{
				CloudSupportCase ticketupdate=TicketUpdate.updateTicketPriority(gcpSupportCaseNo, jiraticketpriority);
				System.out.println("Ticket Priority after update:"+ticketupdate.getPriority());
			}
			
			/*if(!gcpSupportPriority.equalsIgnoreCase(gcpSupportPriorityInJiraTicket))
			{
				myJiraClient.updateGCPTicketStatus(issue.getKey(), "customfield_11708", "value", gcpSupportPriority);
				Thread.sleep(2000);
				System.out.println("Ticket priority to jira Updated successfully");
			}*/
			
			/** checking resolved or closed Jira Ticket */
			if (jiraIssueStatus.equalsIgnoreCase("Resolved") || jiraIssueStatus.equalsIgnoreCase("Closed")) {
				CloudSupportCase ticketupdate=TicketUpdate.updateTicketState(gcpSupportCaseNo,"CLOSED");
				System.out.println("Ticket Status after update:"+ticketupdate.getState());
				myJiraClient.updateGCPTicketStatus(issue.getKey(), "customfield_11709", "value", "Confirmed");
				Thread.sleep(1000);
				System.out.println("Ticket Status in Jira updated successfully");
			}
			
			/**
			 * For checking closed status of GCP support ticket and changing Jira Ticket Status
			 * to RESOLVED
			 */	
			if (jiraIssueStatus.equalsIgnoreCase("Waiting for customer")){
				
				String gcpSupportStatus =ticketRead.getState();
				if(gcpSupportStatus.equalsIgnoreCase("CLOSED")) {
					Iterable<Transition> transitions = myJiraClient.getTransitionByName(issue);
					for (Transition t : transitions) {
						if (t.getName().equalsIgnoreCase("Resolve Issue")) {
							resolveTicketIssueTransactionID = t.getId();

						}
					}
					System.out.println("resolveTicketIssueTransactionID :" + resolveTicketIssueTransactionID);
					System.out.println(issue.getStatus().getName());
					myJiraClient.setTransitionToResolve(issue, resolveTicketIssueTransactionID);
				}
					
			}
		}
	}
	private void setTransitionToResolve(Issue issue, int resolveTicketIssueTransactionID) {
		TransitionInput tinput = new TransitionInput(resolveTicketIssueTransactionID);
		restClient.getIssueClient().transition(issue, tinput).claim();
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

	private URI getJiraUri() {
		return URI.create(this.jiraUrl);
	}

	private Iterable<Issue> getAllIssue(String jql, int maxPerQuery, int startIndex) {
		return restClient.getSearchClient().searchJql(jql, maxPerQuery, startIndex, null).claim().getIssues();
	}
	
	private Iterable<Transition> getTransitionByName(Issue issue) {
		return restClient.getIssueClient().getTransitions(issue.getTransitionsUri()).claim();
	}
}
