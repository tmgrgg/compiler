package smacc.ast.expr;

import java.util.List;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Of form 'a[3][5]'
public class ArrayElemNode extends ExprNode {

  // Ident for the whole array, i.e. 'a' in the above example
  private IdentNode ident;
  // Indices to specific element of array, i.e [3, 5] in above example
  private List<ExprNode> indices;

  public ArrayElemNode(IdentNode ident, List<ExprNode> indices,
      WACCType evaluatedType) {
    super(evaluatedType);
    this.ident = ident;
    this.indices = indices;
  }

  public IdentNode getIdent() {
    return ident;
  }

  public int getIndexCount() {
    return indices.size();
  }

  public ExprNode getIndex(int i) {
    return indices.get(i);
  }

  public String toString() {
    String output = ident.toString();
    for (ExprNode index : indices) {
      output += ("[" + index + "]");
    }
    return output;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitArrayElemNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateArrayElemNode(this);

  }
}
