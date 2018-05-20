package smacc.types;

import java.util.Stack;

import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer;
import antlr.WACCParser;

/*
 * PairType
 * 
 * Holds two WACCTypes corresponding to left and right
 */
public class PairType extends WACCType {
  private WACCType left;
  private WACCType right;

  public PairType(WACCType left, WACCType right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public WACCType getLeft() {
    return left;
  }

  @Override
  public WACCType getRight() {
    return right;
  }

  // Visiting parser token for getting the first or second element of a pair
  public static boolean visitElem(@NotNull WACCParser.Pair_elemContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    WACCType pairType = typestack.pop();

    // Cases always disjoint
    if (ctx.FST() != null) {
      typestack.push(pairType.getLeft());
    } else if (ctx.SND() != null) {
      typestack.push(pairType.getRight());
    }

    return (ctx.FST() != null);
  }

  public static void visitType(@NotNull WACCParser.Pair_typeContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {

    WACCType rightType = typestack.pop();
    WACCType leftType = typestack.pop();

    typestack.push(new PairType(leftType, rightType));
  }

  public static void visitLiteral(@NotNull WACCParser.Pair_literalContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    // Can only be null pair which we represent by the most general pair
    typestack.push(TYPE_PAIR_ANY);
  }

  public static void visitElemType(
      @NotNull WACCParser.Pair_elem_typeContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {
    if (ctx.PAIR() != null) {
      typestack.push(TYPE_PAIR_ANY);
    }
  }

  @Override
  public int getSizeInBytes() {
    // Size of a pointer
    return 4;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AnyType)
      return true;
    if (!(o instanceof PairType)) {
      return false;
    }
    PairType other = (PairType) o;
    return other.left.equals(left) && other.right.equals(right);

  }

  @Override
  public int hashCode() {
    return left.hashCode() + right.hashCode() * 245235;
  }

  @Override
  public String toString() {
    return String.format("(%s,%s)", left.toString(), right.toString());
  }
}