package smacc.arm;

public class ARMSubNode extends ARMNode {

  public enum SubComparator {
    SUB, SUBS, RSBS
  }

  SubComparator comparator;
  Register destReg;
  Register regOp1;
  OperandTwo op2;

  public ARMSubNode(Register destReg, Register regOp1, OperandTwo op2,
      SubComparator comparator) {
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
