package pilots.compiler.codegen;

import java.io.*;
import java.util.*;
import pilots.compiler.parser.*;
import pilots.runtime.*;

public class PilotsCodeGenerator implements PilotsParserVisitor {
    private static final String TAB = "    ";
    private static int indent = 0;

    private String appName = null;
    private List<InputStream> inputs = null;
    private List<String> constants = null;
    private List<OutputStream> outputs = null;
    private List<OutputStream> errors = null;
    private List<Signature> sigs = null;
    private List<Mode> modes = null;
    private List<Correct> corrects = null;
    private String code = null;
    private boolean sim = false;
    private Map<String, String> varsMap = null;

    private static int depth = 0;
    
    public static void main(String[] args) {
        try {
            PilotsParser parser = new PilotsParser(new FileReader(args[0]));
            Node node = parser.Pilots();
            PilotsCodeGenerator visitor = new PilotsCodeGenerator();
            node.jjtAccept(visitor, null);
        } 
        catch (FileNotFoundException ex) {
            System.err.println("FileNotFoundException: " + ex.getMessage());
        }
        catch (TokenMgrError ex) {
            System.err.println("TokeMgrError: " +  ex.getMessage());
        }
        catch (ParseException ex) {
            System.err.println("ParseException: " +  ex.getMessage());
        }
    }

    public PilotsCodeGenerator() {
        inputs =  new ArrayList<>();
        constants = new ArrayList<>();
        outputs = new ArrayList<>();
        errors = new ArrayList<>();
        sigs = new  ArrayList<>();
        modes = new ArrayList<>();
        corrects = new ArrayList<>();
        code = new String();
        varsMap = new HashMap<>(); // Store variables in inputs

        if (System.getProperty("sim") != null)
            sim = true;
    }

    private void goDown(String node) {
        depth++;
    }

    private void goUp() {
        depth--;
    }

    private void incIndent() {
        indent++;
    }

    private void decIndent() {
        indent--;
    }

    private String insIndent() {
        String tab = "";
        for (int i = 0; i < indent; i++)
            tab += TAB;
        return tab;
    }

    private String incInsIndent() {
        incIndent();
        return insIndent();
    }

    private String decInsIndent() {
        decIndent();
        return insIndent();
    }

