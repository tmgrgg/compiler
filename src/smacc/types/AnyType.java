package smacc.types;

/*
 * AnyType that equals any WACCType
 * Is used in the compiler to continue checking for errors after a failed
 * typecheck as well as for general utility
 */

public class AnyType extends WACCType {
  @Override
  public boolean equals(Object o) {
    return (o instanceof WACCType);
  }

  @Override
  public int getSizeInBytes() {
    return 4;
  }
}
