package smacc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;

import smacc.ErrorMessageContainer.ErrorCode;
import smacc.ast.*;
import smacc.ast.stat.*;
import smacc.ast.expr.*;
import smacc.exceptions.IdentifierDeclaredPreviouslyInCurrentScopeException;
import smacc.exceptions.IdentifierUndeclaredException;
import smacc.types.*;
import antlr.WACCParser;
import antlr.WACCParserBaseVisitor;

import java.util.ArrayList;

import java.util.List;
import java.util.Stack;

public class WACCFirstPass extends WACCParserBaseVisitor<ASTNode> {

  /*
   * WACCSemanticVisitor Walks over the parse-tree produced by antlr and checks
   * semantic integrity and builds an intermediate code-generation
   * representation (AST) in the same pass
   * 
   * 
   * Assumes given parsetree is syntactically valid.
   */

  /*
   * The typeStack has an associated contract when it is used by visitExpr and
   * all its possible children that require type-checking. The contract is: the
   * method pushes the type of the node it is visiting and pops the types of any
   * children. This allows for efficient type-checking.
   */
  private Stack<WACCType> typeStack = new Stack<WACCType>();

  private SymbolTable symbolTable = new SymbolTable();
  private smacc.FunctionTable funcTable;

  /*
   * References to WACCTypes used for equality checking and to avoid downcasting
   * or using instanceof
   */
  private static final WACCType TYPE_ANY = WACCType.TYPE_ANY;
  private static final WACCType TYPE_INT = WACCType.TYPE_INT;
  private static final WACCType TYPE_CHAR = WACCType.TYPE_CHAR;
  private static final WACCType TYPE_BOOL = WACCType.TYPE_BOOL;
  private static final WACCType TYPE_ARRAY_ANY = WACCType.TYPE_ARRAY_ANY;
  private static final WACCType TYPE_PAIR_ANY = WACCType.TYPE_PAIR_ANY;
  private static final WACCType TYPE_INT_OR_CHAR = WACCType.TYPE_INT_OR_CHAR;

  // Variables used when analysing function definitions
  private boolean inMain = false;
  private boolean functionHasReturn = false;
  private boolean isExiting = false;
  private Function currentFunction;

  private ErrorMessageContainer errors;

  int errorCode = 0;

  public WACCFirstPass(FunctionTable funcTable, ErrorMessageContainer errors) {
    this.errors = errors;
    this.funcTable = funcTable;
  }

