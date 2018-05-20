package smacc.arm;

//  This class could be used in the ARMMoveNode as the srcvalue

/*
 * OperandTwo Class
 * Used to handle multiple different types of second argument for arm instructions
 */
public class OperandTwo {

  public enum ShiftType {
    LSL, ASL, ASR, LSR
  }

  private ShiftType shiftType;
  private int immediate;
  private Register register;
  private boolean isChar = false;
  private int shiftValue;

  // Used to construct with immediate
  public OperandTwo(int immediate, boolean isChar) {
    this.isChar = isChar;
    this.immediate = immediate;
    shiftType = null;
    register = null;
  }

  // Used to construct with register
  public OperandTwo(Register register) {
    this.register = register;
    this.shiftType = null;
  }

  // Used to construct with register with shift
  public OperandTwo(Register register, ShiftType shiftType, int shiftValue) {
    this.register = register;
    this.shiftType = shiftType;
    this.shiftValue = shiftValue;
  }

  public ShiftType getShiftType() {
    return shiftType;
  }

  public int getImmediate() {
    return immediate;
  }

  public Register getRegister() {
    return register;
  }

  public boolean equals(Object o) {
    // If operand is just a register with no offset it should equals a register
    // object of the same type
    if (o instanceof Register) {
      Register reg = (Register) o;
      return shiftType == null && reg.equals(register);
    } else if (o instanceof OperandTwo) {
      OperandTwo other = (OperandTwo) o;
      return shiftType.equals(other.shiftType) && register == other.register
          && register.equals(register);
    }
    return false;
  }

  public int hashCode() {
    return shiftType.hashCode() * register.hashCode() * shiftType.hashCode();
  }

  public String toString() {

    String operand2;
    if (shiftType == null && register == null) {
      operand2 = "#";

      if (isChar) {
        operand2 += "'" + (char) immediate + "'";
      } else {
        operand2 += immediate;
      }

      return operand2;
    }

    operand2 = register.toString();
    if (shiftType != null) {
      operand2 += ", " + shiftType.name() + " #" + shiftValue;
    }

    return operand2;

  }

}
