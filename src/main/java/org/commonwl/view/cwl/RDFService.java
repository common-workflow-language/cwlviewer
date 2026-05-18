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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Handles the parsing of CWL RDF files */
@Service
public class RDFService {

  // Context for SPARQL queries
  private final String queryCtx =
      """
          PREFIX cwl: <https://w3id.org/cwl/cwl#>
          PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
          PREFIX sld: <https://w3id.org/cwl/salad#>
          PREFIX dct: <http://purl.org/dc/terms/>
          PREFIX doap: <http://usefulinc.com/ns/doap#>
          PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
          PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX s: <http://schema.org/>""";

  private final String rdfService;

  /**
   * Create the RDFService with configuration
   *
   * @param rdfService The SPARQL endpoint from configuration
   */
  @Autowired
  public RDFService(@Value("${sparql.endpoint}") String rdfService) {
    this.rdfService = rdfService;
  }

  /**
   * Add to ontologies in the triple store
   *
   * @param model The model to be stored
   */
  public void addToOntologies(Model model) {
    DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(rdfService);
    accessor.add("ontologies", model);
  }

  /**
   * Get a model from the triple store in a given format
   *
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
   *
   * @param graphName The name of the graph to store the model in
   * @param model The model to be stored
   */
  public void storeModel(String graphName, Model model) {
    DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(rdfService);
    accessor.setOutboundSyntax(RDFFormat.TURTLE);
    Node name = NodeFactory.createURI(graphName);
    accessor.httpPut(name, model.getGraph());
  }

  /**
   * Check if a graph exists within the triple store
   *
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
   *
   * @param ontUri The URI of the property
   * @return Whether the graph exists
   */
  public boolean ontPropertyExists(String ontUri) {
    ParameterizedSparqlString graphQuery = new ParameterizedSparqlString();
    graphQuery.setCommandText(
        """
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        ASK WHERE { GRAPH ?graphName { ?ont rdfs:label ?label } }
        """);
    graphQuery.setIri("ont", ontUri);
    graphQuery.setIri("graphName", rdfService + "ontologies");
    Query query = QueryFactory.create(graphQuery.toString());
    try (QueryExecution qexec = QueryExecutionFactory.createServiceRequest(rdfService, query)) {
      return qexec.execAsk();
    }
  }

