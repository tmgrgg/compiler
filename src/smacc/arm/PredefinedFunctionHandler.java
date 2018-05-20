package smacc.arm;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import smacc.arm.ARMLdrNode.LdrComparator;
import smacc.arm.ARMBranchNode.BranchComparator;
import smacc.arm.ARMAddNode.AddComparator;

/*
 * Predefined Function Handler
 * Used to define the static, unchanging functions used to implement wacc statements and expressions
 * 
 * Messages are placed in file start node
 * Functions are placed in file end node
 */
public class PredefinedFunctionHandler {

  public enum PredefinedFunction {
    P_THROW_RUNTIME_ERROR, P_THROW_OVERFLOW_ERROR, P_THROW_OVERFLOW_ERROR_NE, 
    P_PRINT_STRING, P_PRINT_INT, P_PRINT_REFERENCE, P_PRINTLN, P_PRINT_BOOL, 
    P_READ_INT, P_READ_CHAR, P_FREE_ARRAY, P_CHECK_DIVIDE_BY_ZERO, P_CHECK_ARRAY_BOUNDS, 
    P_CHECK_NULL_POINTER, P_FREE_PAIR
  }

  private ARMFileStart fileStart;
  private List<ARMNode> program;

  private static final Register REG_R0 = new Register(Register.SpecialReg.r0);
  private static final Register REG_R1 = new Register(Register.SpecialReg.r1);
  private static final Register REG_R2 = new Register(Register.SpecialReg.r2);
  private static final Register REG_SP = new Register(Register.SpecialReg.sp);
  private static final Register REG_PC = new Register(Register.SpecialReg.pc);

  private Map<PredefinedFunction, List<ARMNode>> predefinedFunctions;

  public PredefinedFunctionHandler(ARMFileStart fileStart, List<ARMNode> program) {
    this.program = program;
    this.fileStart = fileStart;
    this.predefinedFunctions = new HashMap<>();
  }

  public void addfunction(PredefinedFunction predefinedFunction) {
    switch (predefinedFunction) {
    case P_PRINT_BOOL:
      program.add(new ARMBranchNode("p_print_bool", BranchComparator.BL));
      addPrintBool();
      break;
    case P_PRINT_INT:
      program.add(new ARMBranchNode("p_print_int", BranchComparator.BL));
      addPrintInt();
      break;
    case P_PRINT_REFERENCE:
      program.add(new ARMBranchNode("p_print_reference", BranchComparator.BL));
      addPrintReference();
      break;
    case P_PRINT_STRING:
      program.add(new ARMBranchNode("p_print_string", BranchComparator.BL));
      addPrintString();
      break;
    case P_PRINTLN:
      program.add(new ARMBranchNode("p_println", BranchComparator.BL));
      addPrintln();
      break;
    case P_THROW_OVERFLOW_ERROR:
      program.add(new ARMBranchNode("p_throw_overflow_error",
          BranchComparator.BLVS));
      addPrintOverflowError();
      break;
    case P_THROW_OVERFLOW_ERROR_NE:
      program.add(new ARMBranchNode("p_throw_overflow_error",
          BranchComparator.BLNE));
      addPrintOverflowError();
      break;
    case P_THROW_RUNTIME_ERROR:
      program.add(new ARMBranchNode("p_throw_runtime_error",
          BranchComparator.BL));
      addPrintRuntimeError();
      break;
    case P_READ_INT:
      program.add(new ARMBranchNode("p_read_int", BranchComparator.BL));
      addPrintReadInt();
      break;
    case P_READ_CHAR:
      program.add(new ARMBranchNode("p_read_char", BranchComparator.BL));
      addPrintReadChar();
      break;
    case P_FREE_ARRAY:
      program.add(new ARMBranchNode("p_free_array", BranchComparator.BL));
      addFreeArray();
      break;
    case P_FREE_PAIR:
      program.add(new ARMBranchNode("p_free_pair", BranchComparator.BL));
      addFreePair();
      break;
    case P_CHECK_DIVIDE_BY_ZERO:
      program.add(new ARMBranchNode("p_check_divide_by_zero",
          BranchComparator.BL));
      addCheckDivideByZero();
      break;
    case P_CHECK_ARRAY_BOUNDS:
      program
          .add(new ARMBranchNode("p_check_array_bounds", BranchComparator.BL));
      addCheckArrayBounds();
      break;
    case P_CHECK_NULL_POINTER:
      program
          .add(new ARMBranchNode("p_check_null_pointer", BranchComparator.BL));
      addCheckNullPointer();
      break;
    default:
    }
  }

