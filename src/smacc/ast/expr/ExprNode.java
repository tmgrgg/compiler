package smacc.ast.expr;

import smacc.ast.ASTNode;
import smacc.types.WACCType;

public abstract class ExprNode extends ASTNode {

  // Current scope is in WACC_AST_Visitor as a global symbol
  // table, which will revert back to old scopes when required

  protected WACCType evaluatedType;

  public ExprNode() {
    this.evaluatedType = WACCType.TYPE_ANY;
  }

  public ExprNode(WACCType evaluatedType) {
    this.evaluatedType = evaluatedType;
  }

  public enum UnOp {
    LOGICAL_NOT, NEGATION, LEN, ORD, CHR
  };

  public enum BinOp {
    MULTIPLY, DIVIDE, MODULUS, PLUS, MINUS, GREATER, GEQ, LESS, LEQ, EQUALS, NOTEQUALS, LOGICAL_AND, LOGICAL_OR
  };

  public int getSizeInBytes() {
    return this.evaluatedType.getSizeInBytes();
  }

  public WACCType getType() {
    return evaluatedType;
  }

}
