// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.googlesource.gerrit.plugins.its.redmine;

import com.taskadapter.redmineapi.NotFoundException;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.User;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class RedmineClient {
	private static final String GERRIT_CONFIG_URL = "url";
	private static final String GERRIT_CONFIG_API_KEY = "apiKey";
	private RedmineManager mgr;

	private Logger log = LoggerFactory.getLogger(RedmineClient.class);

	private RedmineClient() {
		throw new UnsupportedOperationException();
	}

	public RedmineClient(final String url, String apiAccessKey) {
		mgr = RedmineManagerFactory.createWithApiKey(url, apiAccessKey);
	}

	public void isRedmineConnectSuccessful() throws RedmineException {
		mgr.getUserManager().getCurrentUser();
	}

	public void updateIssue(final String issueId, final String comment) throws IOException {
		try {
			Issue issue = IssueFactory.create(convertIssueId(issueId));
			issue.setNotes(comment);
			mgr.getIssueManager().update(issue);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public boolean exists(final String issueId) throws IOException {
		try {
			return mgr.getIssueManager().getIssueById(convertIssueId(issueId)) != null;
		} catch (NotFoundException e) {
			if (log.isDebugEnabled()) {
				log.debug("Issue {} doesn't exit: {}", issueId, e.getMessage(), e);
			}
			return false;
		} catch (RedmineException e) {
			log.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}

	public void doPerformAction(final String issueKey, final String action) throws IOException, RedmineException {
		if (isSetStatusAction(issueKey, action)) {
			return;
		}
		throw new RedmineException("Action " + action + " not executable on issue " + issueKey);
	}

	private boolean isSetStatusAction(String issueKey, String action) throws RedmineException, IOException {
		action = action.trim();
		String[] setStatusActions = new String[] { "set-status", "set_status", "setStatus" };
		String status = null;
		for (String item : setStatusActions) {
			if (action.startsWith(item)) {
				status = action.substring(item.length()).trim();
				if (status.isEmpty()) {
					throw new RedmineException("Status is empty!");
				}
				break;
			}
		}
		if (status == null) {
			return false;
		}
		Integer statusId = getStatusId(status);
		if (statusId != null) {
			log.debug("Executing action setStatus {} on issue {}", status, issueKey);
			Issue issue = IssueFactory.create(convertIssueId(issueKey));
			issue.setStatusId(statusId);
			mgr.getIssueManager().update(issue);
		} else {
			log.error("Status {} not found", status);
			throw new RedmineException("Action " + action + " not executable on issue " + issueKey);
		}
		return true;
	}

	public String healthCheckAccess() throws IOException {
		try {
			User user = mgr.getUserManager().getCurrentUser();
			final String result = "{\"status\"=\"ok\",\"username\"=\"" + user.getLogin() + "\"}";
			log.debug("Healtheck on access result: {}", result);
			return result;
		} catch (RedmineException e) {
			throw new IOException(e);
		}
	}

	public String healthCheckSysinfo(final String url) throws IOException {
		try {
			mgr.getUserManager().getCurrentUser();
			final String result = "{\"status\"=\"ok\",\"system\"=\"Redmine\",\"url\"=\"" + url + "\"}";
			log.debug("Healtheck on sysinfo result: {}", result);
			return result;
		} catch (RedmineException e) {
			throw new IOException(e);
		}
	}

	private Integer getStatusId(String status) throws RedmineException {
		int statusId = -1;
		if (isStatusId(status)) {
			statusId = Integer.valueOf(status);
		}
		for (IssueStatus issueStatus : mgr.getIssueManager().getStatuses()) {
			log.debug("IssueStatus: {}", issueStatus);
			if (status.equalsIgnoreCase(issueStatus.getName())) {
				return issueStatus.getId();
			}
			if (statusId == issueStatus.getId()) {
				return issueStatus.getId();
			}
		}
		return null;
	}

	private Integer convertIssueId(String issueId) throws IOException {
		if (!issueIdIsValid(issueId)) {
			log.warn("Issue {} is not a valid issue id", issueId);
			throw new IOException("Issue " + issueId + " is not a valid issue id");
		}
		return Integer.valueOf(issueId);
	}

	private boolean issueIdIsValid(String issueId) {
		return issueId != null && issueId.matches("^\\d+$");
	}

	private boolean isStatusId(String status) {
		return status != null && status.matches("^\\d+$");
	}
}
