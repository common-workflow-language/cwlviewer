package org.commonwl.view.cwl;

import org.commonwl.view.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Interacts with the Python reference implementation
 * of the common workflow language
 */
@Service
public class CWLTool {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cwlToolVersion;

    /**
     * Get the RDF representation of a CWL file
     * @param url The URL of the CWL file
     * @return The RDF representing the CWL file
     * @throws CWLValidationException cwltool errors
     */
    public String getRDF(String url) throws CWLValidationException {
        try {
            // Run cwltool --print-rdf with the URL
            String[] command = {"cwltool", "--non-strict", "--quiet", "--print-rdf", url};
            ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
            Process process = cwlToolProcess.start();

            // Read output from the process using threads
            StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
            errorGobbler.start();
            inputGobbler.start();

            // Wait for process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return inputGobbler.getContent();
            } else {
                throw new CWLValidationException(errorGobbler.getContent());
            }
        } catch (IOException|InterruptedException e) {
            logger.error("Error running cwltool process", e);
            throw new CWLValidationException("Error running cwltool process");
        }
    }

    /**
     * Gets the version of cwltool being used
     * @return The version number
     */
    public String getVersion() {
        if (cwlToolVersion != null) {
            return cwlToolVersion;
        }
        try {
            // Run cwltool --version
            String[] command = {"cwltool", "--version"};
            ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
            Process process = cwlToolProcess.start();

            // Get input stream
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            if ((line = br.readLine()) != null) {
                return line.substring(line.indexOf(' '));
            } else {
                return "<error getting cwl version>";
            }
        } catch (IOException ex) {
            return "<error getting cwl version>";
        }
    }

}
