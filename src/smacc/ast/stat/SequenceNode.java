package smacc.ast.stat;

import java.util.ArrayList;
import java.util.List;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;

//  Simply holds a block of statements that are sequentially composed.
//  Each statement is a child.
//  This is like a semicolon would go between each child.
public class SequenceNode extends StatNode {

  // List is probably easier than having lots of SeqNodes
  // with a StatNode and a SeqNode as its children
  private List<StatNode> stmts;

  // Initialise stmts to empty. Must add StatNode's using addChild

  public SequenceNode() {
    this.stmts = new ArrayList<>();
  }

  public SequenceNode(StatNode stat) {
    this.stmts = new ArrayList<>();
    stmts.add(stat);
  }

  // Adds to end of list
  public void addChild(StatNode stat) {
    stmts.add(stat);
    setIndex(Math.max(this.index, stat.getIndex() + 1));
  }

  // Starting from 0
  public StatNode getStat(int i) {
    return stmts.get(i);
  }

  public int getStatCount() {
    return stmts.size();
  }

  public String toString() {
    return "<SEQUENCE>";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitSequenceNode(this);
    }
    for (StatNode stat : stmts) {
      stat.visit(listeners);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateSequenceNode(this);
  }
}
