/*
 * Copyright 2015 search-solutions.net
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package net.sr_sl.solr.ssq;

import java.io.IOException;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * search-solutions.net implementation of a QueryComponent to allow for
 * server-side query templates.
 * </p>
 * <p/>
 * <b>This API is experimental and may change in the future.</b>
 * 
 * @since solr 4.7
 * @author Paul
 * 
 */
public class SsqQueryComponent extends QueryComponent {
	public static final String SSQ_DELIM = ".";
	public static final String SSQ_PREFIX = "ssq";
	public static final String SSQ_QUERY = "query";
	public static final String SSQ_QUERYSTRING = "querystring";
	public static final String SSQ_PARAM = "param";
	public static final String SSQ_PARAM_DFT = "qq";
	public static final String SSQ_APPLIED_SUFFIX = SSQ_DELIM + "applied";

	private static final Logger LOG = LoggerFactory
			.getLogger(SsqQueryComponent.class);

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		boolean isReplaced = false;

		// modify query request
		isReplaced = modifyQueryRequest(rb);

		if (isReplaced)
			LOG.debug("Modified query request:" + rb.req.getParams());

		// perform the standard prepare method
		super.prepare(rb);

		// Restore the query parameters
		if (isReplaced)
			restoreQueryRequest(rb);
	}

	/**
	 * Modify the query parameters
	 */
	private boolean modifyQueryRequest(ResponseBuilder rb) throws IOException {

		SolrQueryRequest req = rb.req;
		SolrParams params = req.getParams();

		// check whether server side queries is active for this request
		if (!params.getBool(SSQ_PREFIX, false))
			return false;

		// get parameters to use
		String ssqQuery = params.get(SSQ_PREFIX.concat(SSQ_DELIM).concat(
				SSQ_QUERY));
		String ssqParam = params.get(
				SSQ_PREFIX.concat(SSQ_DELIM).concat(SSQ_PARAM), SSQ_PARAM_DFT);

		// when ssqQuery or ssqParam is not set, don't modify
		if (ssqQuery == null || ssqQuery.isEmpty() || ssqParam.isEmpty())
			return false;

		// Get original value for ssqParam and return when already set
		String ssqParamVal = params.get(ssqParam);
		if (ssqParamVal != null && !ssqParamVal.isEmpty())
			return false;

		// Get original query string value
		String origQueryString = rb.getQueryString();
		String origQVal = req.getOriginalParams().get(CommonParams.Q);

		// Retrieve value to use as query-term; when empty, use q.alt
		String qVal = origQVal;
		if (qVal == null || qVal.isEmpty()) {
			String alt_q = params.get(DisMaxParams.ALTQ);
			if (alt_q != null && !alt_q.isEmpty()) {
				qVal = alt_q;
			}
		}

		// Get value for ssqQuery
		String ssqQueryVal = params.get(SSQ_PREFIX.concat(SSQ_DELIM)
				.concat(SSQ_QUERY).concat(SSQ_DELIM).concat(ssqQuery));
		// When value not found, assume that ssqQuery is the query to execute
		// per default
		if (ssqQueryVal == null || ssqQueryVal.isEmpty())
			ssqQueryVal = ssqQuery;

		// Perform replacement
		ModifiableSolrParams mparams = new ModifiableSolrParams();

		// Set flag to indicate that replacement is performed
		mparams.set(SSQ_PREFIX.concat(SSQ_APPLIED_SUFFIX),
				Boolean.toString(true));

		// Store original querystring when <> q
		if (origQVal != null && !origQVal.equals(origQueryString))
			mparams.set(SSQ_PREFIX.concat(SSQ_DELIM).concat(SSQ_QUERYSTRING)
					.concat(SSQ_APPLIED_SUFFIX), origQueryString);

		// Perform the switch (qVal --> ssqParam)
		mparams.set(ssqParam, qVal);
		mparams.set(
				SSQ_PREFIX.concat(SSQ_DELIM).concat(SSQ_QUERY)
						.concat(SSQ_APPLIED_SUFFIX), ssqQueryVal);

		// set the extra parameters
		req.setParams(SolrParams.wrapAppended(req.getParams(), mparams));

		// set queryString to query
		rb.setQueryString(ssqQueryVal);

		return true;
	}

	/**
	 * Restore the query parameters Restore queryString and reset original q (to
	 * ensure further components will be processed correctly)
	 */
	private boolean restoreQueryRequest(ResponseBuilder rb) {

		SolrQueryRequest req = rb.req;
		SolrParams params = req.getParams();

		// check whether server side queries is active for this request
		if (!params.getBool(SSQ_PREFIX, false))
			return false;

		// check whether values where replaced
		if (!params.getBool(SSQ_PREFIX.concat(SSQ_APPLIED_SUFFIX), false))
			return false;

		// retrieve original queryString and q
		String queryString = params.get(SSQ_PREFIX.concat(SSQ_DELIM)
				.concat(SSQ_QUERYSTRING).concat(SSQ_APPLIED_SUFFIX));
		boolean useStoredQueryString = (queryString != null && queryString
				.isEmpty());

		// remove flag that was possibly set to store original queryString
		if (!useStoredQueryString) {
			ModifiableSolrParams mparams = new ModifiableSolrParams(params);
			mparams.remove(SSQ_PREFIX.concat(SSQ_DELIM).concat(SSQ_QUERYSTRING)
					.concat(SSQ_APPLIED_SUFFIX));
			req.setParams(mparams);
		} else {
			queryString = req.getOriginalParams().get(CommonParams.Q);
		}

		// set queryString to original query
		rb.setQueryString(queryString);

		return true;
	}

}
