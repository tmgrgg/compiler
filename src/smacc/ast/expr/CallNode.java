package smacc.ast.expr;

import smacc.Function;
import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

import java.util.ArrayList;
import java.util.List;

//  Always RHS of an assignment - "call f(1, x + 1, true, a[3])"
public class CallNode extends ExprNode {

  // Function object that this will point to
  private Function function;
  // List of ExprNode's which are the parameters for the function call
  private List<ExprNode> args = new ArrayList<>();

  public CallNode(Function function, List<ExprNode> argList,
      WACCType evaluatedType) {
    super(evaluatedType);
    this.function = function;
    this.args = argList;
  }

  public Function getFunction() {
    return function;
  }

  public int getArgCount() {
    return args.size();
  }

  public ExprNode getArg(int i) {
    return args.get(i);
  }

  public String toString() {
    String output = "<CALL " + function.getId() + " (";
    for (int i = 0; i < args.size(); i++) {
      output += args.get(i);
      if (i < args.size() - 1)
        output += ",";
    }
    output += ")>";
    return output;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitCallNode(this);
    }
    for (ExprNode arg : args) {
      arg.visit(listeners);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateCallNode(this);
  }
}
