package org.commonwl.view.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Background thread to consume stream and
 * collect contents
 */
public class StreamGobbler extends Thread {
    private final String lineSeparator = System.getProperty("line.separator");

    private InputStream is;
    private String content = "";

    public StreamGobbler(InputStream is) {
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ( (line = br.readLine()) != null) {
                content += line + lineSeparator;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getContent() {
        return content;
    }
}