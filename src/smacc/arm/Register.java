package smacc.arm;

//CREATED AS WE HAVE A SPECIAL REGISTER (SP)
public class Register {

  public enum SpecialReg {
    pc, lr, sp, r0, r1, r2, r3, r10, r11
  }

  private SpecialReg specialReg = null;
  private int register;

  // Construct standard register
  public Register(int startReg) {
    this.register = startReg;
  }

  // Construct special register
  public Register(SpecialReg specialReg) {
    this.specialReg = specialReg;
    this.register = 0;
  }

  public int getRegisterNumber() {
    if (specialReg != null) {
      if (specialReg.equals(SpecialReg.r10)) {
        return 10;
      }
      if (specialReg.equals(SpecialReg.r11)) {
        return 11;
      }
    }
    return register;
  }

  public void nextReg() {
    register++;
  }

  public Register getNextReg() {
    return new Register(register + 1);
  }

  public Register getPrevReg() {
    return new Register(register - 1);
  }

  // public Register clone(){
  // return new Register(register);
  // }

  public void decReg() {
    register--;
  }

  public boolean equals(Object o) {
    // If operand is just a register with no offset it should equals a register
    // object of the same type
    if (o instanceof OperandTwo) {
      OperandTwo other = (OperandTwo) o;
      return other.getShiftType() == null && this.equals(other.getRegister());
    } else if (o instanceof Register) {
      Register reg = (Register) o;
      return specialReg.equals(reg.specialReg) && register == reg.register;
    }
    return false;
  }

  public int hashCode() {
    return register + 67452 * specialReg.ordinal();
  }

  // If it has a special register it returns it's string representation
  // if not it returns the current register;
  // At the top of the Translator a global Register variable is held that is
  // incremented and decremented accordingly.
  public String toString() {
    if (specialReg != null) {
      return specialReg.name();
    }
    return "r" + register;
  }
}
