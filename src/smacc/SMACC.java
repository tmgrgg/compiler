package smacc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import smacc.arm.ARMNode;
import smacc.ast.ASTStaticViewer;
import smacc.exceptions.InvalidProgramTreeError;

import antlr.WACCLexer;
import antlr.WACCParser;

public class SMACC {

  public static final String helpText = "SMACC: Simple WACC compiler written by"
      + " Daniel Slocombe, Kiran Patel,\nWilliam Springsteen and Thomas Grigg\n "
      + "\nUsage: ./compile program.wacc"
      + "\nor ./compile -g graph.png --format png -v program.wacc"
      + "\n\nWhere the WACC source is always the last argument"
      + "\nOutputs ARM Assembly to program.s"
      + "\n\nAdditional arguments:"
      + "\n  -h,  --help\t\t\tShow this message"
      + "\n  -o,  --output\t\t\tOnly required when running java directly, \n\t\t\t\tspecifies output file"
      + "\n  -g,  --graph\t\t\tProduce a graph of the constructed AST of the \n\t\t\t\tprogram with the following filename (requires \n\t\t\t\tGraphviz dot)"
      + "\n  -gnc,--graph-no-compile\tProduce a graph but do not compile the \n\t\t\t\tGraphviz source"
      + "\n  -ug, --unstructured-graph\tGraph produced is unstructured \n\t\t\t\tie. functions are put in place"
      + "\n  -f,  --format\t\t\tSpecify graph output format, \n\t\t\t\tSupported formats: ps, png, svg"
      + "\n  -q,  --quiet\t\t\tQuiet mode, suppresses all message text"
      + "\n  -v,  --verbose\t\tVerbose mode, increases amount of message text";

  public static final int EXIT_SYNTACTIC_ERR = 100;
  public static final int EXIT_SEMANTIC_ERR = 200;
  public static final int EXIT_RUNTIME_ERR = 255;
  public static final int EXIT_SUCCESS = 0;

  public static final int EXIT_INVALID_ARGUMENT = 1;
  public static final int EXIT_INVALID_FORMAT = 1;

