package org.commonwl.view.cwl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Interacts with the Python reference implementation
 * of the common workflow language
 */
@Service
public class CWLTool {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO: Multiple graph objects require appending id eg #something

    /**
     * Validate a CWL file
     * @param url The URL of the CWL file
     * @return Whether the CWL file is valid
     */
    public boolean isValid(String url) {

        try {
            // Run cwltool --validate with the URL
            String[] command = {"cwltool", "--validate", url};
            ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
            Process process = cwlToolProcess.start();

            // Read error from the process if exists
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            String output = "";
            while ((line = br.readLine()) != null) {
                output += line;
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            } else {
                throw new CWLValidationException(output);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    /**
     * Get the RDF representation of a CWL file
     * @param url The URL of the CWL file
     * @return The RDF representing the CWL file
     */
    public String getRDF(String url) {

        try {
            // Run cwltool --print-rdf with the URL
            String[] command = {"cwltool", "--print-rdf", url};
            ProcessBuilder cwlToolProcess = new ProcessBuilder(command);
            Process process = cwlToolProcess.start();

            // Read response rdf from the process
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            String rdf = "";
            while ((line = br.readLine()) != null) {
                rdf += line;
            }

            return rdf;

        } catch (Exception e) {
            logger.error("Error parsing CWL", e);
            return "";
        }

    }
}
