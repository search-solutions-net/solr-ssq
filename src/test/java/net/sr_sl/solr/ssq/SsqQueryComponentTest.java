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

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests some basic Ssq functionality of Solr 
 */
public class SsqQueryComponentTest extends SolrTestCaseJ4 {

	private static final String HEADER_XPATH = "/response/lst[@name='responseHeader']";

	public String getCoreName() {
		return "basic";
	}

	@BeforeClass
	public static void beforeTests() throws Exception {
		initCore("solr/ssq-solrconfig.xml", "solr/ssq-schema.xml");
	    lrf.args.put("wt","xml");
	    lrf.args.put("echoParams", "all");
	    lrf.args.put("debugQuery", "on");
	}

	@Test
	public void testSsqParams() {
		
		lrf.args.put("ssq","off");
		assertQ(req("foo"), HEADER_XPATH
				+ "/lst[@name='params']/str[@name='ssq'][.='off']");
		assertQ(req("foo"), HEADER_XPATH
				+ "/lst[@name='params']/str[@name='ssq.query.q1']");
	}

	@Test
	public void testSsqProcessing() {
		
		assertU(adoc("id", "1001", "val1_s", "ABC"));
		assertU(adoc("id", "1002", "val2_s", "ABC"));
		assertU(adoc("id", "1003", "val3_s", "ABC"));
		assertU(commit());
		
		assertQ("Test default query (with ssq off)", req("ABC"), "//*[@numFound='3']");

		lrf.args.put("ssq", "on");
		assertQ("Test default ssq.query (q1)", req("ABC"), "//*[@numFound='1']"
	            ,"//result/doc[1]/str[@name='id'][.='1001']");
		
		lrf.args.put("ssq.query", "_query_:{!field f=val3_s v=$qq}");
		assertQ("Test override ssq.query", req("ABC"), "//*[@numFound='1']"
	            ,"//result/doc[1]/str[@name='id'][.='1003']");
		
		lrf.args.put("ssq.query", "q1");
		assertQ("Test query q1", req("ABC"), "//*[@numFound='1']"
	            ,"//result/doc[1]/str[@name='id'][.='1001']");
		
		lrf.args.put("ssq.query", "q2");
		assertQ("Test query q2", req("ABC"), "//*[@numFound='1']"
	            ,"//result/doc[1]/str[@name='id'][.='1002']");

		lrf.args.remove("ssq");
		
	}

}
