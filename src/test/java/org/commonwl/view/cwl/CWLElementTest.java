package org.commonwl.view.cwl;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CWLElementTest {

    /**
     * Test addition and retrieval of source IDs from a node
     * null values should not be added to the list
     */
    @Test
    public void testSourceIDList() throws Exception {

        CWLElement element = new CWLElement();

        element.addSourceID("sourceID1");
        element.addSourceID("sourceID2");
        element.addSourceID(null);
        element.addSourceID("sourceID3");

        List<String> sourceIDs = element.getSourceIDs();

        assertEquals(3, sourceIDs.size());
        assertEquals("sourceID3", sourceIDs.get(2));

    }

}