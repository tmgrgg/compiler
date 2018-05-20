package smacc.arm;

public class ARMStrNode extends ARMNode {
  public enum StrComparator {
    STR, STRB
  }

  // MovNode is able to handle the case where srcValue is not a register,
  // although not sure if this is necessary.
  StrComparator comparator;
  Register src;
  Register dst;
  int offset;
  boolean changeDst;

  public ARMStrNode(Register src, Register dst, int offset, boolean changeDst,
      StrComparator comparator) {
    this.comparator = comparator;
    this.src = src;
    this.dst = dst;
    this.offset = offset;
    this.changeDst = changeDst;
  }

  public String toString() {
    instruction = String.format("\t%s %s, [%s%s]%s\n", comparator.name(), src,
        dst, (offset == 0) ? "" : ", #" + offset, changeDst ? "!" : "");
    return instruction;
  }
}
