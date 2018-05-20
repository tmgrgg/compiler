package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

public class StringType extends WACCType {

  public StringType() {

  }

  public static void pushType(Stack<WACCType> typestack) {
    typestack.push(TYPE_STRING);
  }

  public static boolean isType(Object o) {
    return TYPE_STRING.equals(o);
  }

  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_STRING);
  }

  public static void visitLiteral(
      @NotNull WACCParser.String_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_STRING);
  }

  @Override
  public int getSizeInBytes() {
    // Size of a pointer
    return 4;
  }

  @Override
  public boolean equals(Object o) {
    // Checking for char array of dimensions 1
    if (o instanceof ArrayType) {
      ArrayType other = (ArrayType) o;
      return (other.getBaseType().equals(TYPE_CHAR) && other.getDimensions() == 1);
    }
    return ((o instanceof StringType) || (o instanceof AnyType));
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "string";
  }
}
