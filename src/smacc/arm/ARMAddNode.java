package smacc.arm;

public class ARMAddNode extends ARMNode {

  public enum AddComparator {
    ADD, ADDS
  }

  AddComparator comparator;
  Register destReg;
  Register regOp1;
  OperandTwo op2;

  public ARMAddNode(Register destReg, Register regOp1, OperandTwo op2,
      AddComparator comparator) {
    this.comparator = comparator;
    this.destReg = destReg;
    this.regOp1 = regOp1;
    this.op2 = op2;
  }

  public String toString() {
    instruction = String.format("\t%s %s, %s, %s\n", comparator.name(),
        destReg, regOp1, op2);
    return instruction;
  }
}
