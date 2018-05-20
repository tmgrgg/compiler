package smacc.exceptions;

public class IdentifierUndeclaredException extends SMACCException {
  private String id;

  public IdentifierUndeclaredException(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
