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
            "PREFIX rdfs: <rdfs:>";

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
     * Store a model with triples in the triple store
     * @param graphName The name of the graph to store the model in
     * @param model The model to be stored
     */
    public void storeModel(String graphName, Model model) {
        DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(rdfService);
        accessor.putModel(rdfService + graphName, model);
    }

    /**
     * Check if a graph exists within the triple store
     * @param graphName The name of the graph
     * @return Whether the graph exists
     */
    public boolean graphExists(String graphName) {
        ParameterizedSparqlString graphQuery = new ParameterizedSparqlString();
        graphQuery.setCommandText("ASK WHERE { GRAPH ?graphName { ?s ?p ?o } }");
        graphQuery.setIri("graphName", rdfService + graphName);
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
    public ResultSet getLabelAndDoc(String path, String workflowURI) {
        ParameterizedSparqlString labelQuery = new ParameterizedSparqlString();
        labelQuery.setCommandText(queryCtx +
                "SELECT ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    OPTIONAL { ?wf sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?wf sld:doc|rdfs:comment ?doc }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        labelQuery.setLiteral("wfFilter", path + "$");
        labelQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(labelQuery);
    }

    /**
     * Get the label for an ontology URL
     * TODO: can be merged with getLabelAndDoc when cwltool namespace bug is resolved
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
    public ResultSet getInputs(String path, String workflowURI) {
        ParameterizedSparqlString inputsQuery = new ParameterizedSparqlString();
        inputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?format ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:inputs ?name .\n" +
                "    OPTIONAL { ?name sld:type ?type }\n" +
                "    OPTIONAL { ?name cwl:format ?format }\n" +
                "    OPTIONAL { ?name sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc|rdfs:comment ?doc }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        inputsQuery.setLiteral("wfFilter", path + "$");
        inputsQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(inputsQuery);
    }

    /**
     * Get the outputs for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of outputs
     */
    public ResultSet getOutputs(String path, String workflowURI) {
        ParameterizedSparqlString outputsQuery = new ParameterizedSparqlString();
        outputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?format ?label ?doc\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?name .\n" +
                "    OPTIONAL { ?name sld:type ?type }\n" +
                "    OPTIONAL { ?name cwl:format ?format }\n" +
                "    OPTIONAL { ?name sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc|rdfs:comment ?doc }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        outputsQuery.setLiteral("wfFilter", path + "$");
        outputsQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(outputsQuery);
    }

    /**
     * Get the steps for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getSteps(String path, String workflowURI) {
        ParameterizedSparqlString stepQuery = new ParameterizedSparqlString();
        stepQuery.setCommandText(queryCtx +
                "SELECT ?step ?run ?runtype ?label ?doc ?stepinput ?default ?src\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:run ?run .\n" +
                "    ?run rdf:type ?runtype .\n" +
                "    OPTIONAL { \n" +
                "        ?step cwl:in ?stepinput .\n" +
                "        { ?stepinput cwl:source ?src } UNION { ?stepinput cwl:default ?default }\n" +
                "    }\n" +
                "    OPTIONAL { ?run sld:label|rdfs:label ?label }\n" +
                "    OPTIONAL { ?run sld:doc|rdfs:comment ?doc }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        stepQuery.setLiteral("wfFilter", path + "$");
        stepQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(stepQuery);
    }

    /**
     * Get links between steps for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of step links
     */
    public ResultSet getStepLinks(String path, String workflowURI) {
        ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
        linkQuery.setCommandText(queryCtx +
                "SELECT ?src ?dest ?default\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:in ?dest .\n" +
                "    { ?dest cwl:source ?src } UNION { ?dest cwl:default ?default }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        linkQuery.setLiteral("wfFilter", path + "$");
        linkQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(linkQuery);
    }

    /**
     * Get links between steps and outputs for the workflow in the model
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getOutputLinks(String path, String workflowURI) {
        ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
        linkQuery.setCommandText(queryCtx +
                "SELECT ?src ?dest\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?dest .\n" +
                "    ?dest cwl:outputSource ?src\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        linkQuery.setLiteral("wfFilter", path + "$");
        linkQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(linkQuery);
    }

    /**
     * Gets the docker requirement and pull link for a workflow
     * @param workflowURI URI of the workflow
     * @return Result set of docker hint and pull link
     */
    public ResultSet getDockerLink(String path, String workflowURI) {
        ParameterizedSparqlString dockerQuery = new ParameterizedSparqlString();
        dockerQuery.setCommandText(queryCtx +
                "SELECT ?docker ?pull\n" +
                "WHERE {\n" +
                "  GRAPH ?graphName {" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    { ?wf cwl:requirements ?docker } UNION { ?wf cwl:hints ?docker} .\n" +
                "    ?docker rdf:type cwl:DockerRequirement\n" +
                "    OPTIONAL { ?docker DockerRequirement:dockerPull ?pull }\n" +
                "    FILTER(regex(str(?wf), ?wfFilter, \"i\" ))" +
                "  }" +
                "}");
        dockerQuery.setLiteral("wfFilter", path + "$");
        dockerQuery.setIri("graphName", rdfService + workflowURI);
        return runQuery(dockerQuery);
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
