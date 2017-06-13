package org.commonwl.view.cwl;

import javax.validation.ValidationException;

/**
 * Exception thrown when a workflow failed CWLTool validation
 */
public class CWLValidationException extends ValidationException {

    public CWLValidationException(String message) {
        super(message);
    }

    public CWLValidationException(Throwable throwable) {
        super(throwable);
    }

    public CWLValidationException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
