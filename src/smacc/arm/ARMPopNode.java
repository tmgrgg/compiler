package smacc.arm;

public class ARMPopNode extends ARMNode {

  Register reg;

  public ARMPopNode(Register reg) {
    this.reg = reg;
  }

  public String toString() {
    instruction = String.format("\tPOP {%s}\n", reg);
    return instruction;
  }

}
