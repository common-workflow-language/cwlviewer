package org.commonwl.view.cwl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CWLProcessTest {

    /**
     * Test toString method for enum
     */
    @Test
    public void testToString() throws Exception {
        assertEquals("Workflow", CWLProcess.WORKFLOW.toString());
        assertEquals("Commandlinetool", CWLProcess.COMMANDLINETOOL.toString());
        assertEquals("Expressiontool", CWLProcess.EXPRESSIONTOOL.toString());
    }

}