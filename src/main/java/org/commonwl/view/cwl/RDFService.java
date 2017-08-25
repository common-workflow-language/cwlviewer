/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.commonwl.view.cwl;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles the parsing of CWL RDF files
 */
@Service
public class RDFService {

    // Context for SPARQL queries
    private final String queryCtx = "PREFIX cwl: <https://w3id.org/cwl/cwl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX sld: <https://w3id.org/cwl/salad#>\n" +
            "PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>\n" +
            "PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX s: <http://schema.org/>";

    private String rdfService;

    /**
     * Create the RDFService with configuration
     * @param rdfService The SPARQL endpoint from configuration
     */
    @Autowired
    public RDFService(@Value("${sparql.endpoint}") String rdfService) {
        this.rdfService = rdfService;
    }

    /**
     * Add to ontologies in the triple store
     * @param model The model to be stored
     */
    public void addToOntologies(Model model) {
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(rdfService);
        accessor.add("ontologies", model);
    }

    /**
     * Get a model from the triple store in a given format
     * @param graphName The name of the graph for the model
     * @param format The name of the writer (format to be written)
     * @return A byte array representing the model in the given format
     */
    public byte[] getModel(String graphName, String format) {
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(rdfService);
        Model model = accessor.getModel(graphName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        model.write(outputStream, format);
        return outputStream.toByteArray();
    }

    /**
     * Store a model with triples in the triple store
     * @param graphName The name of the graph to store the model in
     * @param model The model to be stored
     */
    public void storeModel(String graphName, Model model) {
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(rdfService);
        accessor.putModel(graphName, model);
    }

    /**
     * Check if a graph exists within the triple store
     * @param graphName The name of the graph
     * @return Whether the graph exists
     */
    public boolean graphExists(String graphName) {
        ParameterizedSparqlString graphQuery = new ParameterizedSparqlString();
        graphQuery.setCommandText("ASK WHERE { GRAPH ?graphName { ?s ?p ?o } }");
        graphQuery.setIri("graphName", graphName);
        Query query = QueryFactory.create(graphQuery.toString());
        try (QueryExecution qexec = QueryExecutionFactory.createServiceRequest(rdfService, query)) {
            return qexec.execAsk();
        }
    }

    /**
     * Check if a property of the ontology exists within the triple store
     * @param ontUri The URI of the property
     * @return Whether the graph exists
     */
    public boolean ontPropertyExists(String ontUri) {
        ParameterizedSparqlString graphQuery = new ParameterizedSparqlString();
        graphQuery.setCommandText("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                                  "ASK WHERE { GRAPH ?graphName { ?ont rdfs:label ?label } }");
        graphQuery.setIri("ont", ontUri);
        graphQuery.setIri("graphName", rdfService + "ontologies");
        Query query = QueryFactory.create(graphQuery.toString());
        try (QueryExecution qexec = QueryExecutionFactory.createServiceRequest(rdfService, query)) {
            return qexec.execAsk();
        }
    }

    /**
     * Get the label and doc strings for a workflow resource
     * @param workflowURI The URI of the workflow
     * @return Result set with label and doc strings
     */
    public ResultSet getLabelAndDoc(String workflowURI) {
        ParameterizedSparqlString labelQuery = new ParameterizedSparqlString();
        labelQuery.setCommandText(queryCtx +
                "SELECT ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf rdf:type ?type .\n" +
                "    OPTIONAL { ?wf sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?wf sld:doc|rdfs:comment ?doc }\n" +
                "  }" +
                "}");
        labelQuery.setIri("wf", workflowURI);
        return runQuery(labelQuery);
    }

    /**
     * Get the label for an ontology URL
     * TODO: can be merged with getLabelAndDoc when common-workflow-language/cwltool#427 is resolved
     * @param ontologyURI The format URI for the ontology
     * @return Result set with label and doc strings
     */
    public String getOntLabel(String ontologyURI) {
        ParameterizedSparqlString labelQuery = new ParameterizedSparqlString();
        labelQuery.setCommandText("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                "SELECT ?label\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {\n" +
                "    ?ont rdfs:label ?label\n" +
                "  }\n" +
                "}\n");
        labelQuery.setIri("ont", ontologyURI);
        labelQuery.setIri("graphName", rdfService + "ontologies");
        ResultSet result = runQuery(labelQuery);
        if (result.hasNext()) {
            return result.next().get("label").toString();
        }
        return null;
    }

    /**
     * Get the inputs for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of inputs
     */
    public ResultSet getInputs(String workflowURI) {
        ParameterizedSparqlString inputsQuery = new ParameterizedSparqlString();
        inputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?items ?null ?format ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:inputs ?name .\n" +
                "    OPTIONAL {\n" +
                "      { \n" +
                "        ?name sld:type ?type\n" +
                "        FILTER(?type != sld:null) \n" +
                "        FILTER (!isBlank(?type))\n" +
                "      } UNION { \n" +
                "        ?name sld:type ?arraytype .\n" +
                "        ?arraytype sld:type ?type .\n" +
                "        ?arraytype sld:items ?items \n" +
                "      }\n" +
                "    }\n" +
                "    OPTIONAL { \n" +
                "      ?name sld:type ?null\n" +
                "      FILTER(?null = sld:null)\n" +
                "    }\n" +
                "    OPTIONAL { ?name cwl:format ?format }\n" +
                "    OPTIONAL { ?name sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc|rdfs:comment ?doc }" +
                "  }" +
                "}");
        inputsQuery.setIri("wf", workflowURI);
        return runQuery(inputsQuery);
    }

    /**
     * Get the outputs for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of outputs
     */
    public ResultSet getOutputs(String workflowURI) {
        ParameterizedSparqlString outputsQuery = new ParameterizedSparqlString();
        outputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?items ?null ?format ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?name .\n" +
                "    OPTIONAL {\n" +
                "      { \n" +
                "        ?name sld:type ?type\n" +
                "        FILTER(?type != sld:null) \n" +
                "        FILTER (!isBlank(?type))\n" +
                "      } UNION { \n" +
                "        ?name sld:type ?arraytype .\n" +
                "        ?arraytype sld:type ?type .\n" +
                "        ?arraytype sld:items ?items \n" +
                "      }\n" +
                "    }\n" +
                "    OPTIONAL { \n" +
                "      ?name sld:type ?null\n" +
                "      FILTER(?null = sld:null)\n" +
                "    }\n" +
                "    OPTIONAL { ?name cwl:format ?format }\n" +
                "    OPTIONAL { ?name sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc|rdfs:comment ?doc }" +
                "  }" +
                "}");
        outputsQuery.setIri("wf", workflowURI);
        return runQuery(outputsQuery);
    }

    /**
     * Get the steps for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getSteps(String workflowURI) {
        ParameterizedSparqlString stepQuery = new ParameterizedSparqlString();
        stepQuery.setCommandText(queryCtx +
                "SELECT ?step ?run ?runtype ?label ?doc ?stepinput ?default ?src\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:run ?run .\n" +
                "    ?run rdf:type ?runtype .\n" +
                "    OPTIONAL { \n" +
                "        ?step cwl:in ?stepinput .\n" +
                "        { ?stepinput cwl:source ?src } UNION { ?stepinput cwl:default ?default }\n" +
                "    }\n" +
                "    OPTIONAL { ?run sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?run sld:doc|rdfs:comment ?doc }\n" +
                "  }" +
                "}");
        stepQuery.setIri("wf", workflowURI);
        return runQuery(stepQuery);
    }

    /**
     * Get links between steps for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of step links
     */
    public ResultSet getStepLinks(String workflowURI) {
        ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
        linkQuery.setCommandText(queryCtx +
                "SELECT ?src ?dest ?default\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:in ?dest .\n" +
                "    { ?dest cwl:source ?src } UNION { ?dest cwl:default ?default }\n" +
                "  }" +
                "}");
        linkQuery.setIri("wf", workflowURI);
        return runQuery(linkQuery);
    }

    /**
     * Get links between steps and outputs for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getOutputLinks(String workflowURI) {
        ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
        linkQuery.setCommandText(queryCtx +
                "SELECT ?src ?dest\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?dest .\n" +
                "    ?dest cwl:outputSource ?src\n" +
                "  }" +
                "}");
        linkQuery.setIri("wf", workflowURI);
        return runQuery(linkQuery);
    }

    /**
     * Gets the docker requirement and pull link for a workflow
     * @param workflowURI URI of the workflow
     * @return Result set of docker hint and pull link
     */
    public ResultSet getDockerLink(String workflowURI) {
        ParameterizedSparqlString dockerQuery = new ParameterizedSparqlString();
        dockerQuery.setCommandText(queryCtx +
                "SELECT ?docker ?pull\n" +
                "WHERE {\n" +
                "  GRAPH ?wf {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    { ?wf cwl:requirements ?docker } UNION { ?wf cwl:hints ?docker} .\n" +
                "    ?docker rdf:type cwl:DockerRequirement\n" +
                "    OPTIONAL { ?docker DockerRequirement:dockerPull ?pull }\n" +
                "  }" +
                "}");
        dockerQuery.setIri("wf", workflowURI);
        return runQuery(dockerQuery);
    }

    /**
     * Get authors from schema.org creator fields for a file
     * @param path The path within the Git repository to the file
     * @param fileUri URI of the file
     * @return The result set of step links
     */
    public ResultSet getAuthors(String path, String fileUri) {
        ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
        linkQuery.setCommandText(queryCtx +
                "SELECT ?email ?name ?orcid\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?file s:author|s:contributor|s:creator ?author .\n" +
                "    {\n" +
                "      ?creator rdf:type s:Person .\n" +
                "      OPTIONAL { ?author s:email ?email }\n" +
                "      OPTIONAL { ?author s:name ?name }\n" +
                "      OPTIONAL { ?author s:id|s:sameAs ?orcid }\n" +
                "    } UNION {\n" +
                "      ?author rdf:type s:Organization .\n" +
                "      ?author s:department* ?dept .\n" +
                "      ?dept s:member ?member\n" +
                "      OPTIONAL { ?member s:email ?email }\n" +
                "      OPTIONAL { ?member s:name ?name }\n" +
                "      OPTIONAL { ?member s:id|s:sameAs ?orcid }\n" +
                "    }\n" +
                "    FILTER(regex(str(?orcid), \"^https?://orcid.org/\" ))\n" +
                "    FILTER(regex(str(?file), ?wfFilter, \"i\" ))\n" +
                "  }" +
                "}");
        linkQuery.setLiteral("wfFilter", path + "$");
        linkQuery.setIri("graphName", fileUri);
        return runQuery(linkQuery);
    }

    /**
     * Gets the step name from a full URI
     * @param baseUrl the URL of the workflow
     * @param uri The URI
     * @return The step ID
     */
    public String stepNameFromURI(String baseUrl, String uri) {
        uri = uri.substring(uri.indexOf(baseUrl));
        uri = uri.replace(baseUrl, "");
        uri = uri.replace("#", "/");
        uri = uri.substring(1);
        if (uri.indexOf("/") > 0) {
            return uri.substring(0, uri.indexOf("/"));
        }
        return uri;
    }

    /**
     * Format a default value
     * @param defaultVal The default value
     * @return Default value suitable for a node label
     */
    public String formatDefault(String defaultVal) {
        int lastCaret = defaultVal.indexOf("^^");
        if (lastCaret != -1) {
            return defaultVal.substring(0, lastCaret);
        }
        return "\\\"" + defaultVal + "\\\"";
    }

    /**
     * Convert an RDF type to cwl process
     * @param runtype The string from the RDF
     * @return CWL process the string refers to
     */
    public CWLProcess strToRuntype(String runtype) {
        switch (runtype) {
            case "https://w3id.org/cwl/cwl#Workflow":
                return CWLProcess.WORKFLOW;
            case "https://w3id.org/cwl/cwl#CommandLineTool":
                return CWLProcess.COMMANDLINETOOL;
            case "https://w3id.org/cwl/cwl#ExpressionTool":
                return CWLProcess.EXPRESSIONTOOL;
            default:
                return null;
        }
    }

    /**
     * Get the label for the node from its name
     * @param name The name in the form filename#step
     * @return The second part of the name, just the step
     */
    public String labelFromName(String name) {
        name = name.substring(name.indexOf('#') + 1);
        int slashIndex = name.indexOf("/");
        if (slashIndex > 0) {
            name = name.substring(slashIndex + 1);
        }
        return name;
    }

    /**
     * Run a SPARQL query on a given model
     * @param queryString The query to be run
     * @return The result set of the query
     */
    ResultSet runQuery(ParameterizedSparqlString queryString) {
        Query query = QueryFactory.create(queryString.toString());
        try (QueryExecution qexec = QueryExecutionFactory.createServiceRequest(rdfService, query)) {
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

}