  /**
   * Get the label and doc strings for a workflow resource
   *
   * @param workflowURI The URI of the workflow
   * @return Result set with label and doc strings
   */
  public ResultSet getLabelAndDoc(String workflowURI) {
    ParameterizedSparqlString labelQuery = new ParameterizedSparqlString();
    labelQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?label ?doc
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type ?type .
                OPTIONAL { ?wf sld:label|rdfs:label ?label }
                OPTIONAL { ?wf sld:doc|rdfs:comment ?doc }
              }
            }
            """);
    labelQuery.setIri("wf", workflowURI);
    return runQuery(labelQuery);
  }

  /**
   * Get the label for an ontology URL TODO: can be merged with getLabelAndDoc when
   * common-workflow-language/cwltool#427 is resolved
   *
   * @param ontologyURI The format URI for the ontology
   * @return Result set with label and doc strings
   */
  public String getOntLabel(String ontologyURI) {
    ParameterizedSparqlString labelQuery = new ParameterizedSparqlString();
    labelQuery.setCommandText(
        """
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\
            SELECT ?label
            WHERE {
              GRAPH ?graphName {
                ?ont rdfs:label ?label
              }
            }
            """);
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
   *
   * @param workflowURI URI of the workflow
   * @return The result set of inputs
   */
  public ResultSet getInputs(String workflowURI) {
    ParameterizedSparqlString inputsQuery = new ParameterizedSparqlString();
    inputsQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?name ?type ?items ?null ?format ?label ?doc
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type cwl:Workflow .
                ?wf cwl:inputs ?name .
                OPTIONAL {
                  {\s
                    ?name sld:type ?type
                    FILTER(?type != sld:null)\s
                    FILTER (!isBlank(?type))
                  } UNION {\s
                    ?name sld:type ?arraytype .
                    ?arraytype sld:type ?type .
                    ?arraytype sld:items ?items\s
                  }
                }
                OPTIONAL {\s
                  ?name sld:type ?null
                  FILTER(?null = sld:null)
                }
                OPTIONAL { ?name cwl:format ?format }
                OPTIONAL { ?name sld:label|rdfs:label ?label }
                OPTIONAL { ?name sld:doc|rdfs:comment ?doc }
              }
            }
            """);
    inputsQuery.setIri("wf", workflowURI);
    return runQuery(inputsQuery);
  }

  /**
   * Get the outputs for the workflow in the model
   *
   * @param workflowURI URI of the workflow
   * @return The result set of outputs
   */
  public ResultSet getOutputs(String workflowURI) {
    ParameterizedSparqlString outputsQuery = new ParameterizedSparqlString();
    outputsQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?name ?type ?items ?null ?format ?label ?doc
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type cwl:Workflow .
                ?wf cwl:outputs ?name .
                OPTIONAL {
                  {\s
                    ?name sld:type ?type
                    FILTER(?type != sld:null)\s
                    FILTER (!isBlank(?type))
                  } UNION {\s
                    ?name sld:type ?arraytype .
                    ?arraytype sld:type ?type .
                    ?arraytype sld:items ?items\s
                  }
                }
                OPTIONAL {\s
                  ?name sld:type ?null
                  FILTER(?null = sld:null)
                }
                OPTIONAL { ?name cwl:format ?format }
                OPTIONAL { ?name sld:label|rdfs:label ?label }
                OPTIONAL { ?name sld:doc|rdfs:comment ?doc }
              }
            }
            """);
    outputsQuery.setIri("wf", workflowURI);
    return runQuery(outputsQuery);
  }

  /**
   * Get the steps for the workflow in the model
   *
   * @param workflowURI URI of the workflow
   * @return The result set of steps
   */
  public ResultSet getSteps(String workflowURI) {
    ParameterizedSparqlString stepQuery = new ParameterizedSparqlString();
    stepQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?step ?run ?runtype ?label ?doc ?stepinput ?default ?src
            WHERE {
              GRAPH ?wf {
                ?wf Workflow:steps ?step .
                ?step cwl:run ?run .
                ?run rdf:type ?runtype .
                OPTIONAL {\s
                    ?step cwl:in ?stepinput .
                    { ?stepinput cwl:source ?src } UNION { ?stepinput cwl:default ?default }
                }
                OPTIONAL { ?run sld:label|rdfs:label ?label }
                OPTIONAL { ?run sld:doc|rdfs:comment ?doc }
              }
            }
            """);
    stepQuery.setIri("wf", workflowURI);
    return runQuery(stepQuery);
  }

  /**
   * Get links between steps for the workflow in the model
   *
   * @param workflowURI URI of the workflow
   * @return The result set of step links
   */
  public ResultSet getStepLinks(String workflowURI) {
    ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
    linkQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?src ?dest ?default
            WHERE {
              GRAPH ?wf {
                ?wf Workflow:steps ?step .
                ?step cwl:in ?dest .
                { ?dest cwl:source ?src } UNION { ?dest cwl:default ?default }
              }
            }
            """);
    linkQuery.setIri("wf", workflowURI);
    return runQuery(linkQuery);
  }

  /**
   * Get links between steps and outputs for the workflow in the model
   *
   * @param workflowURI URI of the workflow
   * @return The result set of steps
   */
  public ResultSet getOutputLinks(String workflowURI) {
    ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
    linkQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?src ?dest
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type cwl:Workflow .
                ?wf cwl:outputs ?dest .
                ?dest cwl:outputSource ?src
              }
            }
            """);
    linkQuery.setIri("wf", workflowURI);
    return runQuery(linkQuery);
  }

  /**
   * Gets the docker requirement and pull link for a workflow
   *
   * @param workflowURI URI of the workflow
   * @return Result set of docker hint and pull link
   */
  public ResultSet getDockerLink(String workflowURI) {
    ParameterizedSparqlString dockerQuery = new ParameterizedSparqlString();
    dockerQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?docker ?pull
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type cwl:Workflow .
                { ?wf cwl:requirements ?docker } UNION { ?wf cwl:hints ?docker} .
                ?docker rdf:type cwl:DockerRequirement
                OPTIONAL { ?docker DockerRequirement:dockerPull ?pull }
              }
            }
            """);
    dockerQuery.setIri("wf", workflowURI);
    return runQuery(dockerQuery);
  }

  /**
   * Get authors from schema.org creator fields for a file
   *
   * @param path The path within the Git repository to the file
   * @param fileUri URI of the file
   * @return The result set of step links
   */
  public ResultSet getAuthors(String path, String fileUri) {
    ParameterizedSparqlString linkQuery = new ParameterizedSparqlString();
    linkQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?email ?name ?orcid
            WHERE {
              GRAPH ?graphName {
                ?file s:author|s:contributor|s:creator ?author .
                {
                  ?creator rdf:type s:Person .
                  OPTIONAL { ?author s:email ?email }
                  OPTIONAL { ?author s:name ?name }
                  OPTIONAL { ?author s:id|s:sameAs ?orcid }
                } UNION {
                  ?author rdf:type s:Organization .
                  ?author s:department* ?dept .
                  ?dept s:member ?member
                  OPTIONAL { ?member s:email ?email }
                  OPTIONAL { ?member s:name ?name }
                  OPTIONAL { ?member s:id|s:sameAs ?orcid }
                }
                FILTER(regex(str(?orcid), "^https?://orcid.org/" ))
                FILTER(regex(str(?file), ?wfFilter, "i" ))
              }
            }
            """);
    linkQuery.setLiteral("wfFilter", path + "$");
    linkQuery.setIri("graphName", fileUri);
    return runQuery(linkQuery);
  }

  /**
   * Gets the step name from a full URI
   *
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
   *
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
   *
   * @param runtype The string from the RDF
   * @return CWL process the string refers to
   */
  public CWLProcess strToRuntype(String runtype) {
    return switch (runtype) {
      case "https://w3id.org/cwl/cwl#Workflow" -> CWLProcess.WORKFLOW;
      case "https://w3id.org/cwl/cwl#CommandLineTool" -> CWLProcess.COMMANDLINETOOL;
      case "https://w3id.org/cwl/cwl#ExpressionTool" -> CWLProcess.EXPRESSIONTOOL;
      default -> null;
    };
  }

  /**
   * Get the label for the node from its name
   *
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
   *
   * @param queryString The query to be run
   * @return The result set of the query
   */
  ResultSet runQuery(ParameterizedSparqlString queryString) {
    Query query = QueryFactory.create(queryString.toString());
    try (QueryExecution qexec = QueryExecutionFactory.createServiceRequest(rdfService, query)) {
      return ResultSetFactory.copyResults(qexec.execSelect());
    }
  }

  public ResultSet getLicense(String workflowURI) {
    ParameterizedSparqlString licenseQuery = new ParameterizedSparqlString();
    licenseQuery.setCommandText(
        queryCtx
            + """
            PREFIX cwl: <https://w3id.org/cwl/cwl#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX sld: <https://w3id.org/cwl/salad#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX doap: <http://usefulinc.com/ns/doap#>
            PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>
            PREFIX DockerRequirement: <https://w3id.org/cwl/cwl#DockerRequirement/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX s: <http://schema.org/>
            SELECT ?license\s
            WHERE {
              GRAPH ?wf {
                ?wf rdf:type cwl:Workflow .
                { ?wf s:license ?license }\s
            UNION { ?wf doap:license ?license }\s
            UNION { ?wf dct:license ?license }\s
              }
            }
            """);
    licenseQuery.setIri("wf", workflowURI);
    return runQuery(licenseQuery);
  }
}
