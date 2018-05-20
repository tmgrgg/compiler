package smacc.arm;

public class ARMLogicalNode extends ARMNode {
  public enum LogicalComparator {
    AND, ORR
  }

  LogicalComparator comparator;
  Register dstReg;
  Register op1Reg;
  OperandTwo op2;

  public ARMLogicalNode(LogicalComparator comparator, Register dstReg,
      Register op1Reg, OperandTwo op2) {
    this.comparator = comparator;
    this.dstReg = dstReg;
    this.op1Reg = op1Reg;
    this.op2 = op2;
  }

  @Override
  public String toString() {
    instruction = String.format("\t%s %s, %s, %s\n", comparator.name(), dstReg,
        op1Reg, op2);
    return instruction;
  }

}
