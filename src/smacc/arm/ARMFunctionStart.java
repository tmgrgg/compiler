package smacc.arm;

public class ARMFunctionStart extends ARMNode {
  public ARMFunctionStart() {

  }

  public String toString() {
    return "\tPUSH {lr}\n";
  }

}