  private void addPrintBool() {
    ArmMessage trueMessage = new ArmMessage("true\\0", false);
    ArmMessage falseMessage = new ArmMessage("false\\0", false);
    fileStart.addMessage(trueMessage);
    fileStart.addMessage(falseMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_print_bool"));
    function.add(new ARMFunctionStart());
    function.add(new ARMCmpNode(REG_R0, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(trueMessage),
        ARMLdrNode.LdrComparator.LDRNE));
    function
        .add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(falseMessage),
            ARMLdrNode.LdrComparator.LDREQ));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        ARMAddNode.AddComparator.ADD));
    function
        .add(new ARMBranchNode("printf", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMMovNode(REG_R0, new OperandTwo(0, false),
        ARMMovNode.MovComparator.MOV));
    function
        .add(new ARMBranchNode("fflush", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_PRINT_BOOL, function);
  }

  private void addPrintInt() {
    ArmMessage intMessage = new ArmMessage("%d\\0", false);
    fileStart.addMessage(intMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_print_int"));
    function.add(new ARMFunctionStart());
    function.add(new ARMMovNode(REG_R1, new OperandTwo(REG_R0),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(intMessage),
        LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        ARMAddNode.AddComparator.ADD));
    function
        .add(new ARMBranchNode("printf", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMMovNode(REG_R0, new OperandTwo(0, false),
        ARMMovNode.MovComparator.MOV));
    function
        .add(new ARMBranchNode("fflush", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_PRINT_INT, function);
  }

  private void addPrintReference() {
    ArmMessage referenceMessage = new ArmMessage("%p\\0", false);
    fileStart.addMessage(referenceMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_print_reference"));
    function.add(new ARMFunctionStart());
    function.add(new ARMMovNode(REG_R1, new OperandTwo(REG_R0),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(referenceMessage), LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        ARMAddNode.AddComparator.ADD));
    function
        .add(new ARMBranchNode("printf", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMMovNode(REG_R0, new OperandTwo(0, false),
        ARMMovNode.MovComparator.MOV));
    function
        .add(new ARMBranchNode("fflush", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));

    predefinedFunctions.put(PredefinedFunction.P_PRINT_REFERENCE, function);
  }

  private void addPrintString() {
    ArmMessage stringMessage = new ArmMessage("%.*s\\0", false);
    fileStart.addMessage(stringMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_print_string"));
    function.add(new ARMFunctionStart());
    function.add(new ARMLdrNode(REG_R1, REG_R0, 0, LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R2, REG_R0, new OperandTwo(4, false),
        ARMAddNode.AddComparator.ADD));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(stringMessage), LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        ARMAddNode.AddComparator.ADD));
    function
        .add(new ARMBranchNode("printf", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMMovNode(REG_R0, new OperandTwo(0, false),
        ARMMovNode.MovComparator.MOV));
    function
        .add(new ARMBranchNode("fflush", ARMBranchNode.BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_PRINT_STRING, function);
  }

  private void addPrintln() {
    ArmMessage printlnMessage = new ArmMessage("\\0", false);
    fileStart.addMessage(printlnMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_println"));
    function.add(new ARMFunctionStart());
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(printlnMessage), LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        AddComparator.ADD));
    function.add(new ARMBranchNode("puts", BranchComparator.BL));
    function.add(new ARMMovNode(REG_R0, new OperandTwo(0, false),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMBranchNode("fflush", BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_PRINTLN, function);
  }

  private void addPrintOverflowError() {
    ArmMessage overflowMessage = new ArmMessage(
        "OverflowError: the result is too small/"
            + "large to store in a 4-byte signed-integer.\\n", false);
    fileStart.addMessage(overflowMessage);

    List<ARMNode> function = new LinkedList<>();
    function.add(new ARMLabel("p_throw_overflow_error"));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(overflowMessage), LdrComparator.LDR));
    function
        .add(new ARMBranchNode("p_print_runtime_error", BranchComparator.BL));
    addPrintRuntimeError();
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions
        .put(PredefinedFunction.P_THROW_OVERFLOW_ERROR, function);

  }

  private void addPrintRuntimeError() {
    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_print_runtime_error"));
    function.add(new ARMBranchNode("p_print_string", BranchComparator.BL));
    addPrintString();
    function.add(new ARMMovNode(REG_R0, new OperandTwo(-1, false),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMBranchNode("exit", BranchComparator.BL));
    predefinedFunctions.put(PredefinedFunction.P_THROW_RUNTIME_ERROR, function);
  }

  private void addPrintReadInt() {
    ArmMessage intMessage = new ArmMessage("%d\\0", false);
    fileStart.addMessage(intMessage);

    List<ARMNode> function = new LinkedList<>();
    function.add(new ARMLabel("p_read_int"));
    function.add(new ARMFunctionStart());
    function.add(new ARMMovNode(REG_R1, new OperandTwo(REG_R0),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(intMessage),
        LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        AddComparator.ADD));
    function.add(new ARMBranchNode("scanf", BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_READ_INT, function);
  }

  private void addPrintReadChar() {
    ArmMessage charMessge = new ArmMessage(" %c\\0", false);
    fileStart.addMessage(charMessge);
    List<ARMNode> function = new LinkedList<>();
    function.add(new ARMLabel("p_read_char"));
    function.add(new ARMFunctionStart());
    function.add(new ARMMovNode(REG_R1, new OperandTwo(REG_R0),
        ARMMovNode.MovComparator.MOV));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(charMessge),
        LdrComparator.LDR));
    function.add(new ARMAddNode(REG_R0, REG_R0, new OperandTwo(4, false),
        AddComparator.ADD));
    function.add(new ARMBranchNode("scanf", BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_READ_CHAR, function);
  }

  private void addFreeArray() {
    List<ARMNode> function = new LinkedList<>();
    ArmMessage freeMessage = new ArmMessage(
        "NullReferenceError: dereference a null reference\\n\\0", false);
    function.add(new ARMLabel("p_free_array"));
    fileStart.addMessage(freeMessage);
    function.add(new ARMFunctionStart());
    function.add(new ARMCmpNode(REG_R0, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(freeMessage),
        LdrComparator.LDREQ));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BEQ));
    addPrintRuntimeError();
    function.add(new ARMBranchNode("free", BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_FREE_ARRAY, function);
  }

  private void addFreePair() {
    List<ARMNode> function = new LinkedList<>();
    ArmMessage freeMessage = new ArmMessage(
        "NullReferenceError: dereference a null reference\\n\\0", false);
    function.add(new ARMLabel("p_free_pair"));
    fileStart.addMessage(freeMessage);
    function.add(new ARMFunctionStart());
    function.add(new ARMCmpNode(REG_R0, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart.getMessageLabel(freeMessage),
        LdrComparator.LDREQ));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BEQ));
    addPrintRuntimeError();

    function.add(new ARMPushNode(REG_R0));
    function.add(new ARMLdrNode(REG_R0, REG_R0, 0, LdrComparator.LDR));
    function.add(new ARMBranchNode("free", BranchComparator.BL));
    function.add(new ARMLdrNode(REG_R0, REG_SP, 0, LdrComparator.LDR));
    function.add(new ARMLdrNode(REG_R0, REG_R0, 4, LdrComparator.LDR));
    function.add(new ARMBranchNode("free", BranchComparator.BL));
    function.add(new ARMPopNode(REG_R0));

    function.add(new ARMBranchNode("free", BranchComparator.BL));
    function.add(new ARMPopNode(REG_PC));

    predefinedFunctions.put(PredefinedFunction.P_FREE_ARRAY, function);
  }

  private void addCheckDivideByZero() {
    List<ARMNode> function = new LinkedList<>();
    ArmMessage divideByZeroMessage = new ArmMessage(
        "DivideByZeroError: divide or modulo by zero\\n\\0", false);
    fileStart.addMessage(divideByZeroMessage);
    function.add(new ARMLabel("p_check_divide_by_zero"));
    function.add(new ARMFunctionStart());
    function.add(new ARMCmpNode(REG_R1, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(divideByZeroMessage), LdrComparator.LDREQ));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BLEQ));
    addPrintRuntimeError();
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions
        .put(PredefinedFunction.P_CHECK_DIVIDE_BY_ZERO, function);
  }

  private void addCheckArrayBounds() {
    ArmMessage negativeIndexMessage = new ArmMessage(
        "ArrayIndexOutOfBoundsError: negative index\\n\\0", false);
    ArmMessage indexTooLargeMessage = new ArmMessage(
        "ArrayIndexOutOfBoundsError: index too large\\n\\0", false);
    fileStart.addMessage(negativeIndexMessage);
    fileStart.addMessage(indexTooLargeMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_check_array_bounds"));
    function.add(new ARMFunctionStart());

    function.add(new ARMCmpNode(REG_R0, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(negativeIndexMessage), LdrComparator.LDRLT));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BLLT));
    addPrintRuntimeError();
    function.add(new ARMLdrNode(REG_R1, REG_R1, 0, LdrComparator.LDR));
    function.add(new ARMCmpNode(REG_R0, new OperandTwo(REG_R1)));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(indexTooLargeMessage), LdrComparator.LDRCS));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BLCS));
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_CHECK_ARRAY_BOUNDS, function);
  }

  private void addCheckNullPointer() {
    ArmMessage nullReferenceErrorMessage = new ArmMessage(
        "NullReferenceError: dereference a null reference\\n\\0", false);
    fileStart.addMessage(nullReferenceErrorMessage);

    List<ARMNode> function = new LinkedList<>();

    function.add(new ARMLabel("p_check_null_pointer"));
    function.add(new ARMFunctionStart());
    function.add(new ARMCmpNode(REG_R0, new OperandTwo(0, false)));
    function.add(new ARMLdrNode(REG_R0, fileStart
        .getMessageLabel(nullReferenceErrorMessage), LdrComparator.LDREQ));
    function.add(new ARMBranchNode("p_print_runtime_error",
        BranchComparator.BLEQ));
    addPrintRuntimeError();
    function.add(new ARMPopNode(REG_PC));
    predefinedFunctions.put(PredefinedFunction.P_CHECK_NULL_POINTER, function);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (PredefinedFunction key : predefinedFunctions.keySet()) {
      List<ARMNode> function = predefinedFunctions.get(key);
      for (ARMNode node : function) {
        sb.append(node.toString());
      }
    }
    return sb.toString();
  }
}
