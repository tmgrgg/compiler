package smacc.arm;

public class ARMLdrNode extends ARMNode {
  public enum LdrComparator {
    LDR, LDRNE, LDREQ, LDRSB, LDRLT, LDRCS
  }

  // Could possibly make a class like operandtwo for second pard of a ldr instr.
  Register destReg;
  Register memReg = null;
  ARMLabel label;
  LdrComparator comparator;
  int immediate;
  int offset = 0;

  // offset

  public ARMLdrNode(Register destReg, int immediate, LdrComparator comparator) {
    this.destReg = destReg;
    this.immediate = immediate;
    this.comparator = comparator;
  }

  public ARMLdrNode(Register destReg, Register memReg, int offset,
      LdrComparator comparator) {
    this.destReg = destReg;
    this.memReg = memReg;
    this.offset = offset;
    this.comparator = comparator;
  }

  public ARMLdrNode(Register destReg, ARMLabel label, LdrComparator comparator) {
    this.destReg = destReg;
    this.label = label;
    this.comparator = comparator;
  }

  public String toString() {
    instruction = String.format("\t%s %s, ", comparator.name(), destReg);
    if (!(label == null)) {
      instruction += "=" + label;
    } else if (memReg != null) {
      instruction += "[" + memReg + ((offset == 0) ? "" : (", #" + offset))
          + "]";
    } else {
      instruction += "=" + immediate;
    }

    instruction += "\n";
    return instruction;
  }

}
