package smacc.ast;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import smacc.Function;
import smacc.FunctionTable;
import smacc.ast.expr.ArrayElemNode;
import smacc.ast.expr.ArrayLiteralNode;
import smacc.ast.expr.BinaryOpNode;
import smacc.ast.expr.BoolLiteralNode;
import smacc.ast.expr.CallNode;
import smacc.ast.expr.CharLiteralNode;
import smacc.ast.expr.IdentNode;
import smacc.ast.expr.IntLiteralNode;
import smacc.ast.expr.NewPairNode;
import smacc.ast.expr.PairElemNode;
import smacc.ast.expr.UnaryOpNode;
import smacc.ast.stat.*;

public class ASTStaticViewer implements ASTVisitorListener {

  public enum GraphFormat {
    PNG, PS, SVG;
    public String getCommand() {
      switch (this) {
        case PNG:
          return "-Tpng";
        case PS:
          return "-Tps";
        default: // SVG
          return "-Tsvg";
      }
    }
  };

  /*
   * Given a function table and a filename go through each defined function and
   * the table's main method and produce a dot source This may be compiled with
   * the specified format to the given filename
   */
  public static void constructGraphFromFunctiontable(FunctionTable functable,
      String fileout, boolean compileGraph, GraphFormat format,
      boolean graphStructured, int feedbackLevel) {

    String sourcename = "ast.dot";

    // If we are not compiling the graph then the static viewer should produce a
    // source file
    // based on the filename given by the user
    ASTStaticViewer viewer = new ASTStaticViewer(compileGraph ? sourcename
        : fileout, graphStructured);

    Iterator<Map.Entry<String, HashSet<Function>>> iterator = functable
        .iterator();

    // Prototype
    while (iterator.hasNext()) {
      Entry<String, HashSet<Function>> mapping = iterator.next();
      for (Function func : mapping.getValue()) {
        viewer.prototype(func.getId(), func);
      }
    }
    viewer.prototype("main", functable.getMain());

    // Iterate through and visit defined functions
    iterator = functable.iterator();
    while (iterator.hasNext()) {
      Entry<String, HashSet<Function>> mapping = iterator.next();
      for (Function func : mapping.getValue()) {
        if (feedbackLevel > 1)
          System.out.printf("Drawing %s\n", mapping.getKey());
        viewer.visitFunction(func);
      }
    }

    // Visit main
    if (feedbackLevel > 1)
      System.out.printf("Drawing main\n");
    viewer.visitFunction(functable.getMain());
    viewer.end();

    if (compileGraph) {
      try {
        if (feedbackLevel > 1) {
          System.out.printf("Compiling %s to %s\n", sourcename, fileout);
        }
        Runtime.getRuntime().exec(
            String.format("dot %s %s -o %s", format.getCommand(), sourcename,
                fileout));
        // Delete source?
      } catch (IOException e) {
        System.err.printf("SMACC: Could not compile graph source %s",
            sourcename);
        e.printStackTrace();
      }
    }
  }

  // Stylings of nodetypes
  private static final String functionNodeStyle = "shape=tripleoctagon,";
  private static final String statNodeStyle = "shape=box,";
  private static final String intermediateNodeStyle = "shape=trapezium,";

  // Dot variables relating to nodes are simply integers, we keep track of the
  // next to assign with
  // currentVar
  private FileWriter outputStream;
  private int currentVar;
  private Hashtable<ASTNode, Integer> varTable;

  // Dot variables for functions are integers prefixed by 'func'
  // kept track of with currentFunc
  private Hashtable<Function, Integer> funcLabels;
  private int currentFunc;

  // In a few special cases intermediate nodes are drawn that do not exist in
  // the ast, these are counted
  // with currentIntermediate
  private int currentIntermediate;

  // Denotes whether call nodes should draw an arrow to the function they call
  private boolean isStructured = true;

