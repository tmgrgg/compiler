package smacc.arm;

public class ARMBranchNode extends ARMNode {
  public enum BranchComparator {
    BEQ, BLEQ, BLNE, BL, BLCS, BLVS, B, BLLT
  }

  public ARMBranchNode(String label, BranchComparator comparator) {
    instruction = "\t" + comparator.name() + " " + label + '\n';
  }

  public String toString() {
    return instruction;
  }
}
