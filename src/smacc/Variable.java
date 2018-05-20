package smacc;

import smacc.types.WACCType;

/*
 * Variable class used to hold the type and stack offset
 * 
 * So that register optimisation can be done in the future we also hold a
 * temporary register and the birth and death indices of the variable.
 */

public class Variable {

  WACCType type;
  int register;
  int offset;
  int birthIndex;
  int deathIndex;

  public Variable(WACCType type) {
    this.birthIndex = 0;
    this.deathIndex = 0;
    this.type = type;
  }

  public int getBirthIndex() {
    return birthIndex;
  }

  public void setBirthIndex(int birthIndex) {
    this.birthIndex = birthIndex;
  }

  public int getDeathIndex() {
    return deathIndex;
  }

  public void setDeathIndex(int deathIndex) {
    this.deathIndex = deathIndex;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public WACCType getType() {
    return type;
  }

  public int getSizeInBytes() {
    return ((type.equals(WACCType.TYPE_CHAR) || type.equals(WACCType.TYPE_BOOL)) ? 1
        : 4);
  }

  // For testing
  public String printLiveRange() {
    String s = ": " + birthIndex + " to " + deathIndex;
    return s;
  }
}
