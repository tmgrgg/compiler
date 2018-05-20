package smacc.arm;

public class ARMFunctionEnd extends ARMNode {
  public ARMFunctionEnd() {

  }

  public String toString() {
    return "\t.ltorg\n";
  }

}
