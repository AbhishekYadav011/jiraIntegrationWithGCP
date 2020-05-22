# Integration between Jira service desk Ticket and GCP Support Portal
This repository is created for integration b/w two different platform i.e. Jira SD & GCP Support portal.

## It has four main class:
1. CreateGcpSupportTicket:
   This java class will create GCP support ticket with the content(ex. gcp project id) of service desk ticket. Service desk ticket will    be updated once the GCP Support ticket gets created.
   
2. BiDirectionalComments:
   This java class will handle comments bewtween two platform. Whne Jira users comments in jira service desk ticket that comments will      be added to GCP Support ticket and vice-versa.
   
3. BiDirectionalAttachments:
   This java class will handle attachments bewtween two platform. Whne Jira users add any attachments in jira service desk ticket that      attachment will be added to GCP Support ticket and vice-versa.
   
4. BiDirectionalStatus:
   This java class will handle stauts and priority bewtween two platform. Whne Jira users change priority/close ticket/resolve ticket in    jira service desk ticket that stauts and priority will be added to GCP Support ticket and vice-versa.
  
  
**This whole project work on __PROPERTIES_FILE__ all values needs to be completed**
# Support API Constants - DONT CHANGE
1. CLOUD_SUPPORT_API_URL = https://cloudsupport.googleapis.com/v1alpha2
2. CLOUD_SUPPORT_ATTACHMENT_URL = https://cloudsupport.googleapis.com/upload/v1/media/
3. CLOUD_SUPPORT_SCOPE = https://www.googleapis.com/auth/cloudsupport

# Client Specific Constants - PLEASE UPDATE VALUES
1. SERVICE_ACCOUNT_ID = <Need to be registered with google>
2. SERVICE_ACCOUNT_PRIVATE_KEY = <service account private key>
3. SUPPORT_ACCOUNT_ID =<>
4. username = <jira user name>
5. password = <jira user pwd>
6. dirName= <to download and upload attachment from>

**Add __PROPERTIES_FILE__ as system variable**