    private void acceptChildren(SimpleNode node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).jjtAccept(this, data);
        }
    }

    private void generateImports() {
        String p = System.getProperty("package");
        if (p != null) {
            code += "package " + p + ";\n";
            code += "\n";
        }
        if (sim)
            code += "import java.io.*;\n";
        code += "import java.util.*;\n";
        code += "import java.text.*;\n";
        code += "import java.net.Socket;\n";
        code += "import pilots.runtime.*;\n";
        code += "import pilots.runtime.errsig.*;\n";
        code += "\n";
    }

    private void generateClassDeclaration() {
        code += "public class " + appName + " extends PilotsRuntime {\n";
        code += incInsIndent() + "private int currentMode;\n";
        code += insIndent() + "private int currentModeCount;\n";
        if (sim)
            code += insIndent() + "private int time; // msec\n";
        else
            code += insIndent() + "private Timer timer;\n";
        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);
            String[] outputVarNames = output.getVarNames();
            code += insIndent() + "private SlidingWindow win_" + outputVarNames[0] + ";\n";
        }
        if (sigs != null) {
            code += insIndent() + "private List<ErrorSignature> errorSigs;\n";
            code += insIndent() + "private ErrorAnalyzer errorAnalyzer;\n";
        }
        code += "\n";
    }

    private void generateConstants() {
        for (String constant : constants) {
            code += insIndent() + "private static final double " +  constant + ";\n";
        }
        code += "\n";        
    }

    private void generateConstructor() {
        code += insIndent() + "public " + appName + "(String args[]) {\n";
        code += incInsIndent() + "try {\n";
        code += incInsIndent() + "parseArgs(args);\n";
        code += decInsIndent() + "} catch (Exception ex) {\n";
        code += incInsIndent() + "ex.printStackTrace();\n";
        code += decInsIndent() + "};\n";
        code += "\n";
        if (sim)
            code += insIndent() + "time = 0;\n";
        else 
            code += insIndent() + "timer = new Timer();\n";
        code += "\n";
        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);
            String[] outputVarNames = output.getVarNames();
            code += insIndent() + "win" + outputVarNames[0] + " = new SlidingWindow(getOmega());\n";
        }
        if (sigs != null)
            code += insIndent() + "errorSigs = new ArrayList<ErrorSignature>();\n";

        int constIndex = 1;
        for (int i = 0; i < sigs.size(); i++) {
            Signature sig = sigs.get(i);

            if (sig.isConstrained()) {
                code += insIndent()
                    + "Listr<Constraint> constraints"
                    + constIndex + " = new ArrayList<Constraint>();\n";
                List<Constraint> constraints = sig.getConstraints();
                for (int j = 0; j < constraints.size(); j++) {
                    Constraint c = constraints.get(j);
                    code += insIndent() + "constraints" + constIndex + ".add(new Constraint(Constraint." + c.getTypeString() + ", " + c.getValue() + "));\n";
                }
            }

            code += insIndent() + "errorSigs.add(new ErrorSignature(ErrorSignature.";
            if (sig.getType() == Signature.CONST)
                code += "CONST, ";
            else if (sig.getType() == Signature.LINEAR)
                code += "LINEAR, ";
            else {
                System.err.println("No valid type found for: " + sig);
            }
            if (sig.getDesc() != null)
                code += sig.getValue() + ", " + sig.getDesc();
            else 
                code += sig.getValue() + ", null";

            if (sig.isConstrained()) {
                code += ", constraints" + constIndex + "));\n";
                constIndex++;
            }
            else
                code += "));\n";
            code += "\n";
        }

        code += insIndent() + "errorAnalyzer = new ErrorAnalyzer(errorSigs, getTau());\n";
        code += decInsIndent() + "}\n";
        code += "\n";
    }

    private String replaceVar(String exp, Map<String, String> map) {
        // Replace all variables in exp using entires in map
        // E.g. exp: "a + b" ==> "data.get("a") + data.get("b")"
        String newExp = "";
        StringTokenizer tokenizer = new StringTokenizer(exp, "()/*+-", true);

        while (tokenizer.hasMoreElements()) {
            String var = (String)tokenizer.nextElement();
            String hashVar = map.get(var);
            if (hashVar != null)
                newExp += hashVar;
            else 
                newExp += var;
        }

        return newExp;
    }

    private void generateGetCorrectedData() {
        // get all input variables in vars
        List<String> vars = new ArrayList<>();
        Map<String,String> map = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            InputStream input = inputs.get(i);
            String[] inputVarNames = input.getVarNames();
            for (int j = 0; j < inputVarNames.length; j++) {
                vars.add(inputVarNames[j]);
                map.put(inputVarNames[j], inputVarNames[j] + ".getValue()");
            }
        }

        // declaration
        code += insIndent() + "public void getCorrectedData(SlidingWindow win,\n";
        for (int i = 0; i < vars.size(); i++) {
            String var = vars.get(i);
            code += insIndent() + "                              ";
            code += "Value " + var + ", Value " + var + "corrected,\n";
        }
        code += insIndent() + "                              Mode mode, int frequency) {\n";

        // body --->
        
        // getData
        incIndent();
        // generateGetData();

        // e
        code += insIndent() + "double e = ";
        code += replaceVar(replaceMathFuncs(errors.get(0).getExp()), map);
        code += ";\n";
        code += "\n";

        // win & mode
        code += insIndent() + "win.push(e);\n";
        code += insIndent() + "mode.setMode(errorAnalyzer.analyze(win, frequency));\n";
        code += "\n";

        // correct values 
        for (int i = 0; i < vars.size(); i++) {
            String var = vars.get(i);
            code += insIndent() + var + "corrected.setValue(";
            code += map.get(var) + ");\n";
        }
        if (0 < corrects.size())
            code += insIndent() + "switch (mode.getMode()) {\n";
        for (int i = 0; i < corrects.size(); i++) {
            Correct correct = corrects.get(i) ;
            code += insIndent() + "case " + correct.getMode() + ":\n";
            code += incInsIndent() + correct.getVar() + "corrected.setValue(";
            code += replaceVar(replaceMathFuncs(correct.getExp()), map);
            code += ");\n";
            // reset other counters
            code += "setModeCount(" + correct.getMode() + ");\n";
            // trigger save state if this is the one we're recording.
            if (correct.saveState){
                code += String.format("triggerSaveState(%d, %d, \"%s\", %scorrected.getValue());\n", 
                    correct.getMode(), 
                    correct.saveStateTriggerModeCount,
                    correct.getVar(),
                    correct.getVar());
            }
            
            code += insIndent() + "break;\n";
            decIndent();
        }
        if (0 < corrects.size())
            code += insIndent() + "default: setModeCount(-1);\n}\n";

        code += decInsIndent() + "}\n";
        code += "\n";
    }

    public void generateInputs() {
        for (int i = 0; i < inputs.size(); i++) {
            InputStream input = inputs.get(i);
            String[] inputVarNames = input.getVarNames();
            for (int j = 0; j < inputVarNames.length; j++) {
                code += insIndent() + "data.put(\"" + inputVarNames[j]
                    + "\", getData(\"" + inputVarNames[j] + "\", ";
                varsMap.put(inputVarNames[j], "data.get(\"" + inputVarNames[j] + "\")");

                // methods
                List<Method> methods = input.getMethods();
                for (int l = 0; l < methods.size(); l++) {
                    Method method = methods.get(l);
                    if (l == 0)
                        code += "new Method(" + method.toString() + ")";
                    else 
                        code += ", new Method(" + method.toString() + ")";
                }
                code += "));\n";
            }
        }
    }

    public void generateError() {
        for (OutputStream error : errors) {
            code += insIndent() + "data.put(\"" + error.getVarNames()[0] + "\", ";
            code += replaceVar(replaceMathFuncs(error.getExp()), varsMap);
            code += ");\n";
        }
    }

    public String replaceMathFuncs(String exp) {
        String[] funcs1 = {"asin", "acos", "atan"};
        String[] funcs2 = {"sqrt", "sin", "cos", "abs", "PI"};
        String[] funcs3 = {"arcs", "arcc", "arct"};

        for (int i = 0; i < funcs1.length; i++)
            exp = exp.replaceAll(funcs1[i], funcs3[i]);

        for (int i = 0; i < funcs2.length; i++)
            exp = exp.replaceAll(funcs2[i], "Math." + funcs2[i]);

        for (int i = 0; i < funcs3.length; i++) 
            exp = exp.replaceAll(funcs3[i], "Math." + funcs1[i]);

        return exp;
    }

    public String replaceLogicalOps(String exp) {
        String[] opsSources = {"and", "or", "xor", "not"};
        String[] opsTargets = {" && ", " || ", " ^ ", " ! "};

        for (int i = 0; i < opsSources.length; i++) {
            exp = exp.replaceAll(opsSources[i], opsTargets[i]);
        }
        
        return exp;
    }

    private void generateSignaturesErrorDetection() {
        code += insIndent() + "int mode = -1;\n";
        for (OutputStream error : errors) {
            code += insIndent() + "win.push(data.get(\""
                + error.getVarNames()[0] + "\"));\n";
        }
        code += insIndent() + "mode = errorAnalyzer.estimateMode(win, frequency);\n";
    }
    
    private void generateModesErrorDetection() {
    }

    private void generateEstimation() {
        code += insIndent() + "switch (mode.getMode()) {\n";
        for (Correct correct : corrects) {
            code += insIndent() + "case " + correct.getMode() + ":\n";
            code += incInsIndent() + "data.put(\"" + correct.getVar() + "\", "
                + replaceVar(replaceMathFuncs(correct.getExp()), varsMap) + ");\n";
            // reset other counters
            code += "setModeCount(" + correct.getMode() + ");\n";
            // trigger save state if this is the one we're recording.
            if (correct.saveState){
                code += String.format("triggerSaveState(%d, %d, \"%s\", %scorrected.getValue());\n", 
                    correct.getMode(), 
                    correct.saveStateTriggerModeCount,
                    correct.getVar(),
                    correct.getVar());
            }
            
            code += insIndent() + "break;\n";
            decIndent();
        }
        code += insIndent() + "default:\n";
        incIndent();
        code += insIndent() + "setModeCount(-1);\n";
        code += insIndent() + "break;\n";
        decIndent();
        code += insIndent() + "}\n";
    }
    
    private void generateOutputs() {
        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);

            // method declaration
            String[] outputVarNames = output.getVarNames();
            code += insIndent() + "public void produceOutput_" + outputVarNames[0] + "() {\n";

            // openSocket
            code += incInsIndent() + "try {\n";
            code += incInsIndent() + "openSocket(OutputType.Output, " + i
                + ", new String(\"" + outputVarNames[0] + "\"));\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";
            code += insIndent() + "\n";
            
            // timer thread --->
            code += insIndent() + "final int frequency = " + output.getFrequency() + ";\n";
            if (sim)
                code += insIndent() + "while (!isEndTime()) {\n";
            else {
                code += insIndent() + "timer.scheduleAtFixedRate(new TimerTask() {\n";
                incIndent();
                code += incInsIndent() + "public void run() {\n";
            }

            // variable declaration
            List<String> vars = new ArrayList<>();
            Map<String,String> map = new HashMap<>();
            for (int j = 0; j < inputs.size(); j++) {
                InputStream input = inputs.get(j);
                String[] inputVarNames = input.getVarNames();
                for (int k = 0; k < inputVarNames.length; k++) {
                    vars.add(inputVarNames[k]);
                    map.put(inputVarNames[k], inputVarNames[k] + "corrected.getValue()");
                }
            }
            incIndent();
            for (int j = 0; j < vars.size(); j++) {
                String var = vars.get(j);
                code += insIndent() + "Value " + var + " = new Value();\n";
                code += insIndent() + "Value " + var + "corrected = new Value();\n";
            }
            code += insIndent() + "Mode mode = new Mode();\n";
            code += "\n";
            code += insIndent() + "getCorrectedData(win" + outputVarNames[0] + ", ";
            for (int j = 0; j < vars.size(); j++) {
                String var = vars.get(j);
                code += var + ", " + var + "corrected, ";
            }
            code += "mode, frequency);\n";
            code += insIndent() + "double " + outputVarNames[0] + " = ";
            code += replaceVar(replaceMathFuncs(output.getExp()), map) + ";\n";
            code += "\n";

            // errorAnalyzer
            code += insIndent() + "String desc = errorAnalyzer.getDesc(mode.getMode());\n";
            code += insIndent() + "dbgPrint(desc + \", " + outputVarNames[0]
                + "=\" + " + outputVarNames[0] + " + \" at \" + getTime());\n";
            code += "\n";

            // sendData
            code += insIndent() + "try {\n";
            code += incInsIndent() + "sendData(OutputType.Output, " + i + ", "
                + outputVarNames[0] + ");\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";

            if (sim) {
                code += "\n";
                code += insIndent() + "time += frequency;\n";
                code += insIndent() + "progressTime(frequency);\n";
                code += decInsIndent() + "}\n";
                code += "\n";
                code += insIndent() + "dbgPrint(\"Finished at \" + getTime());\n";
            }
            else {
                code += decInsIndent() + "}\n";
                decIndent();
                code += decInsIndent() + "}, 0, frequency);\n";
            }

            code += decInsIndent() + "}\n";
            code += "\n";
        }
    }

    private void generateProduceOutputsNoCorrection() {
        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);

            // method declaration
            String[] outputVarNames = output.getVarNames();
            code += insIndent() + "public void startOutput" + outputVarNames[0] + "() {\n";

            // openSocket
            code += incInsIndent() + "try {\n";
            code += incInsIndent() + "openSocket(OutputType.Output, " + i
                + ", \"" + outputVarNames[0] + "\");\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";
            code += insIndent() + "\n";
            
            // timer thread --->
            code += insIndent() + "final int frequency = " + output.getFrequency() + ";\n";
            if (sim)
                code += insIndent() + "while (!isEndTime()) {\n";
            else {
                code += insIndent() + "timer.scheduleAtFixedRate(new TimerTask() {\n";
                incIndent();
                code += incInsIndent() + "public void run() {\n";
            }

            // variable declaration
            List<String> vars = new ArrayList<>();
            HashMap<String,String> map = new HashMap<String,String>();
            for (int j = 0; j < inputs.size(); j++) {
                InputStream input = inputs.get(j);
                String[] inputVarNames = input.getVarNames();
                for (int k = 0; k < inputVarNames.length; k++) {
                    vars.add(inputVarNames[k]);
                    map.put(inputVarNames[k], inputVarNames[k] + ".getValue()");
                }
            }
            incIndent();
            for (int j = 0; j < vars.size(); j++) {
                String var = vars.get(j);
                code += insIndent() + "Value " + var + " = new Value();\n";
            }
            code += "\n";
            
            // getData
            // generateGetData();

            code += insIndent() + "double " + outputVarNames[0] + " = ";
            code += replaceVar(replaceMathFuncs(output.getExp()), map) + ";\n";
            code += "\n";

            code += insIndent() + "dbgPrint(\"" + outputVarNames[0] + "=\" + "
                + outputVarNames[0] + " + \" at \" + getTime());\n";

            // sendData
            code += insIndent() + "try {\n";
            code += incInsIndent() + "sendData(OutputType.Output, " + i + ", "
                + outputVarNames[0] + ");\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";

            if (sim) {
                code += "\n";
                code += insIndent() + "time += frequency;\n";
                code += insIndent() + "progressTime(frequency);\n";
                code += decInsIndent() + "}\n";
                code += "\n";
                code += insIndent() + "dbgPrint(\"Finished at \" + getTime());\n";
            }
            else {
                code += decInsIndent() + "}\n";
                decIndent();
                code += decInsIndent() + "}, 0, frequency);\n";
            }

            code += decInsIndent() + "}\n";
            code += "\n";
        }
    }

    private void generateModeCountFunctions(){
        code += insIndent() + "private void setModeCount(int mode){\n";
        code += incInsIndent() + "if (currentMode != mode){\n";
        code += incInsIndent() +  "currentMode = mode; currentModeCount = 0;\n";
        code += decInsIndent() + "}else{\n";
        code += incInsIndent() + "currentModeCount++;\n";
        code += decInsIndent() + "}\n";
        code += decInsIndent() + "}\n";
        code += insIndent() + "private void triggerSaveState(int mode, int count, String var, double value){\n";
        code += incInsIndent() + "if (currentMode == mode && currentModeCount > count){\n";
        code += incInsIndent() + "addData(var, String.format(\":%s:%s\", (new SimpleDateFormat(\"yyyy-MM-dd HHmmssSSSZ\")).format(getTime()), Double.toString(value)));\n";
        code += decInsIndent() + "}\n";
        code += decInsIndent() + "}\n";
    }

    private void generateMain() {
        code += insIndent() + "public static void main(String[] args) {\n";
        code += incInsIndent() + appName + " app = new " + appName + "(args);\n";
        code += insIndent() + "app.startServer();\n";

        if (sim) {
            code += "\n";
            code += insIndent() + "BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));\n";
            code += insIndent() + "System.out.println(\"Hit ENTER key after running input producer(s).\");\n";
            code += insIndent() + "try {\n";
            code += incInsIndent() + "reader.readLine();\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";
            code += "\n";
        }

        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);
            String[] outputVarNames = output.getVarNames();
            code += insIndent() + "app.startOutput" + outputVarNames[0] + "();\n";
        }
        code += decInsIndent() + "}\n";
        code += decInsIndent() + "}\n";
    }
    


    private void generateThreads() {
        // Create one thread per output
        for (int i = 0; i < outputs.size(); i++) {
            OutputStream output = outputs.get(i);

            // method declaration
            code += insIndent() + "public void produceOutput" + output.getVarNames()[0] + "() {\n";

            // openSocket
            code += incInsIndent() + "try {\n";
            code += incInsIndent() + "openSocket(OutputType.Output, " + i
                + ", new String(\"" + output.getVarNames()[0] + "\"));\n";
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";
            code += insIndent() + "\n";
            

            code += insIndent() + "final int frequency = " + output.getFrequency() + ";\n";
            code += insIndent() + "Map<String, Double> data = new HashMap<>();\n";
            if (sim)
                code += insIndent() + "while (!isEndTime()) {\n";
            else {
                code += insIndent() + "timer.scheduleAtFixedRate(new TimerTask() {\n";
                incIndent();
                code += incInsIndent() + "public void run() {\n";
            }

            incIndent();
            generateInputs();

            // Error detection & correction
            if (0 < errors.size()) {
                generateError();
                code += "\n";
                if (0 < sigs.size()) {
                    // Signatures-based error detection
                    generateSignaturesErrorDetection();
                    code += "\n";
                }
                else if(0 < modes.size()) {
                    // Modes-based error detection
                    generateModesErrorDetection();
                    code += "\n";
                }
                if (0 < corrects.size()) {
                    generateEstimation();
                }
            }
            
            // outputs
            code += "\n";
            for (String outputVarName : output.getVarNames()) {
                if (output.getExp() != null) {
                    code += insIndent() + "data.put(\"" + outputVarName + "\", "
                        + replaceVar(replaceMathFuncs(output.getExp()), varsMap) + ");\n";
                }
            }

            // sendData
            code += insIndent() + "try {\n";
            for (String outputVarName : output.getVarNames()) {
                code += incInsIndent() + "sendData(OutputType.Output, " + i + ", data.get("
                    + outputVarName + "\"));\n";
            }
            code += decInsIndent() + "} catch (Exception ex) {\n";
            code += incInsIndent() + "ex.printStackTrace();\n";
            code += decInsIndent() + "}\n";

            if (sim) {
                code += "\n";
                code += insIndent() + "time += frequency;\n";
                code += insIndent() + "progressTime(frequency);\n";
                code += decInsIndent() + "}\n";
                code += "\n";
                code += insIndent() + "dbgPrint(\"Finished at \" + getTime());\n";
            }
            else {
                code += decInsIndent() + "}\n";
                decIndent();
                code += decInsIndent() + "}, 0, frequency);\n";
            }

            code += decInsIndent() + "}\n";
            code += "\n";            
        }
    }

    private void generateCode() {
        generateImports();
        generateClassDeclaration();
        generateConstants();
        generateConstructor();
        generateThreads();
        generateModeCountFunctions();
        generateMain();
    }

    private void outputCode() {
        if (System.getProperty("stdout") != null) {
            System.out.println(code);
        }
        else {
            try {
                File file = new File(appName + ".java");
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.print(code);
                pw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    public Object visit(ASTPilots node, Object data) {
        goDown("Pilots");

        appName = (String) node.jjtGetValue();
        appName = appName.substring(0, 1).toUpperCase() + appName.substring(1);
        acceptChildren(node, null);
        generateCode();
        outputCode();

        goUp();

        return null;
    }

    public Object visit(ASTInput node, Object data) {
        goDown("Input");

        InputStream input = new InputStream();
        String[] varNames = ((String)node.jjtGetValue()).split(",");
        input.setVarNames(varNames);
        inputs.add(input);
        acceptChildren(node, input);

        goUp();

        return null;
    }

    public Object visit(ASTConstant node, Object data) {
        goDown("Constant");

        String[] vals = ((String)node.jjtGetValue()).split(":");
        constants.add(vals[0] + " = " + vals[1]);

        goUp();
        
        return null;
    }
    
    public Object visit(ASTOutput node, Object data) {
        goDown("Output");

        
        OutputStream output = new OutputStream();
        output.setOutputType(OutputType.Output);
        String[] vals = ((String)node.jjtGetValue()).split(":");

        String[] varNames = vals[0].split(",");
        output.setVarNames(varNames);
        output.setExp(vals[1]);

        int unit = 0;
        if (vals[3].equalsIgnoreCase("nsec") || vals[3].equalsIgnoreCase("usec")) {
            unit = 0;
        }
        else if (vals[3].equalsIgnoreCase("msec")) {
            unit = 1;
        }
        else if (vals[3].equalsIgnoreCase("sec")) {
            unit = 1000;
        }
        else if (vals[3].equalsIgnoreCase("min")) {
            unit = 60 * 1000;
        }
        else if (vals[3].equalsIgnoreCase("hour")) {
            unit = 60 * 60 * 1000;
        }
        else if (vals[3].equalsIgnoreCase("day")) {
            unit = 24 * 60 * 60 * 1000;
        }
        output.setFrequency((int)(Double.parseDouble(vals[2]) * unit)); // msec
        outputs.add(output);

        node.jjtGetChild(1).jjtAccept(this, output); // accept Exps() only

        goUp();

        return null;
    }

    public Object visit(ASTError node, Object data) {
        goDown("Error");

        OutputStream output = new OutputStream();
        output.setOutputType(OutputType.Error);
        String[] vals = ((String) node.jjtGetValue()).split(":");

        String[] varNames = vals[0].split(",");
        output.setVarNames(varNames);
        output.setExp(vals[1]);
        output.setFrequency(-1); // No frequency for error
        errors.add(output);

        node.jjtGetChild(1).jjtAccept(this, output); // accept Exps() only

        goUp();

        return null;
    }

    public Object visit(ASTSignature node, Object data) {
        goDown("Signature");

        String[] vals = ((String) node.jjtGetValue()).split(":");
        Signature sig = new Signature(vals[0], vals[1], vals[3], vals[4]);
        sigs.add(sig);
        // this should be exactly here, otherwise it will generate wrong result        
        node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, null);
        goUp();
        return null;
    }

    public Object visit(ASTMode node, Object data) {
        goDown("Mode");

        String[] vals = ((String)node.jjtGetValue()).split(":");

        if (vals[0].charAt(0) != 'm') {
            System.err.println("Illegel start of mode identifier: " + vals[0].charAt(0));
            return null;
        }
        int id = Integer.parseInt(vals[0].substring(1));
        String condition = replaceLogicalOps(vals[1]);
        Mode mode = new Mode(id, condition, vals[2]);
       
        goUp();
        return null;
    }
    
    public Object visit(ASTEstimate node, Object data){
        goDown("estimate");
        // BECAREFUL!! THIS WHOLE THING IS HACKED ONLY, IT DOESN'T COMPLY TO ANY DESIGN PATTERN!

        String[] vals = ((String) node.jjtGetValue()).split(":");
        String variable = vals[0]; String expression = vals[3];
        String when = vals[1]; String times = vals[2];

        int mode = -1;
        mode = sigs.size() - 1;
        String signame = sigs.get(mode).getName();
        String argument = sigs.get(mode).getArg();
        Correct correct = new Correct(mode, signame, argument, variable, expression);
        if (!when.equals("null")){
            correct.saveState = true;
            correct.saveStateTriggerModeCount = 1;
            if (!times.equals("null")){
                correct.saveStateTriggerModeCount = Integer.parseInt(times);
            }
        }

        corrects.add(correct); 
        goUp();
        return null;
    }

    public Object visit(ASTCorrect node, Object data) {
        goDown("Correct");

        String[] vals = ((String) node.jjtGetValue()).split(":");
        
        int mode = -1;
        for (int i = 0; i < sigs.size(); i++) {
            Signature sig = sigs.get(i) ;
            if (sig.getName().equalsIgnoreCase(vals[0])) {
                mode = i;
                break;
            }
        }

        Correct correct = new Correct(mode, vals[0], vals[1], vals[2], vals[3]);
        corrects.add(correct);

        goUp();

        return null;
    }

    public Object visit(ASTVars node, Object data) {
        goDown("Vars");  
        goUp();
        return null;
    }

    public Object visit(ASTConstInSignature node, Object data) {
        goDown("Const");        
        goUp();
        return null;
    }

    public Object visit(ASTDim node, Object data) {
        goDown("Dim");
        goUp();
        return null;
    }

    public Object visit(ASTMethod node, Object data) {
        goDown("Method");

        InputStream input = (InputStream)data;
        String[] vals = ((String) node.jjtGetValue()).split(":");
        String[] args = vals[1].split(",");

        int id;
        if (vals[0].equalsIgnoreCase("closest")) {
            id = Method.CLOSEST;
        } else if (vals[0].equalsIgnoreCase("euclidean")) {
            id = Method.EUCLIDEAN;
        } else if (vals[0].equalsIgnoreCase("interpolate")) {
            id = Method.INTERPOLATE;
        } else if (vals[0].equalsIgnoreCase("predict")){
            id = Method.PREDICT;
        } else {
            System.err.println("Invalid method: " + vals[0]);
            return null;
        }
        
        input.addMethod(id, args);
        goUp();

        return null;
    }

    public Object visit(ASTMethods node, Object data) {
        goDown("Methods");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTTime node, Object data) {
        goDown("Time");
        goUp();
        return null;
    }

    public Object visit(ASTExps node, Object data) {
        goDown("Exps");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTExp node, Object data) {
        goDown("Exp");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTExp2 node, Object data) {
        goDown("Exp2");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTFunc node, Object data) {
        goDown("Func");
        goUp();
        return null;
    }

    public Object visit(ASTNumber node, Object data) {
        goDown("Number");
        goUp();
        return null;
    }

    public Object visit(ASTValue node, Object data) {
        goDown("Value");
        if (data instanceof OutputStream && (node.jjtGetValue() != null)) {
            OutputStream output = (OutputStream)data;
            output.addDeclaredVarNames((String)node.jjtGetValue());
        }
        goUp();
        return null;
    }
}
