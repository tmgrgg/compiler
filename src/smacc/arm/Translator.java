package smacc.arm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import smacc.Function;
import smacc.SymbolTable;
import smacc.arm.PredefinedFunctionHandler.PredefinedFunction;
import smacc.arm.ARMAddNode.AddComparator;
import smacc.arm.ARMBranchNode.BranchComparator;
import smacc.arm.ARMLogicalNode.LogicalComparator;
import smacc.arm.ARMMovNode.MovComparator;
import smacc.arm.ARMMulNode.MulComparator;
import smacc.arm.ARMLdrNode.LdrComparator;
import smacc.arm.ARMStrNode.StrComparator;
import smacc.arm.ARMSubNode.SubComparator;
import smacc.arm.OperandTwo.ShiftType;
import smacc.arm.Register.SpecialReg;
import smacc.ast.stat.*;
import smacc.ast.expr.*;
import smacc.types.WACCType;

/*
 * Translator class
 * 
 * Given a function table builds a list of ARM nodes
 */
public class Translator {

  private ARMFileStart fileStart;
  private PredefinedFunctionHandler predefinedFunctionHandler;

  private SymbolTable currentScope;

  /*
   * The returnReg contract The expression currently being evaluated will be
   * returned in the current returnReg.
   */
  private Register returnReg = new Register(4);

  private static Register Reg10 = new Register(SpecialReg.r10);
  private static Register Reg11 = new Register(SpecialReg.r11);

  private void incReturnReg() {
    if (returnReg.getRegisterNumber() == 10) {
      program.add(new ARMPushNode(Reg10));
      stackCount++;
    } else {
      returnReg = returnReg.getNextReg();
    }
  }

  private void decReturnReg() {
    if (stackCount == 0) {
      returnReg = returnReg.getPrevReg();
    }
  }

  /*
   * Store and load comparators change if storing a char
   * Functions handling the checking of these cases
   */
  private StrComparator chooseStrComparator(ExprNode node) {
    return (node.getType().equals(WACCType.TYPE_CHAR) || node.getType().equals(
        WACCType.TYPE_BOOL)) ? StrComparator.STRB : StrComparator.STR;
  }

  private LdrComparator chooseLdrComparator(ExprNode node) {
    return (node.getType().equals(WACCType.TYPE_CHAR) || node.getType().equals(
        WACCType.TYPE_BOOL)) ? LdrComparator.LDRSB : LdrComparator.LDR;
  }

  /*
   * When adding or subtracting from the stack pointer we cannot add more than 1024
   * in one operation
   * Separate into valid sizes handles the construction of multiple OperandTwos to be
   * added or subtracted
   */
  private LinkedList<OperandTwo> separateIntoValidSizes(int operandTwo) {
    LinkedList<OperandTwo> splitValues = new LinkedList<>();
    while (operandTwo > 1024) {
      operandTwo -= 1024;
      splitValues.add(new OperandTwo(1024, false));
    }
    splitValues.add(new OperandTwo(operandTwo, false));
    return splitValues;
  }

  int stackCount = 0;
  // Set in translateIdentNode
  private int lastIdentOffset = 0;
  private boolean translatingAssignLhs = false;
  private boolean translatingPrintOrFree = false;
  private boolean translatingRead = false;
  private boolean translatingArrayElemIndex = false;
  // Set to true in translateReturnNode to clear stack before pop.
  private boolean isReturning = false;
  Function currentFunction;

  private List<ARMNode> program;

  public Translator() {
    program = new ArrayList<>();
    fileStart = new ARMFileStart();
    this.predefinedFunctionHandler = new PredefinedFunctionHandler(fileStart,
        program);
    program.add(fileStart);
  }

  public List<ARMNode> getARM() {
    ARMFileEnd fileEnd = new ARMFileEnd(predefinedFunctionHandler);
    program.add(fileEnd);
    return program;
  }

  // Translating Functions