  @Override
  public ASTNode visitProgram(@NotNull WACCParser.ProgramContext ctx) {
    /*
     * Prototyping functions: we run through the function tokens and declare
     * them with their type-signatures in the function table
     */
    WACCParser.FuncContext func;
    for (int i = 0; i < ctx.getChildCount(); i++) {
      // functions have no previous scope
      if (ctx.getChild(i) instanceof WACCParser.FuncContext) {
        // clear scope to declare function
        SymbolTable paramScope = new SymbolTable();
        SymbolTable tempTable = symbolTable;
        symbolTable = paramScope;

        func = (WACCParser.FuncContext) ctx.getChild(i);

        visit(func.getChild(0));

        for (int j = (func.getChildCount() - 3); j > 0; j--) {
          visit(func.getChild(j));
        }

        // adding parameter objects to the param list
        ArrayList<Variable> params = new ArrayList<>();

        if (func.param_list() != null) {
          int argCount = ((func.param_list().getChildCount() + 1) / 2);
          for (int k = 0; k < argCount; k++) {
            params.add(0, new Variable(typeStack.pop()));
          }
        }

        // return type is left on stack
        WACCType ret = typeStack.pop();
        Function funcDec = new Function((func.func_ident().getText()), ret,
            params, new SymbolTable(symbolTable));

        try {
          funcTable.declare(funcDec, func.func_ident().getText());
        } catch (IdentifierDeclaredPreviouslyInCurrentScopeException e) {
          errors.add(func, "Function % is declared more than once", func
              .func_ident().getText());
        }

        symbolTable = tempTable;
      }
    }

    // now functions are prototyped you can add to them

    for (int i = 0; i < (ctx.getChildCount() - 4); i++) {
      symbolTable = new SymbolTable();
      for (int j = (ctx.func(i).getChildCount() - 3); j > 0; j--) {
        visit(ctx.func(i).getChild(j));
      }

      // adding parameter objects to the param list
      List<WACCType> paramTypes = new ArrayList<>();

      if (ctx.func(i).param_list() != null) {
        int argCount = ((ctx.func(i).param_list().getChildCount() + 1) / 2);
        for (int k = 0; k < argCount; k++) {
          paramTypes.add(0, typeStack.pop());
        }
      }

      try {
        currentFunction = funcTable.lookupFunction(ctx.func(i).func_ident()
            .getText(), paramTypes);
      } catch (IdentifierUndeclaredException e) {
        // indicates compiler error
        e.printStackTrace();
      }

      currentFunction.setBody((StatNode) visit(ctx.func(i)));

    }

    symbolTable = new SymbolTable();

    // ctx.size()-2 is the location of the main stat body.
    inMain = true;
    functionHasReturn = false;

    Function main = new Function("main", TYPE_INT, new ArrayList<Variable>(),
        symbolTable);
    StatNode mainBody = (StatNode) visit(ctx.stat());
    if (!functionHasReturn) {
      SequenceNode seq;
      if (!(mainBody instanceof SequenceNode)) {
        seq = new SequenceNode(mainBody);
      } else {
        seq = (SequenceNode) mainBody;
      }
      seq.addChild(new ReturnNode(new IntLiteralNode(0)));
      mainBody = seq;
    }
    main.setBody(mainBody);
    funcTable.setMain(main);

    while (!typeStack.isEmpty()) {
      System.out.println(typeStack.pop());
    }

    return null;
  }

