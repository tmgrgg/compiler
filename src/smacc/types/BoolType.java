package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

public class BoolType extends WACCType {

  public BoolType() {

  }

  public static void pushType(Stack<WACCType> typestack) {
    typestack.push(TYPE_BOOL);
  }

  public static boolean isType(Object o) {
    return TYPE_BOOL.equals(o);
  }

  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_BOOL);
  }

  public static void visitLiteral(@NotNull WACCParser.Bool_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_BOOL);
  }

  @Override
  public boolean equals(Object o) {
    return ((o instanceof BoolType) || (o instanceof AnyType));
  }

  @Override
  public int getSizeInBytes() {
    return 1;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "bool";
  }
}