  public void translateFunction(Function function) {
    program.add(new ARMLabel(function.getId()));
    program.add(new ARMFunctionStart());
    currentFunction = function;
    function.getBody().translate(this);
    program.add(new ARMFunctionEnd());
  }

  // Translating Stats

  public void translateCallNode(CallNode node) {
    int argCount = node.getArgCount();
    int totalArgByteSize = 0;

    // For each param in reverse order
    for (int i = 0; i < argCount; i++) {

      ExprNode currentArg = node.getArg(i);

      // Load into returnReg
      currentArg.translate(this);

      StrComparator comparator = chooseStrComparator(currentArg);
      int currentArgSize = currentArg.getSizeInBytes();

      program.add(new ARMStrNode(returnReg, new Register(SpecialReg.sp),
          -currentArgSize, true, comparator));
      totalArgByteSize += currentArgSize;
      currentScope.changeSPOffset(currentArgSize);
    }
    program.add(new ARMBranchNode(node.getFunction().getId(),
        BranchComparator.BL));
    currentScope.setSPOffset(0);
    if (argCount > 0) {
      program.add(new ARMAddNode(new Register(SpecialReg.sp), new Register(
          SpecialReg.sp), new OperandTwo(totalArgByteSize, false),
          AddComparator.ADD));
    }
    program.add(new ARMMovNode(returnReg, new OperandTwo(new Register(
        SpecialReg.r0)), MovComparator.MOV));

  }

