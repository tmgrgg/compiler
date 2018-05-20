package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

public abstract class WACCType {

  // Public types used for equality checking in compiler

  public static final WACCType TYPE_ANY = new AnyType();
  public static final WACCType TYPE_CHAR = new CharType();
  public static final WACCType TYPE_INT = new IntType();
  public static final WACCType TYPE_BOOL = new BoolType();
  public static final WACCType TYPE_STRING = new ArrayType(TYPE_CHAR, 1);
  public static final WACCType TYPE_ARRAY_ANY = new ArrayType(TYPE_ANY, -1);
  public static final WACCType TYPE_PAIR_ANY = new PairType(TYPE_ANY, TYPE_ANY);
  public static final WACCType TYPE_INT_OR_CHAR = new IntOrCharType();

  // Supported on all types
  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_ANY);
  }

  public static void visitLiteral(
      @NotNull WACCParser.String_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_ANY);
  }

  // Supported only on array types
  public WACCType getBaseType() {
    throw new UnsupportedOperationException();
  }

  public int getDimensions() {
    throw new UnsupportedOperationException();
  }

  // Supported only on pair types
  public WACCType getLeft() {
    throw new UnsupportedOperationException();
  }

  public WACCType getRight() {
    throw new UnsupportedOperationException();
  }

  // Return base size of type
  public abstract int getSizeInBytes();
}
