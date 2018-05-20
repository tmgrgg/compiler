package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//This node will hold a binary op enum and have two Expr children
public class BinaryOpNode extends ExprNode {

  private BinOp op;
  private ExprNode left;
  private ExprNode right;

  public BinaryOpNode(String op, ExprNode left, ExprNode right,
      WACCType evaluatedType) {
    super(evaluatedType);
    switch (op) {
      case "*":
        this.op = BinOp.MULTIPLY;
        break;
      case "/":
        this.op = BinOp.DIVIDE;
        break;
      case "%":
        this.op = BinOp.MODULUS;
        break;
      case "+":
        this.op = BinOp.PLUS;
        break;
      case "-":
        this.op = BinOp.MINUS;
        break;
      case ">":
        this.op = BinOp.GREATER;
        break;
      case ">=":
        this.op = BinOp.GEQ;
        break;
      case "<":
        this.op = BinOp.LESS;
        break;
      case "<=":
        this.op = BinOp.LEQ;
        break;
      case "==":
        this.op = BinOp.EQUALS;
        break;
      case "!=":
        this.op = BinOp.NOTEQUALS;
        break;
      case "&&":
        this.op = BinOp.LOGICAL_AND;
        break;
      case "||":
        this.op = BinOp.LOGICAL_OR;
        break;
    }
    this.left = left;
    this.right = right;
  }

  public BinOp getOp() {
    return op;
  }

  public ExprNode getLeft() {
    return left;
  }

  public ExprNode getRight() {
    return right;
  }

  @Override
  public String toString() {
    return op.name();
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitBinaryOpNode(this);
    }
    left.visit(listeners);
    right.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateBinaryOpNode(this);
  }

}
