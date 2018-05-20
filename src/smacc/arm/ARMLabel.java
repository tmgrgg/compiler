package smacc.arm;

public class ARMLabel extends ARMNode {
  String label;
  String instruction;

  private static int lCount = 0;
  private static int mCount = 0;

  public ARMLabel(String label) {
    this.label = label;
    instruction = String.format("%s:\n", label);
  }

  // Use this Constructor for predefined labels, could use an enum if
  // more predefined lables are needed.
  public ARMLabel(boolean isMessage) {
    if (!isMessage) {
      instruction = String.format("L%s:\n", lCount);
      label = "L" + lCount;
      lCount++;
    } else {
      instruction = String.format("msg_%s", mCount);
      mCount++;
    }
  }

  public ARMLabel(String label, boolean isMessageOperand) {
    if (isMessageOperand) {
      this.label = label;
      this.instruction = label;
    } else {
      System.out.println("Error - Shouldn't construct an ARMLabel with "
          + "parameters (String s, false)");
    }
  }

  // compare labels only
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!o.getClass().equals(this.getClass())) {
      return false;
    }

    ARMLabel otherLabel = (ARMLabel) o;

    return (otherLabel.getLabel()).equals(this.getLabel());
  }

  public String getLabel() {
    return label;
  }

  public String toString() {
    return instruction;
  }
}
