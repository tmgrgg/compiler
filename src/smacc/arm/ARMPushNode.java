package smacc.arm;

public class ARMPushNode extends ARMNode {

  Register reg;

  public ARMPushNode(Register reg) {
    this.reg = reg;
  }

  public String toString() {
    instruction = String.format("\tPUSH {%s}\n", reg);
    return instruction;
  }
}
