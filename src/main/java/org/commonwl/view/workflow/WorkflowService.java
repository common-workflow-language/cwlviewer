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

package org.commonwl.view.workflow;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.commonwl.view.cwl.CWLService;
import org.commonwl.view.cwl.CWLTool;
import org.commonwl.view.github.GitHubService;
import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.researchobject.ROBundleFactory;
import org.commonwl.view.researchobject.ROBundleNotFoundException;
import org.eclipse.egit.github.core.RepositoryContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final CWLService cwlService;
    private final CWLTool cwlTool;
    private final WorkflowRepository workflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final GraphVizService graphVizService;
    private final int cacheDays;

    @Autowired
    public WorkflowService(GitHubService githubService,
                           CWLService cwlService,
                           CWLTool cwlTool,
                           WorkflowRepository workflowRepository,
                           ROBundleFactory ROBundleFactory,
                           GraphVizService graphVizService,
                           @Value("${cacheDays}") int cacheDays) {
        this.githubService = githubService;
        this.cwlService = cwlService;
        this.cwlTool = cwlTool;
        this.workflowRepository = workflowRepository;
        this.ROBundleFactory = ROBundleFactory;
        this.graphVizService = graphVizService;
        this.cacheDays = cacheDays;
    }

    /**
     * Gets a page of all workflows from the database
     * @param pageable The details of the page to be requested
     * @return The resulting page of the workflow entries
     */
    public Page<Workflow> getPageOfWorkflows(Pageable pageable) {
        return workflowRepository.findAllByOrderByRetrievedOnDesc(pageable);
    }

    /**
     * Get a workflow from the database by its ID
     * @param id The ID of the workflow
     * @return The model for the workflow
     */
    public Workflow getWorkflow(String id) {
        return workflowRepository.findOne(id);
    }

    /**
     * Get a list of workflows from a directory in Github
     * @param githubInfo Github information for the workflow
     * @return The list of workflow names
     */
    public List<WorkflowOverview> getWorkflowsFromDirectory(GithubDetails githubInfo) throws IOException {
        List<WorkflowOverview> workflowsInDir = new ArrayList<>();
        for (RepositoryContents content : githubService.getContents(githubInfo)) {
            int eIndex = content.getName().lastIndexOf('.') + 1;
            if (eIndex > 0) {
                String extension = content.getName().substring(eIndex);
                if (extension.equals("cwl")) {
                    GithubDetails githubFile = new GithubDetails(githubInfo.getOwner(),
                            githubInfo.getRepoName(), githubInfo.getBranch(), content.getPath());
                    WorkflowOverview overview = cwlService.getWorkflowOverview(githubFile);
                    if (overview != null) {
                        workflowsInDir.add(overview);
                    }
                }
            }
        }
        return workflowsInDir;
    }

    /**
     * Get a workflow from the database, refreshing it if cache has expired
     * @param githubInfo Github information for the workflow
     * @return The workflow model associated with githubInfo
     */
    public Workflow getWorkflow(GithubDetails githubInfo) {
        return null;
    }

    /**
     * Get the RO bundle for a Workflow, triggering re-download if it does not exist
     * @param id The ID of the workflow
     * @return The file containing the RO bundle
     * @throws ROBundleNotFoundException If the RO bundle was not found
     */
    public File getROBundle(String id) throws ROBundleNotFoundException {
        // Get workflow from database
        Workflow workflow = getWorkflow(id);

        // If workflow does not exist or the bundle doesn't yet
        if (workflow == null || workflow.getRoBundle() == null) {
            throw new ROBundleNotFoundException();
        }

        // 404 error with retry if the file on disk does not exist
        File bundleDownload = new File(workflow.getRoBundle());
        if (!bundleDownload.exists()) {
            // Clear current RO bundle link and create a new one (async)
            workflow.setRoBundle(null);
            workflowRepository.save(workflow);
            generateROBundle(workflow);
            throw new ROBundleNotFoundException();
        }

        return bundleDownload;
    }

    /**
     * Builds a new workflow from Github
     * @param githubInfo Github information for the workflow
     * @return The constructed model for the Workflow
     */
    public Workflow createWorkflow(GithubDetails githubInfo) {
        try {
            // Get the sha hash from a branch reference
            String latestCommit = githubService.getCommitSha(githubInfo);

            // Get rdf representation from cwltool
            String url = String.format("https://cdn.rawgit.com/%s/%s/%s/%s", githubInfo.getOwner(),
                    githubInfo.getRepoName(), latestCommit, githubInfo.getPath());

            // Validate CWL
            if (cwlTool.isValid(url)) {

                // If valid, get the RDF representation
                String rdf = cwlTool.getRDF(url);

                // Create a workflow model from RDF representation
                final Model model = ModelFactory.createDefaultModel();
                model.read(new ByteArrayInputStream(rdf.getBytes()), null, "TURTLE");

                // Base workflow details
                Resource workflow = model.getResource(url);
                String label;
                String doc;

                if (workflow.hasProperty(RDFS.label)) {
                    label = workflow.getProperty(RDFS.label).toString();
                } else {
                    label = FilenameUtils.getName(url);
                }

                if (workflow.hasProperty(RDFS.comment)) {
                    doc = workflow.getProperty(RDFS.comment).toString();
                }

                final String context = "PREFIX cwl: <https://w3id.org/cwl/cwl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX sld: <https://w3id.org/cwl/salad#>\n" +
                        "PREFIX Workflow: <https://w3id.org/cwl/cwl#Workflow/>";

                // Read inputs (cwl:inputs)
                logger.info("INPUTS");
                String inputsQuery = context +
                        "SELECT ?input ?type ?label ?doc \n" +
                        "WHERE { \n" +
                        "?wf rdf:type cwl:Workflow .\n" +
                        "?wf cwl:inputs ?input .\n" +
                        "OPTIONAL { ?input sld:type ?type } \n" +
                        "OPTIONAL { ?input sld:label ?label } \n" +
                        "OPTIONAL { ?input sld:doc ?doc } \n" +
                        "}";
                Query query = QueryFactory.create(inputsQuery) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution sln = results.nextSolution();
                        String output = sln.get("input") + " " + sln.get("type");
                        if (sln.contains("label")) {
                            output += sln.get("label");
                        }
                        if (sln.contains("doc")) {
                            output += sln.get("doc");
                        }
                        logger.info(output);
                    }
                }

                // Read outputs (cwl:outputs)
                logger.info("OUTPUTS");
                // Outputs are similar to inputs but also have sources for links

                // Read steps (Workflow:steps)
                logger.info("STEPS");
                String stepQuery = context +
                        "SELECT ?step ?run ?runtype ?label ?doc \n" +
                        "WHERE { \n" +
                        "?wf Workflow:steps ?step .\n" +
                        "?step cwl:run ?run .\n" +
                        "?run rdf:type ?runtype .\n" +
                        "OPTIONAL { ?run sld:label ?label } \n" +
                        "OPTIONAL { ?run sld:doc ?doc } \n" +
                        "}";
                query = QueryFactory.create(stepQuery) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                    ResultSet results = qexec.execSelect() ;
                    while(results.hasNext()) {
                        QuerySolution sln = results.nextSolution();
                        String output = sln.get("step") + " " + sln.get("run") + " " + sln.get("runtype");
                        if (sln.contains("label")) {
                            output += sln.get("label");
                        }
                        if (sln.contains("doc")) {
                            output += sln.get("doc");
                        }
                        logger.info(output);
                    }
                }

                // Read docker image (cwl:requirements)

                // Create workflow model
                //Workflow workflowModel = new Workflow(label, doc, );

            }

            //Workflow workflowModel = new Workflow();

            /*if (workflowModel != null) {
                // Set origin details
                workflowModel.setRetrievedOn(new Date());
                workflowModel.setRetrievedFrom(githubInfo);
                workflowModel.setLastCommit(latestCommit);

                // Create a new research object bundle for the workflow
                // This is Async so cannot just call constructor, needs intermediate as per Spring framework
                generateROBundle(workflowModel);

                // Save to database
                workflowRepository.save(workflowModel);

                // Return this model to be displayed
                return workflowModel;
            } else {
                logger.error("No workflow could be found");
            }*/

        } catch (Exception ex) {
            logger.error("Error creating workflow", ex);
        }

        return null;
    }

    /**
     * Generates the RO bundle for a Workflow and adds it to the model
     * @param workflow The workflow model to create a Research Object for
     */
    private void generateROBundle(Workflow workflow) {
        try {
            ROBundleFactory.workflowROFromGithub(workflow.getRetrievedFrom());
        } catch (Exception ex) {
            logger.error("Error creating RO Bundle", ex);
        }
    }

    /**
     * Removes a workflow and its research object bundle
     * @param workflow The workflow to be deleted
     */
    private void removeWorkflow(Workflow workflow) {
        // Delete the Research Object Bundle from disk
        File roBundle = new File(workflow.getRoBundle());
        if (roBundle.delete()) {
            logger.debug("Deleted Research Object Bundle");
        } else {
            logger.debug("Failed to delete Research Object Bundle");
        }

        // Delete cached graphviz images if they exist
        graphVizService.deleteCache(workflow.getID());

        // Remove the workflow from the database
        workflowRepository.delete(workflow);
    }

    /**
     * Check for cache expiration based on time and commit sha
     * @param workflow The cached workflow model
     * @return Whether or not there are new commits
     */
    private boolean cacheExpired(Workflow workflow) {
        try {
            // Calculate expiration
            Calendar expireCal = Calendar.getInstance();
            expireCal.setTime(workflow.getRetrievedOn());
            expireCal.add(Calendar.DATE, cacheDays);
            Date expirationDate = expireCal.getTime();

            // Check cached retrievedOn date
            if (expirationDate.before(new Date())) {
                // Cache expiry time has elapsed
                // Check current head of the branch with the cached head
                logger.debug("Time has expired for caching, checking commits...");
                String currentHead = githubService.getCommitSha(workflow.getRetrievedFrom());
                logger.debug("Current: " + workflow.getLastCommit() + ", HEAD: " + currentHead);

                // Reset date in database if there are still no changes
                boolean expired = !workflow.getLastCommit().equals(currentHead);
                if (!expired) {
                    workflow.setRetrievedOn(new Date());
                    workflowRepository.save(workflow);
                }

                // Return whether the cache has expired
                return expired;
            } else {
                // Cache expiry time has not elapsed yet
                return false;
            }
        } catch (Exception ex) {
            // Default to no expiry if there was an API error
            return false;
        }
    }
}
