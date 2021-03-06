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

import java.util.List;

import com.google.cloudsupport.v1alpha2.model.CloudSupportCase;

public class CloudSupportCaseBuilder {
	private String displayName;
	private String projectId;
	private String state = "NEW"; // default
	private String priority ; // default
	private String component;
	private String description;
	//private String subcomponent = "Availability / Latency";

	private List<String> ccList;
	private boolean testCaseFlag;

	public CloudSupportCaseBuilder(String displayName,String description, String projectId,String component) {
		this.displayName = displayName;
		this.description=description;
		this.projectId = projectId;
		this.component =component;
		
	}

	public CloudSupportCaseBuilder setState(String state) {
		this.state = state;
		return this;
	}

	public CloudSupportCaseBuilder setPriority(String priority) {
		this.priority = priority;
		return this;
	}

	public CloudSupportCaseBuilder setCCList(List<String> ccList) {
		this.ccList = ccList;
		return this;
	}

	public CloudSupportCaseBuilder setTestCaseFlag(boolean testCaseFlag) {
		this.testCaseFlag = testCaseFlag;
		return this;
	}

	public CloudSupportCase build() {
		CloudSupportCase supportCase = new CloudSupportCase();
		supportCase.setDisplayName(displayName);
		supportCase.setDescription(description);
		supportCase.setProjectId(projectId);
		supportCase.setState(state);
		supportCase.setPriority(priority);
		supportCase.setCcAddresses(ccList);
		supportCase.setTestCase(testCaseFlag);
		supportCase.setComponent(component);
		//supportCase.setSubcomponent(subcomponent);

		return supportCase;
	}
}
