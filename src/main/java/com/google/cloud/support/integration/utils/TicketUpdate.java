/**
 * 
 */
package com.google.cloud.support.integration.utils;

import java.io.IOException;

import com.google.cloudsupport.v1alpha2.CloudSupport;
import com.google.cloudsupport.v1alpha2.model.CloudSupportCase;

/**
 * @author I334554
 *
 */
public class TicketUpdate {

	public static CloudSupportCase updateTicketPriority(String caseName, String priority) throws IOException {
		CloudSupport supportService = SupportAPIClient.getSupportService();
		CloudSupportCase cloudCase = supportService.supportAccounts().cases().get(caseName).execute();
		CloudSupportCase updateCloudCasePriority = supportService.supportAccounts().cases()
				.patch(caseName, cloudCase.setPriority(priority)).setUpdateMask("case.priority").execute();
		System.out.println("Update Priority:" + updateCloudCasePriority.getPriority());
		return updateCloudCasePriority;

	}
	
	public static CloudSupportCase updateTicketState(String caseName, String state) throws IOException {
		CloudSupport supportService = SupportAPIClient.getSupportService();
		CloudSupportCase cloudCase = supportService.supportAccounts().cases().get(caseName).execute();
		CloudSupportCase updateCloudCaseState = supportService.supportAccounts().cases()
				.patch(caseName, cloudCase.setState(state)).setUpdateMask("case.state").execute();
		System.out.println("Update Priority:" + updateCloudCaseState.getState());
		return updateCloudCaseState;

	}

}
