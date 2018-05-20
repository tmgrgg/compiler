package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

public class CharType extends WACCType {

  public CharType() {

  }

  public static void pushType(Stack<WACCType> typestack) {
    typestack.push(TYPE_CHAR);
  }

  public static boolean isType(Object o) {
    return TYPE_CHAR.equals(o);
  }

  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_CHAR);
  }

  public static void visitLiteral(@NotNull WACCParser.Char_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_CHAR);
  }

  @Override
  public int getSizeInBytes() {
    return 1;
  }

  @Override
  public boolean equals(Object o) {
    return ((o instanceof CharType) || (o instanceof IntOrCharType) || (o instanceof AnyType));
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "char";
  }
}