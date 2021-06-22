package com.grame.services.queries.meta;

/*-
 * ‌
 * grame Services Node
 * ​
 * Copyright (C) 2018 - 2021 grame grame, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.ActiveVersions;
import com.grame.services.context.properties.SemanticVersions;
import com.grame.services.queries.AbstractAnswer;
import com.grame.services.utils.EntityIdUtils;
import com.gramegrame.api.proto.java.NetworkGetVersionInfoResponse;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.SemanticVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static com.gramegrame.api.proto.java.grameFunctionality.GetVersionInfo;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public class GetVersionInfoAnswer extends AbstractAnswer {
	private static final Logger log = LogManager.getLogger(GetVersionInfoAnswer.class);

	private final SemanticVersions semanticVersions;

	public GetVersionInfoAnswer(SemanticVersions semanticVersions) {
		super(
				GetVersionInfo,
				query -> query.getNetworkGetVersionInfo().getHeader().getPayment(),
				query -> query.getNetworkGetVersionInfo().getHeader().getResponseType(),
				response -> response.getNetworkGetVersionInfo().getHeader().getNodeTransactionPrecheckCode(),
				(query, view) -> semanticVersions.getDeployed().isPresent() ? OK : FAIL_INVALID);
		this.semanticVersions = semanticVersions;
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		var op = query.getNetworkGetVersionInfo();
		var response = NetworkGetVersionInfoResponse.newBuilder();

		ResponseType type = op.getHeader().getResponseType();
		if (validity != OK) {
			response.setHeader(header(validity, type, cost));
		} else {
			if (type == COST_ANSWER) {
				response.setHeader(costAnswerHeader(OK, cost));
			} else {
				response.setHeader(answerOnlyHeader(OK));
				var answer = semanticVersions.getDeployed().get();
				response.setHapiProtoVersion(answer.protoSemVer());
				response.setgrameServicesVersion(answer.grameSemVer());
			}
		}

		return Response.newBuilder()
				.setNetworkGetVersionInfo(response)
				.build();
	}
}