  public ASTStaticViewer(String filename, boolean isStructured) {
    this.isStructured = isStructured;
    this.varTable = new Hashtable<ASTNode, Integer>();
    this.funcLabels = new Hashtable<Function, Integer>();
    try {
      outputStream = new FileWriter(filename);
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeHeader();
  }

  private void writeHeader() {
    try {
      outputStream.append("digraph ast{\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void end() {
    try {
      outputStream.append("}");
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
   * Use hashtable to lookup or define node If node needs defining add source
   * overhead such as border type
   */
  private int getVar(ASTNode node) {
    Integer var = varTable.get(node);
    if (var == null) {
      var = currentVar;
      currentVar++;
      varTable.put(node, var);
      String shapeMod = "";
      if (node instanceof StatNode) {
        shapeMod = statNodeStyle;
      }
      String s = String.format("  %d [%slabel=\"(%s) %s\"];\n", var, shapeMod,
          node.getIndex(), node.toString());
      try {
        outputStream.append(s);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
    return var;
  }

  public void prototype(String s, Function f) {

    funcLabels.put(f, currentFunc);

    String funcVarName = "func" + Integer.toString(currentFunc);
    String def = String.format("  %s [%slabel=\"%s\"];\n", funcVarName,
        functionNodeStyle, s);

    try {
      outputStream.write(def);
    } catch (IOException e) {
      e.printStackTrace();
    }
    currentFunc++;
  }

  /*
   * Lookup or define function from hashtable If node needs defining add source
   * overhead such as border type
   */

  public void visitFunction(Function f) {

    String funcVarName = String.format("func%d", funcLabels.get(f));

    ASTNode body = f.getBody();
    writeRelation(funcVarName, body);
    body.visit(this);

    currentFunc++;
  }

  private void writeRelation(ASTNode node1, ASTNode node2) {
    int var1 = getVar(node1);
    int var2 = getVar(node2);
    writeArrow(Integer.toString(var1), Integer.toString(var2));
  }

  private void writeRelation(String string, ASTNode node) {
    int var = getVar(node);
    writeArrow(string, Integer.toString(var));
  }

  private void writeRelation(ASTNode node, String string) {
    int var = getVar(node);
    writeArrow(Integer.toString(var), string);
  }

  private void writeRelation(ASTNode node, Function func) {
    int var = getVar(node);
    int funcVar = funcLabels.get(func);
    writeArrow(Integer.toString(var), String.format("func%d", funcVar));
  }

  private void writeArrow(String a, String b) {
    String s = String.format("  %s -> %s;\n", a, b);
    try {
      outputStream.write(s);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
   * Check if an ASTNode is a sequence node and if so draw in the correct way
   * Intermediate is an - used to output a node not in the ast that is between
   * it and the children, for if statements mainly
   */
  private void writeIntermediate(ASTNode prev, ASTNode node, String intermediate) {

    String interName = String.format("intermediate%d", currentIntermediate);
    String def = String.format("  %s [%slabel=\"%s\"];\n", interName,
        intermediateNodeStyle, intermediate);
    try {
      outputStream.write(def);
    } catch (IOException e) {
      e.printStackTrace();
    }
    writeRelation(prev, interName);
    writeRelation(interName, node);

    currentIntermediate++;
  }

  @Override
  public void visitArrayElemNode(ArrayElemNode node) {
    writeRelation(node, node.getIdent());
    for (int i = 0; i < node.getIndexCount(); i++) {
      writeRelation(node, node.getIndex(i));
    }
  }

  @Override
  public void visitArrayLiteralNode(ArrayLiteralNode node) {
  }

  @Override
  public void visitBinaryOpNode(BinaryOpNode node) {
    writeRelation(node, node.getLeft());
    writeRelation(node, node.getRight());
  }

  @Override
  public void visitBoolLiteralNode(BoolLiteralNode node) {
  }

  @Override
  public void visitCallNode(CallNode node) {
    if (!isStructured)
      writeRelation(node, node.getFunction());
    for (int i = 0; i < node.getArgCount(); i++) {
      writeRelation(node, node.getArg(i));
    }
  }

  @Override
  public void visitCharLiteralNode(CharLiteralNode node) {

  }

  @Override
  public void visitIdentNode(IdentNode node) {

  }

  @Override
  public void visitIntLiteralNode(IntLiteralNode node) {

  }

  @Override
  public void visitNewPairNode(NewPairNode node) {
    writeRelation(node, node.getLeft());
    writeRelation(node, node.getRight());
  }

  @Override
  public void visitPairElemNode(PairElemNode node) {

  }

  @Override
  public void visitUnaryOpNode(UnaryOpNode node) {
    writeRelation(node, node.getArg());
  }

  @Override
  public void visitAssignmentNode(AssignmentNode node) {
    writeRelation(node, node.getLHS());
    writeRelation(node, node.getRHS());
  }

  @Override
  public void visitIfNode(IfNode node) {
    writeIntermediate(node, node.getConditional(), "Conditional");
    writeIntermediate(node, node.getTrueBody(), "True");
    writeIntermediate(node, node.getFalseBody(), "False");
  }

  @Override
  public void visitPrintNode(PrintNode node) {
    writeRelation(node, node.getExpr());
  }

  @Override
  public void visitReadNode(ReadNode node) {
    writeRelation(node, node.getExpr());
  }

  public void visitFreeNode(FreeNode node) {
    writeRelation(node, node.getExpr());
  }

  @Override
  public void visitReturnNode(ReturnNode node) {
    writeRelation(node, node.getExpr());
  }

  @Override
  public void visitScopeNode(ScopeNode node) {
    writeRelation(node, node.getStat());
  }

  @Override
  public void visitWhileNode(WhileNode node) {
    writeIntermediate(node, node.getConditional(), "Conditional");
    writeIntermediate(node, node.getBody(), "Body");
  }

  @Override
  public void visitSequenceNode(SequenceNode node) {

    for (int i = 0; i < node.getStatCount(); i++) {
      writeRelation(node, node.getStat(i));
    }

  }

  @Override
  public void visitExitNode(ExitNode node) {
    writeRelation(node, node.getExpr());
  }
}
