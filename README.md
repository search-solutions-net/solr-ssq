# solr-ssq - Solr Server Side Query templates

https://www.search-solutions.net

## Purpose
The purpose of this component is to store solr queries in the solr (server side) configuration.
This hides solr query complexity for the clients and avoids the distribution of complex queries on client side.

See further info on https://www.search-solutions.net/en/blog/solr-server-side-query-templates.

## Installation
1. Add jar file (solr-ssq-VERSION.jar for Solr v5) in the "./lib" sub-directory of the Solr Home Directory (e.g. /solr-5.0.0/server/solr/collection1). For any other version solr version, generate your own jar.

2. Add the following line in the solrconfig.xml to load the jar
	  <lib dir="${solr.home.dir:}/lib/" regex="solr-ssq-\d.*\.jar" />
	  
3. Add the following line in the solrconfig.xml to set the searchcomponent
	  <searchComponent name="query" class="net.sr_sl.solr.ssq.SsqQueryComponent" />
	  
4. Configure the parameters for requestHandler (optionally via initParams (in case of Solr 5.0)) e.g.
  1. Example with simple setup
  
    ```
    <lst name="defaults">
	  ...
	  <str name="ssq">on</str>
      <str name="ssq.query">{!edismax qf='subject^10.0 content flags' v=$qq}</str>
    </lst>
    ```

  2. Extended example with several ssq

    ```
    <lst name="defaults">
	  ...
	  <str name="ssq">on</str>
      <str name="ssq.query">content</str>
      <str name="ssq.query.subject">{!edismax qf='subject^10.0 content flags' v=$qq}</str>
      <str name="ssq.query.content">{!edismax qf='subject content^10.0 flags' v=$qq}</str>
      <str name="ssq.query.flags">{!edismax qf='subject content flags^10.0' v=$qq}</str>
    </lst>
    ```
    
5. Perform your queries. Based on example 2 setup above, the executed query (under the hood) will be:

    ```
    /select?q=foo
		--> /select?q={!edismax qf='subject content^10.0 flags' v=$qq}&qq=foo
		   
	/select?q=foo&ssq.query=flags
		--> /select?q={!edismax qf='subject content flags^10.0' v=$qq}&qq=foo
		   
	/select?q=foo&ssq.query={!query ...}
		--> /select?q={!query ...}&qq=foo   
    ```
    
The parameter name qq can optionally be changed to another arbitrary (valid parameter name) value via configuration of the parameter:
    ```
    <str name="ssq.param">qqq</str>
    ```
When the qq parameter (or the parameter defined in "ssq.param") is set in the query request by the user/client, than the ssq query template will not be applied 
(as qq parameter needs to be set to the query-terms for the query template to work).

	/select?q=foo&qq=bar&ssq.query={!query ...}
		--> /select?q=foo 

To verify whether the ssq functionality was applied on your request, review the request parameters (via &echoParams=all) in the response. 
The following (request) parameters will additionally be set when ssq functionality was applied:

    "ssq.applied": "true",
    "qq": <set-to-value-of-original-q>,
    "ssq.query.applied": "{!edismax qf='subject content^10.0 flags' v=$qq}"


## Version notes
The jar file is compiled with java version jdk1.8 on top of Solr 5.0. 
But this component can be compiled this component against all Solr versions from 4.x onwards.

## License
Released under the Apache License, Version 2.0

2015 search-solutions.net
