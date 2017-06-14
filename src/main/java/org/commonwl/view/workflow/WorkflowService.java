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

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.commonwl.view.cwl.*;
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
import java.util.*;

@Service
public class WorkflowService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final GitHubService githubService;
    private final CWLService cwlService;
    private final CWLTool cwlTool;
    private final RDFService rdfService;
    private final WorkflowRepository workflowRepository;
    private final ROBundleFactory ROBundleFactory;
    private final GraphVizService graphVizService;
    private final int cacheDays;

    @Autowired
    public WorkflowService(GitHubService githubService,
                           CWLService cwlService,
                           CWLTool cwlTool,
                           RDFService rdfService,
                           WorkflowRepository workflowRepository,
                           ROBundleFactory ROBundleFactory,
                           GraphVizService graphVizService,
                           @Value("${cacheDays}") int cacheDays) {
        this.githubService = githubService;
        this.cwlService = cwlService;
        this.cwlTool = cwlTool;
        this.rdfService = rdfService;
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
                String label = rdfService.getLabel(workflow);
                String doc = rdfService.getDoc(workflow);

                // Inputs
                Map<String, CWLElement> wfInputs = new HashMap<>();
                ResultSet inputs = rdfService.getInputs(model);
                while (inputs.hasNext()) {
                    QuerySolution input = inputs.nextSolution();
                    CWLElement wfInput = new CWLElement();
                    wfInput.setType(input.get("type").toString());
                    if (input.contains("label")) {
                        wfInput.setLabel(input.get("label").toString());
                    }
                    if (input.contains("doc")) {
                        wfInput.setDoc(input.get("doc").toString());
                    }
                    wfInputs.put(input.get("input").toString(), wfInput);
                }

                // Outputs
                Map<String, CWLElement> wfOutputs = new HashMap<>();
                ResultSet outputs = rdfService.getOutputs(model);
                while (outputs.hasNext()) {
                    QuerySolution output = outputs.nextSolution();
                    CWLElement wfOutput = new CWLElement();
                    wfOutput.setType(output.get("type").toString());
                    if (output.contains("src")) {
                        wfOutput.addSourceID(output.get("src").toString());
                    }
                    if (output.contains("label")) {
                        wfOutput.setLabel(output.get("label").toString());
                    }
                    if (output.contains("doc")) {
                        wfOutput.setDoc(output.get("doc").toString());
                    }
                    wfOutputs.put(output.get("output").toString(), wfOutput);
                }


                // Steps
                Map<String, CWLStep> wfSteps = new HashMap<>();
                ResultSet steps = rdfService.getSteps(model);
                while(steps.hasNext()) {
                    QuerySolution step = steps.nextSolution();
                    String uri = step.get("step").toString();
                    if (wfSteps.containsKey(uri)) {
                        // Already got step details, add extra source ID
                        if (step.contains("src")) {
                            CWLElement src = new CWLElement();
                            src.addSourceID(step.get("src").toString());
                            wfSteps.get(uri).getSources().put(
                                    step.get("stepinput").toString(), src);
                        }
                    } else {
                        // Add new step
                        CWLStep wfStep = new CWLStep();
                        wfStep.setRun(step.get("run").toString());
                        if (step.contains("src")) {
                            CWLElement src = new CWLElement();
                            src.addSourceID(step.get("src").toString());
                            Map<String, CWLElement> srcList = new HashMap<>();
                            srcList.put(step.get("stepinput").toString(), src);
                            wfStep.setSources(srcList);
                        }
                        if (step.contains("label")) {
                            wfStep.setLabel(step.get("label").toString());
                        }
                        if (step.contains("doc")) {
                            wfStep.setDoc(step.get("doc").toString());
                        }
                        wfSteps.put(uri, wfStep);
                    }
                }

                // Docker link

                // Create workflow model
                Workflow workflowModel = new Workflow(label, doc,
                        wfInputs, wfOutputs, wfSteps, null);
                workflowModel.generateDOT();

                workflowModel.setRetrievedOn(new Date());
                workflowModel.setRetrievedFrom(githubInfo);
                workflowModel.setLastCommit(latestCommit);

                return workflowModel;

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
