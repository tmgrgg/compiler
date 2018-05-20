package smacc.arm;

public class ARMMulNode extends ARMNode {
  public enum MulComparator {
    SMULL
  }

  MulComparator comparator;
  Register regHigher32Bits;
  Register regLower32Bits;
  Register regOp1;
  Register regOp2;

  public ARMMulNode(MulComparator comparator, Register regLower32Bits,
      Register regHigher32Bits, Register regOp1, Register regOp2) {
    this.comparator = comparator;
    this.regLower32Bits = regLower32Bits;
    this.regHigher32Bits = regHigher32Bits;
    this.regOp1 = regOp1;
    this.regOp2 = regOp2;
  }

  @Override
  public String toString() {
    instruction = String.format("\t%s %s, %s, %s, %s\n", comparator.name(),
        regLower32Bits, regHigher32Bits, regOp1, regOp2);
    return instruction;
  }

}
