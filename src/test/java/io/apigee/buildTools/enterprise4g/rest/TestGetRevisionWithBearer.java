/**
 * Copyright (C) 2016 Apigee Corporation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apigee.buildTools.enterprise4g.rest;

import static org.junit.Assert.*;

import org.junit.Test;

import com.apigee.mgmtapi.sdk.client.MgmtAPIClient;
import com.apigee.mgmtapi.sdk.model.AccessToken;

import java.io.IOException;

public class TestGetRevisionWithBearer extends RestClientTestBase {

	@Override
	protected Bundle createBundle() {
		return new Bundle("taskservice");
	}


	@Test
	public void testGetLatestRevision() throws IOException {
		Long latest = createClient().getLatestRevision(getBundle());
		log.info("revision number::{}", latest);
		assertNotNull(latest);
	}

	//the client can generate the token using any other plugin they choose
	private String generateAccessToken() throws Exception {
		MgmtAPIClient client = new MgmtAPIClient();
		AccessToken token = client.getAccessToken(
				"https://login.apigee.com/oauth/token",
				"edgecli", "edgeclisecret",
				System.getProperty("username"),
				System.getProperty("password"));
		return token.getAccess_token();
	}

}
