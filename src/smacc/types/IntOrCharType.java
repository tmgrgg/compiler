package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

public class IntOrCharType extends WACCType {

  /*
   * Exists for checking certain binary operator types
   */
  public IntOrCharType() {

  }

  public static void pushType(Stack<WACCType> typestack) {
    typestack.push(TYPE_INT_OR_CHAR);
  }

  public static boolean isType(Object o) {
    return TYPE_INT_OR_CHAR.equals(o);
  }

  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_INT_OR_CHAR);
  }

  public static void visitLiteral(@NotNull WACCParser.Char_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_INT_OR_CHAR);
  }

  @Override
  public boolean equals(Object o) {
    return ((o instanceof IntOrCharType) || (o instanceof AnyType)
        || (o instanceof IntType) || (o instanceof CharType));
  }

  @Override
  public int getSizeInBytes() {
    // Returns largest possible value to avoid underallocating
    return 4;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "int/char";
  }
}
