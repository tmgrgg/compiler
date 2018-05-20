package smacc.arm;

public class ARMOrNode extends ARMNode {

  public enum OrOperator {
    EOR
  }

  OrOperator operator;
  Register destReg;
  Register regOp1;
  OperandTwo op2;

  public ARMOrNode(Register destReg, Register regOp1, OperandTwo op2,
      OrOperator operator) {
    this.operator = operator;
    this.destReg = destReg;
    this.regOp1 = regOp1;
    this.op2 = op2;
  }

  public String toString() {
    instruction = String.format("\t%s %s, %s, %s\n", operator.name(), destReg,
        regOp1, op2);
    return instruction;
  }

}