  @Override
  public StatNode visitStat(@NotNull WACCParser.StatContext ctx) {
    /*
     * BEGIN/IF/WHILE: cases where a new scope is created
     */
    if (ctx.BEGIN() != null) {
      SymbolTable newScope = new SymbolTable(symbolTable);
      symbolTable = newScope;
      StatNode scopeNode = new ScopeNode((StatNode) visit(ctx.stat(0)),
          symbolTable);

      // remove
      // visitChildren(ctx)
      symbolTable = newScope.getPreviousTable();
      return scopeNode;
    }

    /*
     * IF/WHILE: statements inside of a function that is not main also need to
     * consider whether or not they need a return value.
     */
    if (ctx.IF() != null || ctx.WHILE() != null) {
      // Check that the conditional expression evaluates to a boolean
      /*

         */
      ExprNode conditional = (ExprNode) visit(ctx.expr());
      WACCType type = typeStack.pop();
      if (!type.equals(TYPE_BOOL)) {
        errors.add(ctx, "Expected 'bool' in % conditional, got %",
            ctx.IF() != null ? "if" : "while", type);
      }
      SymbolTable newScope = new SymbolTable(symbolTable);
      symbolTable = newScope;

      // Visit while's body or if statement's first body

      ScopeNode trueBody = new ScopeNode((StatNode) visit(ctx.stat(0)),
          symbolTable);

      symbolTable = newScope.getPreviousTable();
      if (ctx.WHILE() != null) {
        inWhileLoop = true;
        WhileNode node = new WhileNode(conditional, trueBody);

        topWhileIndex = Math.max(node.getIndex(), topWhileIndex);

        return node;
      }

      if (ctx.IF() != null) {
        boolean thenHasReturn = functionHasReturn;
        newScope = new SymbolTable(symbolTable);
        symbolTable = newScope;

        // Visit if statement's second body

        functionHasReturn = false;
        ScopeNode falseBody = new ScopeNode((StatNode) visit(ctx.stat(1)),
            symbolTable);

        functionHasReturn = (functionHasReturn && thenHasReturn);
        symbolTable = newScope.getPreviousTable();
        return new IfNode(conditional, trueBody, falseBody);
      }
    }

    // look over the comments in this if statement for ast construction
    if (ctx.SEMICOLON() != null) {
      StatNode lhs = (StatNode) visit(ctx.getChild(0));
      StatNode rhs = null;
      SequenceNode ret;

      boolean tmp = isExiting;
      isExiting = false;
      if (ctx.stat(0).SEMICOLON() != null) {
        ret = (SequenceNode) lhs;
      } else {
        ret = new SequenceNode(lhs);
        ASTNode.setIndexCounter(ASTNode.getIndexCounter() - 1);
      }
      isExiting = tmp;

      // Cannot have a statement after return
      if ((((WACCParser.StatContext) ctx.getChild(0)).RETURN() != null || functionHasReturn)
          && !isExiting) {
        errors.add((WACCParser.StatContext) ctx.getChild(2),
            "Additional statement after return");

      } else {
        rhs = (StatNode) visit(ctx.getChild(2));
      }

      if (rhs != null) {
        ret.addChild(rhs);
      }

      return ret;

    }

    if (ctx.SKIP() != null) {
      return new SkipNode();
    }

    /*
     * Must check the types of both sides of a variable declaration or
     * assignment
     */
    if (ctx.type() != null
        || (ctx.assign_lhs() != null && ctx.assign_rhs() != null)) {

      // declaration and assignment are handled at the same time, but we still
      // need to visit type in order to type-check.
      if (ctx.type() != null) {
        visit(ctx.type());
      }

      // visits the two sides of the assignment to do type-checking and creates
      // representative ExprNodes
      AssignmentNode ret = new AssignmentNode((ExprNode) visit(ctx.getChild(ctx
          .getChildCount() - 3)), (ExprNode) visit(ctx.getChild(ctx
          .getChildCount() - 1)));

      WACCType rhsType = typeStack.pop();
      WACCType lhsType = typeStack.pop();

      if (!lhsType.equals(rhsType)) {
        errors.add(ctx, "Unable to assign % to %", rhsType, lhsType);
      }
      return ret;
    }

    // ctx.getChild(ctx.getChildCount() - 1 = either expr or assign-lhs
    ExprNode expr = (ExprNode) visit(ctx.getChild(ctx.getChildCount() - 1));

    /*
     * DECLARATION/ASSIGNMENT: Must check that a return expression has the type
     * declared by its function signature
     */
    // ReturnNode
    if (ctx.RETURN() != null) {
      WACCType ret = typeStack.pop();
      if (!inMain) {
        if (!ret.equals(currentFunction.getReturnType())) {
          errors.add(ctx, "Function expected return type % but got %",
              currentFunction.getReturnType(), ret);
        }
        functionHasReturn = true;
      } else {
        errors.add(ctx, "Returning outside of a function");
      }

      return new ReturnNode(expr);
    }

    /*
     * READ/FREE/PRINT/EXIT: validity of statements depend on type of stackhead
     */
    WACCType type = typeStack.pop();

    if (ctx.READ() != null) {
      if (!(type.equals(TYPE_INT) || type.equals(TYPE_CHAR))) {
        errors.add(ctx, "Can only read from stdin to 'int' or 'char', not %",
            type);
      }
      return new ReadNode(expr);
    } else if (ctx.FREE() != null) {
      if (!(type.equals(TYPE_PAIR_ANY) || type.equals(TYPE_ARRAY_ANY))) {
        errors.add(ctx, "Can only free a 'pair' or an 'array', not %", type);
      }

      return new FreeNode(expr);
    } else if (ctx.PRINT() != null || ctx.PRINTLN() != null) {
      return new PrintNode(expr, ctx.PRINTLN() != null);
      // PRINT can take any type
    } else if (ctx.EXIT() != null) {
      functionHasReturn = true;
      isExiting = true;
      if (!type.equals(TYPE_INT)) {
        errors.add(ctx, "Expected exit type 'int', not %", type);
      }
      return new ExitNode(expr);
    }
    return null;
  }

