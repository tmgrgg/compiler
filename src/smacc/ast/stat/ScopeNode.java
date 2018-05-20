package smacc.ast.stat;

import smacc.SymbolTable;
import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;

public class ScopeNode extends StatNode {

  private SymbolTable scope;
  private StatNode stat;

  public ScopeNode(StatNode stat, SymbolTable scope) {
    this.scope = scope;
    this.stat = stat;
  }

  public void setStat(StatNode stat) {
    this.stat = stat;
  }

  public SymbolTable getSymbolTable() {
    return scope;
  }

  public StatNode getStat() {
    return stat;
  }

  public String toString() {
    return "<SCOPE>";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitScopeNode(this);
    }
    stat.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateScopeNode(this);
  }
}
