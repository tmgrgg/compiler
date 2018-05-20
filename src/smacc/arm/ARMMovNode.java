package smacc.arm;

public class ARMMovNode extends ARMNode {
  public enum MovComparator {
    MOV, MOVEQ, MOVNE, MOVGT, MOVLE, MOVGE, MOVLT
  }

  // MovNode is able to handle the case where srcValue is not a register,
  // although not sure if this is necessary.
  Register destReg;
  OperandTwo operandTwo;
  MovComparator comparator;

  public ARMMovNode(Register destReg, OperandTwo operandTwo,
      MovComparator comparator) {
    this.destReg = destReg;
    this.operandTwo = operandTwo;
    this.comparator = comparator;
  }

  public String toString() {
    instruction = String.format("\t%s %s, %s\n", comparator.name(), destReg,
        operandTwo);
    return instruction;
  }
}
