package org.researchobject.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.taverna.cwl.utilities.CWLUtil;
import org.apache.taverna.cwl.utilities.PortDetail;
import org.eclipse.egit.github.core.RepositoryContents;
import org.researchobject.domain.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowFactory {

    /**
     * Github API service
     */
    private final GitHubUtil githubUtil;

    @Autowired
    public WorkflowFactory(GitHubUtil githubUtil) {
        this.githubUtil = githubUtil;
    }

    /**
     * Builds a new workflow from cwl files fetched from Github
     * @param githubURL Github directory URL to get the files from
     * @return The constructed model for the Workflow
     */
    public Workflow workflowFromGithub(String githubURL) {

        List<String> directoryDetails = githubUtil.detailsFromDirURL(githubURL);

        // If the URL is valid and details could be extracted
        if (directoryDetails.size() > 0) {

            // Store returned details
            final String owner = directoryDetails.get(0);
            final String repoName = directoryDetails.get(1);
            final String branch = directoryDetails.get(2);
            final String path = directoryDetails.get(3);

            // Get contents of the repo
            try {
                List<RepositoryContents> repoContents = githubUtil.getContents(owner, repoName, branch, path);

                // Filter the repository contents into a new list - only files with .cwl extension
                List<RepositoryContents> workflowFiles = new ArrayList<>();
                for (RepositoryContents repoContent : repoContents) {
                    if (repoContent.getType().equals("file")) {
                        int eIndex = repoContent.getName().lastIndexOf('.') + 1;
                        if (eIndex > 0) {
                            String extension = repoContent.getName().substring(eIndex);
                            if (extension.equals("cwl")) {
                                workflowFiles.add(repoContent);
                            }
                        }
                    }
                }

                // Loop through the cwl files
                for (RepositoryContents workflowFile : workflowFiles) {

                    // Get the content of specific file from the listing
                    List<RepositoryContents> currentWorkflow = githubUtil.getContents(owner, repoName, branch, workflowFile.getPath());
                    String fileContentBase64 = currentWorkflow.get(0).getContent();
                    String fileContent = new String(Base64.decodeBase64(fileContentBase64.getBytes()));

                    // Parse as yaml
                    Yaml reader = new Yaml();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode cwlFile = mapper.valueToTree(reader.load(fileContent));
                    CWLUtil cwlUtil = new CWLUtil(cwlFile);

                    // Find the first file which is a workflow
                    if (cwlFile.get("class").asText().equals("Workflow")) {

                        // Get label, description and details of inputs/outputs
                        PortDetail workflowDetails = new PortDetail();
                        cwlUtil.extractLabel(cwlFile, workflowDetails);
                        cwlUtil.extractDescription(cwlFile, workflowDetails);
                        Map<String, PortDetail> inputs = cwlUtil.processInputDetails();
                        Map<String, PortDetail> outputs = cwlUtil.processOutputDetails();

                        // If the label for the workflow is null, use the filename
                        if (workflowDetails.getLabel() == null) {
                            workflowDetails.setLabel(workflowFile.getName());
                        }

                        // If the description of the workflow is null, use a default
                        if (workflowDetails.getDescription() == null) {
                            workflowDetails.setDescription("Missing workflow description");
                        }

                        // Construct new workflow from the details
                        return new Workflow(workflowDetails.getLabel(), workflowDetails.getDescription(), inputs, outputs);
                    }

                }
            } catch (IOException ex) {
                System.out.println("API Error");
            }
        } else {
            System.out.println("Error should never happen, already passed validation");
        }

        return null;
    }
}
