package smacc.ast.expr;

import java.util.List;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.ArrayType;
import smacc.types.WACCType;

//Example of form "[1, 2, 3]" or "['a', 'b']" etc.
public class ArrayLiteralNode extends ExprNode {

  private List<ExprNode> elements;
  boolean pureString;

  public static ArrayLiteralNode constructPureString(List<ExprNode> elements) {
    ArrayLiteralNode string = new ArrayLiteralNode(elements, new ArrayType(
        WACCType.TYPE_CHAR, 1));
    string.pureString = true;
    return string;
  }

  public static boolean isPureString(ArrayLiteralNode node) {
    return node.pureString;
  }

  public ArrayLiteralNode(List<ExprNode> elements, WACCType evaluatedType) {
    super(evaluatedType);
    this.elements = elements;
    pureString = false;
  }

  public int getLength() {
    return elements.size();
  }

  public int getSizeInBytes() {
    if (pureString) {
      return 4;
    }
    int size = 0;
    for (ExprNode node : elements) {
      if (node.getType().equals(WACCType.TYPE_PAIR_ANY)
          || node.getType().equals(WACCType.TYPE_ARRAY_ANY)) {
        size += 4;
      } else
        size += node.getSizeInBytes();
    }
    size += 4; // For holding the length of the array
    return size;
  }

  public ExprNode getElement(int i) {
    return elements.get(i);
  }

  // returns a normal string representation if its a Char Array
  @Override
  public String toString() {
    String output = "";
    if (pureString) {
      for (ExprNode exprNode : elements) {
        String charToAdd = Character.toString(((CharLiteralNode) exprNode)
            .getValue());
        switch (charToAdd) {
          case "\n":
            charToAdd = "\\n";
            break;
          case "\t":
            charToAdd = "\\t";
            break;
          case "\0":
            charToAdd = "\\0";
            break;
          case "\b":
            charToAdd = "\\b";
            break;
          case "\f":
            charToAdd = "\\f";
            break;
          case "\r":
            charToAdd = "\\r";
            break;
        }
        output += charToAdd;
      }
    } else {
      output = "[";
      for (ExprNode element : elements) {
        output += (element + ",");
      }
      output = output.substring(0, output.length() - 1);
      output += "]";
    }

    return output;

  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitArrayLiteralNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateArrayLiteralNode(this);
  }
}
