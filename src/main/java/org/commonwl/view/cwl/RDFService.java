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
     * @return The result set of inputs
     */
    public ResultSet getInputs(Model model) {
        String inputsQuery = queryCtx +
                "SELECT ?input ?type ?label ?doc ?default\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:inputs ?input .\n" +
                "    OPTIONAL { ?input sld:type ?type }\n" +
                "    OPTIONAL { ?input sld:label ?label }\n" +
                "    OPTIONAL { ?input sld:doc ?doc }\n" +
                "}";
        return runQuery(inputsQuery, model);
    }

    /**
     * Get the outputs for the workflow in the model
     * @param model RDF model of the workflow and tools
     * @return The result set of outputs
     */
    public ResultSet getOutputs(Model model) {
        String outputsQuery = queryCtx +
                "SELECT ?output ?type ?src ?label ?doc\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    ?wf cwl:outputs ?output .\n" +
                "    OPTIONAL { ?output cwl:outputSource ?src }\n" +
                "    OPTIONAL { ?output sld:type ?type }\n" +
                "    OPTIONAL { ?output sld:label ?label }\n" +
                "    OPTIONAL { ?output sld:doc ?doc }\n" +
                "}";
        return runQuery(outputsQuery, model);
    }

    /**
     * Get the steps for the workflow in the model
     * @param model RDF model of the workflow and tools
     * @return The result set of steps
     */
    public ResultSet getSteps(Model model) {
        String stepQuery = queryCtx +
                "SELECT ?step ?run ?runtype ?label ?doc ?stepinput ?src\n" +
                "WHERE {\n" +
                "    ?wf Workflow:steps ?step .\n" +
                "    ?step cwl:run ?run .\n" +
                "    ?run rdf:type ?runtype .\n" +
                "    OPTIONAL { \n" +
                "        ?step cwl:in ?stepinput .\n" +
                "        ?stepinput cwl:source ?src\n" +
                "    }\n" +
                "    OPTIONAL { ?run sld:label ?label }\n" +
                "    OPTIONAL { ?run sld:doc ?doc }\n" +
                "}";
        return runQuery(stepQuery, model);
    }

    /**
     * Gets the docker requirement and pull link for a workflow
     * @param model RDF model of the workflow and tools
     * @return Result set of docker hint and pull link
     */
    public ResultSet getDockerLink(Model model) {
        String dockerQuery = queryCtx +
                "SELECT ?docker ?pull\n" +
                "WHERE {\n" +
                "    ?wf rdf:type cwl:Workflow .\n" +
                "    { ?wf cwl:requirements ?docker } UNION { ?wf cwl:hints ?docker} .\n" +
                "    ?docker rdf:type cwl:DockerRequirement.\n" +
                "    OPTIONAL { ?docker DockerRequirement:dockerPull ?pull }\n" +
                "}";
        return runQuery(dockerQuery, model);
    }

    /**
     * Run a SPARQL query on a given model
     * @param queryString The query to be run
     * @param model The model to be run on
     * @return The result set of the query
     */
    private ResultSet runQuery(String queryString, Model model) {
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            return ResultSetFactory.copyResults(qexec.execSelect());
        }
    }

}
