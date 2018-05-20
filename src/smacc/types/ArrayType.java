package smacc.types;

import java.util.Stack;
import org.antlr.v4.runtime.misc.NotNull;
import smacc.ErrorMessageContainer;
import antlr.WACCParser;
import antlr.WACCParser.Array_typeContext;

/*
 * ArrayType
 * 
 * Holds a basetype and a number of dimensions
 * An array of dimensions -1 will equal with any array of the same basetype
 */

public class ArrayType extends WACCType {

  private WACCType baseType;
  private int dimensions;

  public static WACCType subtractDimensionFromType(ArrayType type) {
    if (type.getDimensions() > 1) {
      type.dimensions -= 1;
      return type;
    }
    return type.getBaseType();
  }

  public ArrayType(WACCType baseType, int dimensions) {
    this.baseType = baseType;
    this.dimensions = dimensions;
  }

  public WACCType getBaseType() {
    return baseType;
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AnyType)
      return true;
    // Check for other being a string and this being a char array
    if (o instanceof StringType && baseType instanceof CharType
        && dimensions == 1)
      return true;
    if (!(o instanceof ArrayType)) {
      return false;
    }
    ArrayType other = (ArrayType) o;
    // Check again for string and character arrays of a degree higher
    if (baseType instanceof CharType && other.baseType instanceof StringType) {
      return (dimensions == other.dimensions + 1);
    }
    if (baseType instanceof StringType && other.baseType instanceof CharType) {
      return (dimensions == other.dimensions - 1);
    }

    boolean typeEquality = other.baseType.equals(baseType)
        || baseType instanceof AnyType || other.baseType instanceof AnyType;
    boolean dimensionEquality = other.dimensions == dimensions
        || dimensions == -1 || other.dimensions == -1;

    return typeEquality && dimensionEquality;

  }

  /*
   * Visit array element parser node
   * 
   * Assumes already visited children and had typestack altered as a result ie
   * has the types of the indexing expr above the identifier type
   */
  public static void visitElem(@NotNull WACCParser.Array_elemContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {

    int accessedIndices = (ctx.getChildCount() - 1) / 3;

    // Get types of elements
    for (int i = 0; i < accessedIndices; i++) {
      if (!(typestack.pop().equals(TYPE_INT))) {
        errors.add(ctx, "Tried to index an array using a non-integer");
        typestack.push(TYPE_ANY);
        return;
      }
    }

    WACCType arrayType = typestack.pop();
    if (arrayType.equals(TYPE_STRING)) {
      arrayType = new ArrayType(TYPE_CHAR, 1);
    }
    if (!arrayType.equals(TYPE_ARRAY_ANY)) {
      errors.add(ctx, "Tried to index a non-array type %", arrayType);
      typestack.push(TYPE_ANY);
      return;
    }

    // If try to access more than basetype's dimensions
    if ((arrayType.getDimensions() - accessedIndices < 0)) {
      errors.add(ctx,
          "Tried to access array % with % dimensions to a degree of %", ctx
              .ident().getText(), arrayType.getDimensions(), accessedIndices);
      typestack.push(TYPE_ANY);
      return;
      // If try to access exactly basetype's dimensions
    } else if (arrayType.getDimensions() - accessedIndices == 0) {
      typestack.push(arrayType.getBaseType());
      // If try to access less than basetype's dimensions
    } else {
      typestack.push(new ArrayType(arrayType.getBaseType(), arrayType
          .getDimensions()
          - accessedIndices));
    }
  }

  public static void visitType(@NotNull Array_typeContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {

    // Assumes array basetype is on the top of the stack
    WACCType arrayElemType = typestack.pop();

    // Number of dimensions is calculated by the number of children
    // ie the number of square brackets
    int numDimensions = (ctx.getChildCount() - 1) / 2;

    // Special case to deal with strings
    if (arrayElemType.equals(TYPE_STRING)) {
      typestack.push(new ArrayType(TYPE_CHAR, numDimensions + 1));
    } else {
      typestack.push(new ArrayType(arrayElemType, numDimensions));
    }
  }

  public static void visitLiteral(@NotNull WACCParser.Array_literContext ctx,
      ErrorMessageContainer errors, Stack<WACCType> typestack) {

    if (ctx.getChildCount() == 2) {
      // Case with empty array []
      // This should match with any array so we push the most general array type
      typestack.push(new ArrayType(TYPE_ANY, -1));
      return;
    }

    int numberOfIndices = (ctx.getChildCount() - 1) / 2;
    WACCType arrayElem = typestack.pop();

    // Checking that all literal elements are of the same type
    for (int i = 1; i < numberOfIndices; i++) {
      if (!typestack.pop().equals(arrayElem)) {
        errors.add(ctx, "Unmatched index types in array literal");
        typestack.push(new ArrayType(TYPE_ANY, -1));
      }
    }
    // Special cases for string
    if (arrayElem.equals(TYPE_STRING)) {
      typestack.push(new ArrayType(TYPE_CHAR, 2));
    } else if (arrayElem.equals(TYPE_ARRAY_ANY)) {
      typestack.push(new ArrayType(arrayElem.getBaseType(), arrayElem
          .getDimensions() + 1));
    } else {
      typestack.push(new ArrayType(arrayElem, 1));
    }
  }

  @Override
  public int getSizeInBytes() {
    // Size of a pointer
    return 4;
  }

  @Override
  public int hashCode() {
    return baseType.hashCode() + dimensions * 35644;
  }

  @Override
  public String toString() {
    String s = baseType.toString();
    for (int i = 0; i < dimensions; i++) {
      s += "[]";
    }
    return s;
  }

}
