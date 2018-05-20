package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import smacc.ErrorMessageContainer.ErrorCode;
import smacc.ast.expr.IntLiteralNode;
import smacc.types.WACCType;

import antlr.WACCParser;

public class IntType extends WACCType {

  public static final long INTEGER_MAX_VALUE = 2147483647;
  public static final long INTEGER_MIN_VALUE = -2147483648;

  public IntType() {

  }

  public static void pushType(Stack<WACCType> typestack) {
    typestack.push(TYPE_INT);
  }

  public static boolean isType(Object o) {
    return TYPE_INT.equals(o);
  }

  public static void visitType(@NotNull ParserRuleContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    typestack.push(TYPE_INT);
  }

  public static IntLiteralNode visitLiteral(
      @NotNull WACCParser.Int_literalContext ctx, ErrorMessageContainer errors,
      Stack<WACCType> typestack) {
    long x = Long.parseLong(ctx.INT_LITERAL().getText());
    typestack.push(TYPE_INT);
    if (x > INTEGER_MAX_VALUE) {
      errors.add(ctx, "Int literal % is too large, over 2^31 - 1");
      errors.setErrorType(ErrorCode.SYNTACTIC_ERR);
      return new IntLiteralNode(-1);
    }
    if (x < INTEGER_MIN_VALUE) {
      errors.add(ctx, "Int literal % is too small, under -2^31");
      errors.setErrorType(ErrorCode.SYNTACTIC_ERR);
      return new IntLiteralNode(-1);
    }
    return new IntLiteralNode((int) x);
  }

  @Override
  public int getSizeInBytes() {
    return 4;
  }

  @Override
  public boolean equals(Object o) {
    return ((o instanceof IntType) || (o instanceof IntOrCharType) || (o instanceof AnyType));
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "int";
  }
}