  /*
   * Binary operators require type-checking of both lhs and rhs arguments they
   * push their return type in accordance with the contract of typeStack
   * 
   * Subject to typeStack contract
   */
  private ExprNode binOpHelper(ParserRuleContext ctx, int precedence) {

    ExprNode lhsnode = (ExprNode) visit(ctx.getChild(0));
    ExprNode rhsnode = (ExprNode) visit(ctx.getChild(2));

    String binOp = ctx.getChild(1).getText();

    WACCType expectedArgType = null;

    WACCType rhs = typeStack.pop();
    WACCType lhs = typeStack.pop();
    WACCType returnType = null;

    switch (precedence) {

    // '*', '/', '%'
      case 1:
        // '+', '-'
      case 2:
        expectedArgType = TYPE_INT;
        returnType = TYPE_INT;
        break;

      // '>', '>=', '<', '<='
      case 3:
        expectedArgType = TYPE_INT_OR_CHAR;
        returnType = TYPE_BOOL;
        break;

      // '==' '!='
      case 4:
        expectedArgType = TYPE_ANY;
        returnType = TYPE_BOOL;
        break;

      // '&&'
      case 5:
        // '||'
      case 6:
        expectedArgType = TYPE_BOOL;
        returnType = TYPE_BOOL;
        break;
    }
    boolean typesAreCorrect = true;
    if (!expectedArgType.equals(lhs)) {
      if (rhs.equals(expectedArgType)) {
        expectedArgType = rhs;
      }
      errors.add(ctx, "Expected left argument of % to be % but got %", binOp,
          expectedArgType, lhs);
      typesAreCorrect = false;
    }
    if (!expectedArgType.equals(rhs)) {
      if (lhs.equals(expectedArgType)) {
        expectedArgType = lhs;
      }
      errors.add(ctx, "Expected right argument of % to be % but got %", binOp,
          expectedArgType, rhs);
      typesAreCorrect = false;
    }
    if (expectedArgType.equals(lhs) && expectedArgType.equals(rhs)
        && !(lhs.equals(rhs))) {
      errors.add(ctx, "Type mismatch in %", binOp);
      typesAreCorrect = false;
    }
    typeStack.push(returnType);

    if (typesAreCorrect && lhsnode instanceof ImmediateReplacable
        && rhsnode instanceof ImmediateReplacable) {

      /*
       * We know both operands are immediates and so we can evaluate the result
       * here, at compile time
       * 
       * We apologise profusely
       */

      ImmediateReplacable lhsImm = (ImmediateReplacable) lhsnode;
      ImmediateReplacable rhsImm = (ImmediateReplacable) rhsnode;

      switch (binOp) {
        case "%":
          return new IntLiteralNode(lhsImm.getIntValue() % rhsImm.getIntValue());
        case "/":
          if (rhsImm.getIntValue() == 0) {
            errors.add(ctx, "Tried to divide by zero");
            errors.setErrorType(ErrorCode.RUNTIME_ERR);

          } else
            return new IntLiteralNode(lhsImm.getIntValue()
                / rhsImm.getIntValue());
        case "*":
          return new IntLiteralNode(lhsImm.getIntValue() * rhsImm.getIntValue());
        case "+":
          return new IntLiteralNode(lhsImm.getIntValue() + rhsImm.getIntValue());
        case "-":
          return new IntLiteralNode(lhsImm.getIntValue() - rhsImm.getIntValue());
        case ">":
          return new BoolLiteralNode(lhsImm.getIntValue() > rhsImm
              .getIntValue());
        case ">=":
          return new BoolLiteralNode(lhsImm.getIntValue() >= rhsImm
              .getIntValue());
        case "<":
          return new BoolLiteralNode(lhsImm.getIntValue() < rhsImm
              .getIntValue());
        case "<=":
          return new BoolLiteralNode(lhsImm.getIntValue() <= rhsImm
              .getIntValue());
        case "==":
          return new BoolLiteralNode(lhsImm.getIntValue() == rhsImm
              .getIntValue());
        case "!=":
          return new BoolLiteralNode(lhsImm.getIntValue() != rhsImm
              .getIntValue());
        case "&&":
          return new BoolLiteralNode(lhsImm.getIntValue() == 1
              && rhsImm.getIntValue() == 1);
        case "||":
          return new BoolLiteralNode(lhsImm.getIntValue() == 1
              || rhsImm.getIntValue() == 1);

        default:
          break;
      }
    }

    return new BinaryOpNode(binOp, lhsnode, rhsnode, returnType);
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitExpr(@NotNull WACCParser.ExprContext ctx) {
    ExprNode ret;
    if (ctx.bin_op_pr_1() != null)
      ret = binOpHelper(ctx, 1);
    else if (ctx.bin_op_pr_2() != null)
      ret = binOpHelper(ctx, 2);
    else if (ctx.bin_op_pr_3() != null)
      ret = binOpHelper(ctx, 3);
    else if (ctx.bin_op_pr_4() != null)
      ret = binOpHelper(ctx, 4);
    else if (ctx.bin_op_pr_5() != null)
      ret = binOpHelper(ctx, 5);
    else if (ctx.bin_op_pr_6() != null)
      ret = binOpHelper(ctx, 6);

    else if (ctx.unary_op() != null) {
      // type checking
      visit(ctx.unary_op());
      // we know that there's only one expr
      WACCType expected = typeStack.pop();

      ret = new UnaryOpNode(ctx.unary_op().getText(), (ExprNode) visit(ctx
          .expr(0)));

      WACCType argumentType = typeStack.pop();

      if (!argumentType.equals(expected)) {
        errors.add(ctx, "Invalid argument of %, expected % got %", ctx
            .unary_op().getText(), expected, argumentType);
      }
    } else if (ctx.OPEN_PARENTHESIS() != null) {
      ret = (ExprNode) visit(ctx.expr(0));
    } else {
      // literal expressions
      ret = (ExprNode) visitChildren(ctx);
    }
    return ret;
  }

  @Override
  public ASTNode visitFunc(@NotNull WACCParser.FuncContext ctx) {
    inMain = false;

    // Functions have their own scope

    SymbolTable globalScope = symbolTable;

    symbolTable = currentFunction.getFuncScope();

    // removed the need to look at function paramaters
    // as we do this whilst prototyping.

    // building root node for function stat body.
    StatNode body = (StatNode) visit(ctx.getChild(ctx.getChildCount() - 2));

    // Exited function and checking if a return has occurred.
    inMain = true;
    if (!functionHasReturn) {
      errors.add(ctx, "Function % is missing a return statement", ctx
          .func_ident().getText());
      errors.setErrorType(ErrorCode.SYNTACTIC_ERR);
    }
    functionHasReturn = false;
    symbolTable = globalScope; // all functions are in global scope
                               // but can't see the global scope.
    return body;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitAssign_rhs(@NotNull WACCParser.Assign_rhsContext ctx) {
    ExprNode ret = null;

    if (ctx.NEWPAIR() != null) {
      ExprNode lhs = (ExprNode) visit(ctx.expr(0));
      ExprNode rhs = (ExprNode) visit(ctx.expr(1));

      WACCType right = typeStack.pop();
      WACCType left = typeStack.pop();

      WACCType exprType = new PairType(left, right);
      typeStack.push(exprType);
      ret = new NewPairNode(lhs, rhs, exprType);
    } else if (ctx.CALL() != null) {
      try {

        // handling the parameters
        ArrayList<ExprNode> params = new ArrayList<>();
        List<WACCType> paramTypes = new ArrayList<>();
        if (ctx.arg_list() != null) {
          for (WACCParser.ExprContext expr : ctx.arg_list().expr()) {
            params.add((ExprNode) visit(expr));
            paramTypes.add(typeStack.peek());
          }
        }

        Function function = funcTable.lookupFunction(
            ctx.func_ident().getText(), paramTypes);
        WACCType stackHead;

        // Check that the function call has right number of arguments
        int currentArgumentCount = (ctx.getChild(3).getChildCount() + 1) / 2;
        boolean tooFewArgs = false;
        boolean tooManyArgs = false;

        // Too many arguments
        if (function.getArgumentCount() < (currentArgumentCount)) {
          tooManyArgs = true;
          errors.add(ctx,
              "Function % has too many arguments, expected % got %", ctx
                  .func_ident().getText(), function.getArgumentCount(),
              currentArgumentCount);
        }

        // Too few arguments
        if (function.getArgumentCount() > (currentArgumentCount)) {
          tooFewArgs = true;
          errors.add(ctx, "Function % has too few arguments, expected % got %",
              ctx.func_ident().getText(), function.getArgumentCount(),
              currentArgumentCount);
        }

        // Pop extra types off
        if (tooManyArgs) {
          for (int i = currentArgumentCount - 1; i >= function
              .getArgumentCount(); i--) {
            typeStack.pop();
          }
        }

        /*
         * Iterate over arguments backwards to check against stack pop off the
         * right number of arguments
         */
        for (int i = (function.getArgumentCount() - 1); i >= 0; i--) {
          if (tooFewArgs && (i >= currentArgumentCount)) {
            stackHead = function.getArgumentType(i);
          } else {
            stackHead = typeStack.pop();
          }

          if (!(stackHead.equals(function.getArgumentType(i)))) {
            errors.add(ctx,
                "Function % argument number % expected type % got %", ctx
                    .func_ident().getText(), i + 1,
                function.getArgumentType(i), stackHead);
          }
        }

        WACCType returnType = function.getReturnType();
        typeStack.push(returnType);
        ret = new CallNode(function, params, returnType);

      } catch (IdentifierUndeclaredException e) {
        errors.add(ctx, "Function % was not declared", e.getId());
        typeStack.push(TYPE_ANY);
      }

    } else {
      ret = (ExprNode) visitChildren(ctx);
    }
    return ret;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitAssign_lhs(@NotNull WACCParser.Assign_lhsContext ctx) {
    return visitChildren(ctx);
  }

  /*
   * Unary operators check their type before pushing their return type in
   * accordance with the contract of typeStack
   */
  // Subject to typeStack contract
  @Override
  public ASTNode visitUnary_op(@NotNull WACCParser.Unary_opContext ctx) {
    WACCType expectedArgumentType = TYPE_ANY;
    WACCType returnType = TYPE_ANY;
    String unaryOp = ctx.getText();

    switch (unaryOp) {
      case "!":
        expectedArgumentType = returnType = TYPE_BOOL;
        break;
      case "len":
        expectedArgumentType = TYPE_ARRAY_ANY;
        returnType = TYPE_INT;
        break;
      case "ord":
        expectedArgumentType = TYPE_CHAR;
        returnType = TYPE_INT;
        break;
      case "-":
        expectedArgumentType = returnType = TYPE_INT;
        break;
      case "chr":
        expectedArgumentType = TYPE_INT;
        returnType = TYPE_CHAR;
        break;
      default:
        // Shouldn't be able to get here because syntax checker should catch
        // the case of an invalid unary operation.
        errors.add(ctx, "% is an invalid unary operator", unaryOp);
    }

    typeStack.push(returnType);

    // Push expectedArgumentType to get further relevant errors
    typeStack.push(expectedArgumentType);

    return null;
  }

  private boolean inWhileLoop;
  private int topWhileIndex;

  @Override
  public IdentNode visitDefine_ident(@NotNull WACCParser.Define_identContext ctx) {
    WACCType varType = typeStack.peek();
    Variable newVariable = new Variable(varType);
    try {
      symbolTable.declare(newVariable, ctx.IDENT().getText());
    } catch (IdentifierDeclaredPreviouslyInCurrentScopeException e) {
      errors.add(ctx,
          "Identifier % was previously declared in the current scope", ctx
              .IDENT().getText());
    }

    IdentNode node = new IdentNode(ctx.IDENT().getText(), newVariable, varType);
    return node;
  }

  // Subject to typeStack contract
  @Override
  public IdentNode visitIdent(@NotNull WACCParser.IdentContext ctx) {
    Variable variable = null;
    WACCType varType;
    try {
      String identName = ctx.IDENT().getText();
      variable = symbolTable.lookupAll(identName);
      varType = variable.getType();
    } catch (IdentifierUndeclaredException e) {
      errors.add(ctx, "Identifier % was not declared in the current scope", ctx
          .IDENT().getText());
      varType = TYPE_ANY;
    }
    typeStack.push(varType);

    IdentNode node = new IdentNode(ctx.IDENT().getText(), variable, varType);
    return node;
  }

  /*
   * Type visitation methods are delegated to static functions in the respective
   * class
   */

  // Subject to typeStack contract
  @Override
  public ASTNode visitBase_type(@NotNull WACCParser.Base_typeContext ctx) {

    if (ctx.INT() != null) {
      IntType.visitType(ctx, errors, typeStack);
    } else if (ctx.BOOL() != null) {
      BoolType.visitType(ctx, errors, typeStack);
    } else if (ctx.CHAR() != null) {
      CharType.visitType(ctx, errors, typeStack);
    } else if (ctx.STRING() != null) {
      StringType.visitType(ctx, errors, typeStack);
    }

    return null;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitArray_type(@NotNull WACCParser.Array_typeContext ctx) {
    visitChildren(ctx);
    ArrayType.visitType(ctx, errors, typeStack);
    return null;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitPair_type(@NotNull WACCParser.Pair_typeContext ctx) {
    visitChildren(ctx);
    PairType.visitType(ctx, errors, typeStack);
    return null;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitPair_elem(@NotNull WACCParser.Pair_elemContext ctx) {
    return new PairElemNode((ExprNode) visit(ctx.expr()), PairType.visitElem(
        ctx, errors, typeStack), typeStack.peek());
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitPair_elem_type(
      @NotNull WACCParser.Pair_elem_typeContext ctx) {
    visitChildren(ctx);
    PairType.visitElemType(ctx, errors, typeStack);
    return null;
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitPair_literal(@NotNull WACCParser.Pair_literalContext ctx) {
    visitChildren(ctx);
    PairType.visitLiteral(ctx, errors, typeStack);
    return new NullNode(TYPE_PAIR_ANY);
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitArray_liter(@NotNull WACCParser.Array_literContext ctx) {
    ArrayList<ExprNode> elements = new ArrayList<>();
    boolean pureString = true;
    for (WACCParser.ExprContext expr : ctx.expr()) {
      ExprNode elem = (ExprNode) visit(expr);
      elements.add(elem);
      if (pureString && !(elem instanceof CharLiteralNode))
        pureString = false;
    }
    ArrayType.visitLiteral(ctx, errors, typeStack);

    if (pureString) {
      return ArrayLiteralNode.constructPureString(elements);
    }

    return new ArrayLiteralNode(elements, typeStack.peek());
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitArray_elem(@NotNull WACCParser.Array_elemContext ctx) {
    // visitChildren(ctx);
    IdentNode identNode = (IdentNode) visit(ctx.ident());
    List<ExprNode> indices = new ArrayList<>();
    for (WACCParser.ExprContext expr : ctx.expr()) {
      indices.add((ExprNode) (visit(expr)));
    }
    ArrayType.visitElem(ctx, errors, typeStack);
    return new ArrayElemNode(identNode, indices, typeStack.peek());
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitChar_literal(@NotNull WACCParser.Char_literalContext ctx) {
    CharType.visitLiteral(ctx, errors, typeStack);

    String literalCharAsString = ctx.CHAR_LITERAL().getText();

    char literalChar = literalCharAsString.charAt(1);

    if (literalChar == '\\') {
      literalChar = literalCharAsString.charAt(2);
    }

    return new CharLiteralNode(literalChar, TYPE_CHAR);
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitString_literal(
      @NotNull WACCParser.String_literalContext ctx) {
    List<ExprNode> charArray = new ArrayList<>();

    char[] string = ctx.STRING_LITERAL().getText().toCharArray();
    for (int i = 1; i < (string.length - 1); i++) {
      charArray.add(new CharLiteralNode(string[i], TYPE_CHAR));
    }

    StringType.visitLiteral(ctx, errors, typeStack);
    return ArrayLiteralNode.constructPureString(charArray);
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitBool_literal(@NotNull WACCParser.Bool_literalContext ctx) {
    BoolType.visitLiteral(ctx, errors, typeStack);
    return new BoolLiteralNode(ctx.TRUE() != null);
  }

  // Subject to typeStack contract
  @Override
  public ASTNode visitInt_literal(@NotNull WACCParser.Int_literalContext ctx) {
    return IntType.visitLiteral(ctx, errors, typeStack);
  }
}