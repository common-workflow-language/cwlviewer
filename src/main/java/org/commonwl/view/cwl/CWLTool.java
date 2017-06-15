package org.commonwl.view.cwl;

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

    // TODO: Multiple graph objects require appending id eg #something

    /**
     * Get the RDF representation of a CWL file
     * @param url The URL of the CWL file
     * @return The RDF representing the CWL file
     * @throws CWLValidationException cwltool errors
     */
    public String getRDF(String url) throws CWLValidationException {

        try {
            // Make sure the CWL is valid
            validate(url);

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
                rdf += line + "\n";
            }

            return rdf;

        } catch (IOException|InterruptedException e) {
            logger.error("Error running cwltool process", e);
            throw new CWLValidationException("Error running cwltool process");
        }

    }

    /**
     * Validate a CWL file
     * @param url The URL of the CWL file
     * @throws CWLValidationException cwltool errors
     * @throws InterruptedException cwltool does not run to completion
     * @throws IOException Process errors while running cwltool
     */
    private void validate(String url)
            throws CWLValidationException, InterruptedException, IOException {
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
            output += line + "\n";
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new CWLValidationException(output);
        }
    }
}
