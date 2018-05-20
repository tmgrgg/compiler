package smacc.ast;

public abstract class ASTNode implements Translatable {
  protected int index;
  private static int indexCounter = 1;

  public ASTNode() {
    this.index = indexCounter++;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public static void resetIndexCounter() {
    indexCounter = 1;
  }

  public static int getIndexCounter() {
    return indexCounter;
  }

  public static void setIndexCounter(int indexCounter) {
    ASTNode.indexCounter = indexCounter;
  }

  public abstract void visit(ASTVisitorListener... listener);
}