package org.commonwl.view.cwl;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
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
            "PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>";

    /**
     * Get the doc string for a workflow resource
     * @param wfResource RDF resource of the workflow
     * @return The doc string
     */
    public String getDoc(Resource wfResource) {
        if (wfResource.hasProperty(RDFS.comment)) {
            return wfResource.getProperty(RDFS.comment).toString();
        } else {
            return null;
        }
    }

    /**
     * Get the label string for a workflow resource
     * @param wfResource RDF resource of the workflow
     * @return The label string
     */
    public String getLabel(Resource wfResource) {
        if (wfResource.hasProperty(RDFS.label)) {
            return wfResource.getProperty(RDFS.label).toString();
        } else {
            return FilenameUtils.getName(wfResource.getURI());
        }
    }

    /**
     * Get the inputs for the workflow in the model
     * @param model RDF model of the workflow and tools
     * @param workflowURI URI of the workflow
     * @return The result set of inputs
     */
    public ResultSet getInputs(Model model, String workflowURI) {
        ParameterizedSparqlString inputsQuery = new ParameterizedSparqlString();
        inputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?label ?doc\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:inputs ?name .\n" +
                "    OPTIONAL { ?name sld:type ?type }\n" +
                "    OPTIONAL { ?name sld:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc ?doc }\n" +
                "}");
        inputsQuery.setIri("wf", workflowURI);
        return runQuery(inputsQuery, model);
    }

    /**
     * Get the outputs for the workflow in the model
     * @param model RDF model of the workflow and tools
     * @param workflowURI URI of the workflow
     * @return The result set of outputs
     */
    public ResultSet getOutputs(Model model, String workflowURI) {
        ParameterizedSparqlString outputsQuery = new ParameterizedSparqlString();
        outputsQuery.setCommandText(queryCtx +
                "SELECT ?name ?type ?src ?label ?doc\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?name .\n" +
                "    OPTIONAL { ?name cwl:outputSource ?src }\n" +
                "    OPTIONAL { ?name sld:type ?type }\n" +
                "    OPTIONAL { ?name sld:label ?label }\n" +
                "    OPTIONAL { ?name sld:doc ?doc }\n" +
                "}");
        outputsQuery.setIri("wf", workflowURI);
        return runQuery(outputsQuery, model);
    }

    /**
     * Get the steps for the workflow in the model
     * @param model RDF model of the workflow and tools
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getSteps(Model model, String workflowURI) {
        ParameterizedSparqlString stepQuery = new ParameterizedSparqlString();
        stepQuery.setCommandText(queryCtx +
                "SELECT ?step ?run ?runtype ?label ?doc ?stepinput ?default ?src\n" +
                "WHERE {\n" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:run ?run .\n" +
                "    ?run rdf:type ?runtype .\n" +
                "    OPTIONAL { \n" +
                "        ?step cwl:in ?stepinput .\n" +
                "        { ?stepinput cwl:source ?src } UNION { ?stepinput cwl:default ?default }\n" +
                "    }\n" +
                "    OPTIONAL { ?run sld:label ?label }\n" +
                "    OPTIONAL { ?run sld:doc ?doc }\n" +
                "}");
        stepQuery.setIri("wf", workflowURI);
        return runQuery(stepQuery, model);
    }

    /**
     * Get links between steps within a workflow
     * @param model RDF model of the workflow and tools
     * @param workflowURI URI of the workflow
     * @return The result set of steps
     */
    public ResultSet getStepLinks(Model model, String workflowURI) {
        ParameterizedSparqlString stepQuery = new ParameterizedSparqlString();
        stepQuery.setCommandText(queryCtx +
                "SELECT ?src ?dest ?default\n" +
                "WHERE {\n" +
                "    { \n" +
                "        ?wf Workflow:steps ?dest .\n" +
                "        ?dest cwl:in ?stepinput .\n" +
                "        { ?stepinput cwl:source ?src } UNION { ?stepinput cwl:default ?default }\n" +
                "  \t} UNION {\n" +
                "        ?wf rdf:type cwl:Workflow .\n" +
                "        ?wf cwl:outputs ?dest .\n" +
                "        ?dest cwl:outputSource ?src\n" +
                "  \t}\n" +
                "}");
        stepQuery.setIri("wf", workflowURI);
        return runQuery(stepQuery, model);
    }

    /**
     * Gets the docker requirement and pull link for a workflow
     * @param model RDF model of the workflow and tools
     * @param workflowURI URI of the workflow
     * @return Result set of docker hint and pull link
     */
    public ResultSet getDockerLink(Model model, String workflowURI) {
        ParameterizedSparqlString dockerQuery = new ParameterizedSparqlString();
        dockerQuery.setCommandText(queryCtx +
                "SELECT ?docker ?pull\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    { ?wf cwl:requirements ?docker } UNION { ?wf cwl:hints ?docker} .\n" +
                "    ?docker rdf:type cwl:DockerRequirement\n" +
                "    OPTIONAL { ?docker DockerRequirement:dockerPull ?pull }\n" +
                "}");
        dockerQuery.setIri("wf", workflowURI);
        return runQuery(dockerQuery, model);
    }

    /**
     * Gets the step ID from a full URI
     * @param uri The URI
     * @return The step ID
     */
    public String stepFromURI(String uri) {
        int lastHash = uri.lastIndexOf('#');
        if (lastHash != -1) {
            uri = uri.substring(lastHash + 1);
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash != -1) {
                uri = uri.substring(0, lastSlash);
            }
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
        return defaultVal;
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
     * Run a SPARQL query on a given model
     * @param queryString The query to be run
     * @param model The model to be run on
     * @return The result set of the query
     */
    private ResultSet runQuery(ParameterizedSparqlString queryString, Model model) {
        Query query = QueryFactory.create(queryString.toString());
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

}