  public void translateReturnNode(ReturnNode node) {
    node.getExpr().translate(this);

    // Creates a register with zero in the int field
    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));

    // Don't pop here, pop at end of translateScopeNode
    isReturning = true;
    int numElemsInScope = currentScope.getSizeOfAllInBytes()
        - currentFunction.totalParameterSizeInBytes();
    if (numElemsInScope != 0) {
      LinkedList<OperandTwo> operands = separateIntoValidSizes(numElemsInScope);
      for (OperandTwo operandTwo : operands) {
        program.add(new ARMAddNode(new Register(Register.SpecialReg.sp),
            new Register(Register.SpecialReg.sp), operandTwo,
            ARMAddNode.AddComparator.ADD));
      }
    }
  }

  public void translateIfNode(IfNode node) {
    node.getConditional().translate(this);
    ARMLabel label = new ARMLabel(false);
    ARMLabel label2 = new ARMLabel(false);
    // Compare result of conditional with 0
    program.add(new ARMCmpNode(returnReg, new OperandTwo(0, false)));
    // Jump to false body if false
    program.add(new ARMBranchNode(label.getLabel(), BranchComparator.BEQ));
    node.getTrueBody().translate(this);
    // Avoid executing false body after true body is executed
    program.add(new ARMBranchNode(label2.getLabel(), BranchComparator.B));
    // Add false label
    program.add(label);
    node.getFalseBody().translate(this);
    // Add skip label
    program.add(label2);
  }

  public void translateAssignmentNode(AssignmentNode node) {
    node.getRHS().translate(this);

    incReturnReg();
    // Will do nothing for translateIdentNode
    translatingAssignLhs = true;
    node.getLHS().translate(this);
    translatingAssignLhs = false;
    decReturnReg();

    StrComparator comparator = chooseStrComparator(node.getLHS());

    if (node.getLHS() instanceof ArrayElemNode
        || node.getLHS() instanceof PairElemNode) {
      program.add(new ARMStrNode(returnReg, returnReg.getNextReg(), 0, false,
          comparator));
    } else {
      program.add(new ARMStrNode(returnReg, new Register(SpecialReg.sp),
          lastIdentOffset, false, comparator));
    }
  }

  public void translatePrintNode(PrintNode node) {
    translatingPrintOrFree = true;
    node.getExpr().translate(this);

    // Move expression value into R0 for printing
    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));

    if (node.getExpr().getType().equals(WACCType.TYPE_BOOL)) {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_PRINT_BOOL);
    } else if (node.getExpr().getType().equals(WACCType.TYPE_STRING)) {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_PRINT_STRING);
    } else if (node.getExpr().getType().equals(WACCType.TYPE_INT)) {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_PRINT_INT);
    } else if (node.getExpr().getType().equals(WACCType.TYPE_CHAR)) {
      program.add(new ARMBranchNode("putchar", BranchComparator.BL));
    } else {
      predefinedFunctionHandler
          .addfunction(PredefinedFunction.P_PRINT_REFERENCE);
    }

    if (node.getIsPrintln()) {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_PRINTLN);
    }

    translatingPrintOrFree = false;
  }

  // only looks at ints or chars
  public void translateReadNode(ReadNode node) {
    translatingRead = true;
    node.getExpr().translate(this);

    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));

    if (node.getExpr().getType().equals(WACCType.TYPE_CHAR)) {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_READ_CHAR);
    } else {
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_READ_INT);
    }
    translatingRead = false;
  }

  public void translateFreeNode(FreeNode node) {
    translatingPrintOrFree = true;
    node.getExpr().translate(this);
    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));
    if (node.getExpr().getType().equals(WACCType.TYPE_ARRAY_ANY))
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_FREE_ARRAY);
    else
      predefinedFunctionHandler.addfunction(PredefinedFunction.P_FREE_PAIR);

    translatingPrintOrFree = false;
  }

  public void translateSequenceNode(SequenceNode node) {
    // Translates sequenced stats in order
    for (int i = 0; i < node.getStatCount(); i++) {
      node.getStat(i).translate(this);
    }
  }

  public void translateSkipNode(SkipNode node) {
    // SkipNodes are not represented in ARM assembly
  }

  public void translateWhileNode(WhileNode node) {
    ARMLabel label = new ARMLabel(false);
    ARMLabel label2 = new ARMLabel(false);

    // Branch to label condition check
    program.add(new ARMBranchNode(label.getLabel(), BranchComparator.B));
    // Add label2 first, with loop body code in it, which falls through into
    // condition check label without a branch
    program.add(label2);
    node.getBody().translate(this);
    // Add label with condition check followed by rest of code after loop
    program.add(label);
    node.getConditional().translate(this);
    // If conditional evaluates to true, branch back up to loop body
    program.add(new ARMCmpNode(returnReg, new OperandTwo(1, false)));
    program.add(new ARMBranchNode(label2.getLabel(), BranchComparator.BEQ));
  }

  public void translateExitNode(ExitNode node) {
    node.getExpr().translate(this);
    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));
    program.add(new ARMBranchNode("exit", BranchComparator.BL));
  }

  /*
   * Gets the number of items in a symboltable for the specific scope and makes
   * space for it on the stack using a stack pointer register
   */
  public void translateScopeNode(ScopeNode node) {

    SymbolTable tempTable = currentScope;
    currentScope = node.getSymbolTable();

    int numElemsInScope = currentScope.getSizeOfTableInBytes();
    // Make new immediate operand 2
    LinkedList<OperandTwo> operands = separateIntoValidSizes(numElemsInScope);

    // Subtract from stack if there are local variables
    if (numElemsInScope != 0) {
      for (OperandTwo operandTwo : operands) {
        program.add(new ARMSubNode(new Register(Register.SpecialReg.sp),
            new Register(Register.SpecialReg.sp), operandTwo,
            ARMSubNode.SubComparator.SUB));
      }
    }

    // Translate scope body
    node.getStat().translate(this);

    // Add to stack if you subtracted
    if (!isReturning && numElemsInScope != 0) {
      for (OperandTwo operandTwo : operands) {
        program.add(new ARMAddNode(new Register(Register.SpecialReg.sp),
            new Register(Register.SpecialReg.sp), operandTwo,
            ARMAddNode.AddComparator.ADD));
      }
    }

    currentScope = tempTable;
    if (isReturning) {
      program.add(new ARMPopNode(new Register(SpecialReg.pc)));
    }
    isReturning = false;
  }

  // Translating Exprs

  public void translateArrayElemNode(ArrayElemNode node) {
    // Sets sp offset to array ident and loads into returnReg if
    node.getIdent().translate(this);
    // ADD returnReg, sp, offsetToArrayFromSp
    // returnReg will hold pointer to position of array pointer on stack
    program.add(new ARMAddNode(returnReg, new Register(SpecialReg.sp),
        new OperandTwo(lastIdentOffset, false), AddComparator.ADD));
    for (int i = 0; i < node.getIndexCount(); i++) {
      translatingArrayElemIndex = true;
      incReturnReg();
      node.getIndex(i).translate(this);
      decReturnReg();
      translatingArrayElemIndex = false;

      program.add(new ARMLdrNode(returnReg, returnReg, 0, LdrComparator.LDR));
      program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
          returnReg.getNextReg()), MovComparator.MOV));
      program.add(new ARMMovNode(new Register(SpecialReg.r1), new OperandTwo(
          returnReg), MovComparator.MOV));
      predefinedFunctionHandler
          .addfunction(PredefinedFunction.P_CHECK_ARRAY_BOUNDS);
      program.add(new ARMAddNode(returnReg, returnReg,
          new OperandTwo(4, false), AddComparator.ADD));
      if (node.getType().equals(WACCType.TYPE_CHAR)) {
        program.add(new ARMAddNode(returnReg, returnReg, new OperandTwo(
            returnReg.getNextReg()), AddComparator.ADD));
      } else {
        program.add(new ARMAddNode(returnReg, returnReg, new OperandTwo(
            returnReg.getNextReg(), ShiftType.LSL, 2), AddComparator.ADD));
      }
      if (!translatingAssignLhs && (i == (node.getIndexCount() - 1))) {

        program.add(new ARMLdrNode(returnReg, returnReg, 0, LdrComparator.LDR));
      }
    }
  }

  public void translateArrayLiteralNode(ArrayLiteralNode node) {
    // LDR r0, totalSizeToMalloc
    if (ArrayLiteralNode.isPureString(node)) {
      ArmMessage string = new ArmMessage(node.toString(), true);
      fileStart.addMessage(string);
      program.add(new ARMLdrNode(returnReg, fileStart.getMessageLabel(string),
          LdrComparator.LDR));
    } else {
      program.add(new ARMLdrNode(new Register(SpecialReg.r0), node
          .getSizeInBytes(), LdrComparator.LDR));
      // BL malloc (Calls C function malloc, and will use value in r0 as
      // parameter
      program.add(new ARMBranchNode("malloc", BranchComparator.BL));
      // MOV returnReg, r0 (Puts address of allocated memory
      // (from malloc) in returnReg
      program.add(new ARMMovNode(returnReg, new OperandTwo(new Register(
          SpecialReg.r0)), MovComparator.MOV));

      int offset = 0;
      // Translate each element of array, then store it in the array at the
      // correct index (using offset to store at that index)
      for (int i = 0; i < node.getLength(); i++) {
        ExprNode curr = node.getElement(i);
        incReturnReg();
        curr.translate(this);
        decReturnReg();
        StrComparator comparator = chooseStrComparator(curr);
        // First offset is always 4 because the int
        // representing the length of the array goes first
        if (offset == 0) {
          offset = 4;
        } else {
          offset += curr.getSizeInBytes();
        }
        program.add(new ARMStrNode(returnReg.getNextReg(), returnReg, offset,
            false, comparator));
      }
      // Handle putting length of array at start of array
      incReturnReg();
      new IntLiteralNode(node.getLength()).translate(this);
      decReturnReg();
      program.add(new ARMStrNode(returnReg.getNextReg(), returnReg, 0, false,
          StrComparator.STR));
    }
  }

  public void translateBinaryOpNode(BinaryOpNode node) {

    // We know that at most one argument is an immediate

    int imm = -1;
    int immVal = 0;
    boolean immIsChar = false;
    if (node.getLeft() instanceof ImmediateReplacable) {
      imm = 0;
      immVal = ((ImmediateReplacable) node.getLeft()).getIntValue();
      immIsChar = (node.getLeft().getType().equals(WACCType.TYPE_CHAR));
    }
    if (immVal > 255 || immVal <= -255 || imm == -1) {
      node.getLeft().translate(this);
    }

    if (node.getRight() instanceof ImmediateReplacable) {
      imm = 1;
      immVal = ((ImmediateReplacable) node.getRight()).getIntValue();
      immIsChar = (node.getRight().getType().equals(WACCType.TYPE_CHAR));
    }
    if (immVal > 255 || immVal <= -255 || imm <= 0) {
      incReturnReg();
      node.getRight().translate(this);
      decReturnReg();
    }

    Register dstReg;
    Register op1Reg;
    Register op2Reg;

    if (stackCount > 0) {
      program.add(new ARMPopNode(Reg11));
      stackCount--;
      dstReg = returnReg; // R10
      op1Reg = (Reg11);
      op2Reg = returnReg; // R10
    } else {
      dstReg = returnReg;
      op1Reg = returnReg;

      if (imm < 0 || immVal > 255 || immVal <= -255)
        op2Reg = returnReg.getNextReg();
      else
        op2Reg = returnReg;
    }

    if ((imm >= 0) && (immVal <= 255) && (immVal > -255)) {
      /*
       * Immediate value handling
       */
      switch (node.getOp()) {
        case DIVIDE:
          if (imm == 0) {
            program.add(new ARMMovNode(new Register(SpecialReg.r0),
                new OperandTwo(immVal, immIsChar), MovComparator.MOV));
            program.add(new ARMMovNode(new Register(SpecialReg.r1),
                new OperandTwo(op2Reg), MovComparator.MOV));
          } else {
            program.add(new ARMMovNode(new Register(SpecialReg.r0),
                new OperandTwo(op1Reg), MovComparator.MOV));
            program.add(new ARMMovNode(new Register(SpecialReg.r1),
                new OperandTwo(immVal, immIsChar), MovComparator.MOV));
          }
          predefinedFunctionHandler
              .addfunction(PredefinedFunction.P_CHECK_DIVIDE_BY_ZERO);
          program.add(new ARMBranchNode("__aeabi_idiv", BranchComparator.BL));
          program.add(new ARMMovNode(dstReg, new OperandTwo(new Register(
              SpecialReg.r0)), MovComparator.MOV));
          break;
        case EQUALS:
          // As one argument is an immediate we know that op1Reg = op2Reg
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
              MovComparator.MOVEQ));
          program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
              MovComparator.MOVNE));
          break;
        case GEQ:
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          if (imm == 0) {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVLT));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVGE));
          } else {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVGE));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVLT));
          }
          break;
        case GREATER:
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          if (imm == 0) {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVLE));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVGT));
          } else {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVGT));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVLE));
          }
          break;
        case LEQ:
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          if (imm == 0) {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVGT));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVLE));
          } else {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVLE));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVGT));
          }
          break;
        case LESS:
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          if (imm == 0) {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVGE));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVLT));
          } else {
            program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
                MovComparator.MOVLT));
            program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
                MovComparator.MOVGE));
          }
          break;
        case LOGICAL_AND:
          program.add(new ARMLogicalNode(LogicalComparator.AND, dstReg, op1Reg,
              new OperandTwo(immVal, immIsChar)));
          break;
        case LOGICAL_OR:
          program.add(new ARMLogicalNode(LogicalComparator.ORR, dstReg, op1Reg,
              new OperandTwo(immVal, immIsChar)));
          break;
        case MINUS:
          if (imm == 0) {
            program.add(new ARMSubNode(dstReg, op2Reg, new OperandTwo(immVal,
                false), SubComparator.RSBS));
          } else {
            program.add(new ARMSubNode(dstReg, op1Reg, new OperandTwo(immVal,
                false), SubComparator.SUBS));
          }
          predefinedFunctionHandler
              .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR);
          break;
        case MODULUS:
          if (imm == 0) {
            program.add(new ARMMovNode(new Register(SpecialReg.r0),
                new OperandTwo(immVal, immIsChar), MovComparator.MOV));
            program.add(new ARMMovNode(new Register(SpecialReg.r1),
                new OperandTwo(op2Reg), MovComparator.MOV));
          } else {
            program.add(new ARMMovNode(new Register(SpecialReg.r0),
                new OperandTwo(op1Reg), MovComparator.MOV));
            program.add(new ARMMovNode(new Register(SpecialReg.r1),
                new OperandTwo(immVal, immIsChar), MovComparator.MOV));
          }
          predefinedFunctionHandler
              .addfunction(PredefinedFunction.P_CHECK_DIVIDE_BY_ZERO);
          program
              .add(new ARMBranchNode("__aeabi_idivmod", BranchComparator.BL));
          program.add(new ARMMovNode(dstReg, new OperandTwo(new Register(
              SpecialReg.r1)), MovComparator.MOV));
          break;
        case MULTIPLY:
          // DOES STACK COUNT STUFF NEED TO
          if (stackCount > 0) {
            program.add(new ARMMulNode(MulComparator.SMULL, Reg10, Reg11,
                Reg11, Reg10));
            program.add(new ARMCmpNode(Reg11, new OperandTwo(Reg10,
                ShiftType.ASR, 31)));
          } else {
            program.add(new ARMLdrNode(returnReg.getNextReg(), immVal,
                LdrComparator.LDR));
            program.add(new ARMMulNode(MulComparator.SMULL, returnReg,
                returnReg.getNextReg(), returnReg, returnReg.getNextReg()));
            program.add(new ARMCmpNode(returnReg.getNextReg(), new OperandTwo(
                returnReg, ShiftType.ASR, 31)));
          }
          predefinedFunctionHandler
              .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR_NE);
          break;
        case NOTEQUALS:
          program
              .add(new ARMCmpNode(op1Reg, new OperandTwo(immVal, immIsChar)));
          program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
              MovComparator.MOVNE));
          program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
              MovComparator.MOVEQ));
          break;
        case PLUS:
          if (imm == 0) {
            program.add(new ARMAddNode(dstReg, op2Reg, new OperandTwo(immVal,
                false), AddComparator.ADDS));
          } else {
            program.add(new ARMAddNode(dstReg, op1Reg, new OperandTwo(immVal,
                false), AddComparator.ADDS));
          }
          predefinedFunctionHandler
              .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR);
          break;
        default:
          break;

      }
      return;
    }

    switch (node.getOp()) {
      case DIVIDE:
        program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
            op1Reg), MovComparator.MOV));
        program.add(new ARMMovNode(new Register(SpecialReg.r1), new OperandTwo(
            op2Reg), MovComparator.MOV));
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_CHECK_DIVIDE_BY_ZERO);
        program.add(new ARMBranchNode("__aeabi_idiv", BranchComparator.BL));
        program.add(new ARMMovNode(dstReg, new OperandTwo(new Register(
            SpecialReg.r0)), MovComparator.MOV));
        break;
      case EQUALS:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVEQ));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVNE));
        break;
      case GEQ:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVGE));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVLT));
        break;
      case GREATER:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVGT));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVLE));
        break;
      case LEQ:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVLE));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVGT));
        break;
      case LESS:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVLT));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVGE));
        break;
      case LOGICAL_AND:
        program.add(new ARMLogicalNode(LogicalComparator.AND, dstReg, op1Reg,
            new OperandTwo(op2Reg)));
        break;
      case LOGICAL_OR:
        program.add(new ARMLogicalNode(LogicalComparator.ORR, dstReg, op1Reg,
            new OperandTwo(op2Reg)));
        break;
      case MINUS:
        program.add(new ARMSubNode(dstReg, op1Reg, new OperandTwo(op2Reg),
            SubComparator.SUBS));
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR);
        break;
      case MODULUS:
        program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
            op1Reg), MovComparator.MOV));
        program.add(new ARMMovNode(new Register(SpecialReg.r1), new OperandTwo(
            op2Reg), MovComparator.MOV));
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_CHECK_DIVIDE_BY_ZERO);
        program.add(new ARMBranchNode("__aeabi_idivmod", BranchComparator.BL));
        program.add(new ARMMovNode(dstReg, new OperandTwo(new Register(
            SpecialReg.r1)), MovComparator.MOV));
        break;
      case MULTIPLY:
        if (stackCount > 0) {
          program.add(new ARMMulNode(MulComparator.SMULL, Reg10, Reg11, Reg11,
              Reg10));
          program.add(new ARMCmpNode(Reg11, new OperandTwo(Reg10,
              ShiftType.ASR, 31)));
        } else {
          program.add(new ARMMulNode(MulComparator.SMULL, returnReg, returnReg
              .getNextReg(), returnReg, returnReg.getNextReg()));
          program.add(new ARMCmpNode(returnReg.getNextReg(), new OperandTwo(
              returnReg, ShiftType.ASR, 31)));
        }
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR_NE);
        break;
      case NOTEQUALS:
        program.add(new ARMCmpNode(op1Reg, new OperandTwo(op2Reg)));
        program.add(new ARMMovNode(dstReg, new OperandTwo(1, false),
            MovComparator.MOVNE));
        program.add(new ARMMovNode(dstReg, new OperandTwo(0, false),
            MovComparator.MOVEQ));
        break;
      case PLUS:
        program.add(new ARMAddNode(dstReg, op1Reg, new OperandTwo(op2Reg),
            AddComparator.ADDS));
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR);
        break;
    }
  }

  public void translateBoolLiteralNode(BoolLiteralNode node) {
    program.add(new ARMMovNode(returnReg, new OperandTwo(node.getValue() ? 1
        : 0, false), MovComparator.MOV));
  }

  public void translateCharLiteralNode(CharLiteralNode node) {
    program.add(new ARMMovNode(returnReg,
        new OperandTwo(node.getValue(), true), MovComparator.MOV));
  }

  // Needs to find offset from the Variable,
  // which gets it from the symbol table, and set lastIdentOffset once found
  public void translateIdentNode(IdentNode node) {
    lastIdentOffset = currentScope.getOffset(node.getVariable());
    if (!currentFunction.isMain()
        && currentFunction.isParam(node.toString(), currentScope)) {
      lastIdentOffset += 4;
    }
    LdrComparator comparator = (node.getSizeInBytes() != 1) ? LdrComparator.LDR
        : LdrComparator.LDRSB;
    if (translatingPrintOrFree) {

      program.add(new ARMLdrNode(returnReg, new Register(SpecialReg.sp),
          lastIdentOffset, comparator));

    } else if (translatingRead) {
      LinkedList<OperandTwo> operands = separateIntoValidSizes(lastIdentOffset);
      for (OperandTwo operandTwo : operands) {
        program.add(new ARMAddNode(returnReg, new Register(SpecialReg.sp),
            operandTwo, AddComparator.ADD));
      }
    } else if (!translatingAssignLhs || translatingArrayElemIndex) {

      program.add(new ARMLdrNode(returnReg, new Register(SpecialReg.sp),
          lastIdentOffset, comparator));
    }
  }

  public void translateIntLiteralNode(IntLiteralNode node) {
    program.add(new ARMLdrNode(returnReg, node.getValue(), LdrComparator.LDR));
  }

  /*
   * Will be called when translating pair(type, type) p = newpair(a, b) (One of
   * the counterparts of ArrayLiteral, along with PairLiteral and Null)
   */
  public void translateNewPairNode(NewPairNode node) {
    // First 3 instructions malloc space for pair (2 pointers = 2 * 4bytes =
    // 8bytes)
    // and put the address of this space into returnReg

    program.add(new ARMLdrNode(new Register(SpecialReg.r0), 8,
        LdrComparator.LDR));

    program.add(new ARMBranchNode("malloc", BranchComparator.BL));
    program.add(new ARMMovNode(returnReg, new OperandTwo(new Register(
        SpecialReg.r0)), MovComparator.MOV));
    // Put first element of pair into returnReg.getNextReg()
    incReturnReg();
    node.getLeft().translate(this);
    decReturnReg();
    // malloc space for first element (which will go in first 4 bytes of the 8
    // bytes malloc'd above)
    int sizeElem1 = (node.getLeft().getSizeInBytes());

    program.add(new ARMLdrNode(new Register(SpecialReg.r0), sizeElem1,
        LdrComparator.LDR));

    program.add(new ARMBranchNode("malloc", BranchComparator.BL));
    // Put first element of pair in this most recently malloc'd space
    StrComparator comparator = chooseStrComparator(node.getLeft());
    program.add(new ARMStrNode(returnReg.getNextReg(), new Register(
        SpecialReg.r0), 0, false, comparator));
    // Put the address of most recently malloc'd space into first 4 bytes of the
    // 8 bytes malloc'd earlier for the whole pair
    program.add(new ARMStrNode(new Register(SpecialReg.r0), returnReg, 0,
        false, StrComparator.STR));
    // Put second element of pair into returnReg.getNextReg()
    incReturnReg();
    node.getRight().translate(this);
    decReturnReg();
    int sizeElem2 = (node.getRight().getSizeInBytes());

    program.add(new ARMLdrNode(new Register(SpecialReg.r0), sizeElem2,
        LdrComparator.LDR));

    program.add(new ARMBranchNode("malloc", BranchComparator.BL));
    StrComparator comparator2 = chooseStrComparator(node.getRight());
    program.add(new ARMStrNode(returnReg.getNextReg(), new Register(
        SpecialReg.r0), 0, false, comparator2));
    // Put the address of most recently malloc'd space into second 4 bytes of
    // the 8 bytes malloc'd earlier for the whole pair
    program.add(new ARMStrNode(new Register(SpecialReg.r0), returnReg, 4,
        false, StrComparator.STR));
    // Now both elements are in the pair, where returnReg holds address of pair
  }

  public void translatePairElemNode(PairElemNode node) {
    incReturnReg();
    node.getExpr().translate(this);
    decReturnReg();

    program.add(new ARMLdrNode(returnReg, new Register(SpecialReg.sp),
        lastIdentOffset, LdrComparator.LDR));
    program.add(new ARMMovNode(new Register(SpecialReg.r0), new OperandTwo(
        returnReg), MovComparator.MOV));
    predefinedFunctionHandler
        .addfunction(PredefinedFunction.P_CHECK_NULL_POINTER);
    int offset = node.getIsFst() ? 0 : 4;

    program
        .add(new ARMLdrNode(returnReg, returnReg, offset, LdrComparator.LDR));
    if (!translatingAssignLhs) {

      program.add(new ARMLdrNode(returnReg, returnReg, 0,
          chooseLdrComparator(node)));
    }
  }

  public void translateUnaryOpNode(UnaryOpNode node) {
    node.getArg().translate(this);

    switch (node.getOp()) {
      case CHR:
        // This case is handled by previous logic
        break;
      case LEN:
        program.add(new ARMLdrNode(returnReg, new Register(SpecialReg.sp),
            lastIdentOffset, LdrComparator.LDR));
        program.add(new ARMLdrNode(returnReg, returnReg, 0, LdrComparator.LDR));
        break;
      case LOGICAL_NOT:
        program.add(new ARMOrNode(returnReg, returnReg,
            new OperandTwo(1, false), ARMOrNode.OrOperator.EOR));
        break;
      case NEGATION:
        program.add(new ARMSubNode(returnReg, returnReg, new OperandTwo(0,
            false), ARMSubNode.SubComparator.RSBS));
        // program
        predefinedFunctionHandler
            .addfunction(PredefinedFunction.P_THROW_OVERFLOW_ERROR);
        break;
      case ORD:
        // This case is handled by previous logic
        break;
      default:
        break;
    }
  }

  // Null pointer is just 0
  public void translateNullNode(NullNode node) {
    program.add(new ARMLdrNode(returnReg, 0, LdrComparator.LDR));
  }
}