  public static void main(String[] args) {

    // Process arguments, options and flags

    boolean produceGraph = false;
    boolean compileGraph = true;
    boolean graphStructured = true;
    ASTStaticViewer.GraphFormat graphFormat = ASTStaticViewer.GraphFormat.PS;
    String graphPath = "astgraph";

    int feedbackLevel = 1;
    boolean writeArmToSTDOut = true;
    String armPath = "out.s";

    for (int i = 0; i < args.length; i++) {

      String arg = args[i];

      switch (arg) {

        case "-h":
        case "--help":
          // Print help text and then exit
          System.out.println(helpText);
          System.exit(EXIT_SUCCESS);
          break;

        case "-o":
        case "--output":
          // Next argument should be arm output file
          // Check for invalid filenames?
          armPath = args[++i];
          writeArmToSTDOut = false;
          break;

        case "-g":
        case "--graph":
          // Next argument should be graph output file
          // Check for invalid filenames?
          graphPath = args[++i];
          produceGraph = true;
          break;

        case "-gnc":
        case "--graph-no-compile":
          // Next argument should be graph output file
          // Check for invalid filenames?
          graphPath = args[++i];
          produceGraph = true;
          compileGraph = false;
          break;

        case "-ug":
        case "--unstructured-graph":
          graphStructured = false;
          break;

        case "-f":
        case "--format":
          // Analyse next argument - look for file format
          arg = args[++i];
          switch (arg) {
            case "PS":
            case "ps":
              graphFormat = ASTStaticViewer.GraphFormat.PS;
              break;
            case "PNG":
            case "png":
              graphFormat = ASTStaticViewer.GraphFormat.PNG;
              break;
            case "SVG":
            case "svg":
              graphFormat = ASTStaticViewer.GraphFormat.SVG;
              break;
            default:
              // Exit on unknown file format
              System.err.printf("SMACC: unrecognised file format %s\n", arg);
              System.err.println("for supported formats see ./compile --help");
              System.exit(EXIT_INVALID_FORMAT);
          }
          break;

        case "-q":
        case "--quiet":
          feedbackLevel = 0;
          break;

        case "-v":
        case "--verbose":
          feedbackLevel = 2;
          break;

        default:
          // Exit with an error on invalid option
          System.err.printf(
              "SMACC: unrecognised option %s, try ./compile --help\n", arg);
          System.exit(EXIT_INVALID_ARGUMENT);

      }
    }

    // Get source input from stdin
    CharStream input;
    try {
      input = new ANTLRInputStream(System.in);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    // Build list of newline positions for error messages
    List<Integer> newlinePositions = new ArrayList<Integer>();
    int pos = 0;
    newlinePositions.add(pos);

    // Guarantees we can seek back
    input.mark();

    if (input.LA(0) != ANTLRInputStream.EOF) {
      while (input.LA(1) != ANTLRInputStream.EOF) {
        pos++;
        if (input.LA(1) == '\n') {
          newlinePositions.add(pos);
        }
        input.consume();
      }
      newlinePositions.add(pos);
    }

    // Reset position
    input.seek(0);

    // Lex the input
    if (feedbackLevel > 1)
      System.out.printf("Lexing...  ");
    WACCLexer lexer = new WACCLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    // Parse the input
    if (feedbackLevel > 1)
      System.out.printf("Done!\nParsing...  ");
    WACCParser parser = new WACCParser(tokens);
    parser.addErrorListener(new SMACCTreeErrorListener());

    ParseTree program = null;
    try {
      program = parser.program();
    } catch (InvalidProgramTreeError e) {
      // We do not want to continue with an invalid syntax tree
      System.exit(EXIT_SYNTACTIC_ERR);
    }

    if (feedbackLevel > 1)
      System.out.printf("Done!\nChecking for errors in source...");

    // Setup error message container, function table, visitor for first pass
    // After first pass function table will hold the structure of the AST

    ErrorMessageContainer ec = new ErrorMessageContainer(input,
        newlinePositions);
    ec.setupErrorCodes(EXIT_SUCCESS, EXIT_SYNTACTIC_ERR, EXIT_SEMANTIC_ERR,
        EXIT_RUNTIME_ERR);

    FunctionTable funcTable = new FunctionTable();

    WACCFirstPass visitor = new WACCFirstPass(funcTable, ec);

    visitor.visit(program);
    if (ec.getErrorCount() > 0) {
      System.err.println(ec.toString());
      System.exit(ec.getErrorCode());
    }

    if (feedbackLevel > 1)
      System.out.printf("Done!\n");

    // Produce graph if options are set
    if (produceGraph) {
      if (feedbackLevel > 0) {
        System.out.println("Producing graph");
      }
      if (feedbackLevel > 1) {
        System.out.printf("With options output=%s compiling=%b format=%s\n",
            graphPath, compileGraph, graphFormat.toString());
      }
      ASTStaticViewer.constructGraphFromFunctiontable(funcTable, graphPath,
          compileGraph, graphFormat, graphStructured, feedbackLevel);
    }

    // Prepare stream to write assembly
    BufferedWriter out = null;
    if (writeArmToSTDOut) {
      if (feedbackLevel > 0)
        System.out.println("Writing output to stdout... ");
      out = new BufferedWriter(new OutputStreamWriter(System.out));
    } else {
      try {
        if (feedbackLevel > 0)
          System.out.printf("Writing output to %s... \n", armPath);
        out = new BufferedWriter(new FileWriter(armPath));
      } catch (IOException e) {
        // Default to stdout if file cannot be opened
        System.err.printf(
            "SMACC: Could not open %s for writing, defaulting to stdout",
            args[0]);
        out = new BufferedWriter(new OutputStreamWriter(System.out));
      }
    }

    // Translate AST into ARM nodes then convert to strings
    List<ARMNode> arm = funcTable.translate();
    for (ARMNode node : arm) {
      try {
        out.write(node.toString());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Flush and close output stream
    try {
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (feedbackLevel > 0)
      System.out.println("Done!");

    System.exit(EXIT_SUCCESS);
  }
}
