package smacc.arm;

/*
 * Adds static functions from the predefined function handler
 */
public class ARMFileEnd extends ARMNode {

  private PredefinedFunctionHandler predefinedFunctionHandler;

  public ARMFileEnd(PredefinedFunctionHandler predefinedFunctionHandler) {
    this.predefinedFunctionHandler = predefinedFunctionHandler;
  }

  public String toString() {
    return predefinedFunctionHandler.toString();
  }

}
