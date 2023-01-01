package org.commonwl.view.git;

import javax.validation.ValidationException;

public class GitLicenseException extends ValidationException {

  public GitLicenseException(String message) {
    super(message);
  }

  public GitLicenseException(Throwable throwable) {
    super(throwable);
  }

  public GitLicenseException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
