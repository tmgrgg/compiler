package smacc.arm;

public class ARMCmpNode extends ARMNode {
  Register register;
  OperandTwo operandTwo;

  public ARMCmpNode(Register register, OperandTwo operandTwo) {
    this.register = register;
    this.operandTwo = operandTwo;
  }

  public String toString() {
    instruction = String.format("\tCMP %s, %s\n", register, operandTwo);
    return instruction;
  }
}
