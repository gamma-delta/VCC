import me.gammadelta.vcc.common.program.compilation.ASMCompiler;
import me.gammadelta.vcc.common.program.compilation.CodeCompileException;
import me.gammadelta.vcc.common.program.compilation.Instruction;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ParsingASMTest  {
    // Note to anyone who wants to run these tests:
    // you have to set the test runner to be intellij
    // https://stackoverflow.com/questions/60228404/no-tests-found-for-given-includes-when-running-gradle-tests-in-intellij-idea
    @Test
    public void prelimTestParsing() throws CodeCompileException {
        String test = "MOV 5 R0\n"
        + "; Comment wow lol lmao look ma no hands\n"
        + "ADD R2 0x4141\n"
        + "SUB R2 @2'R1\n"
        + "COPY 5 D1 0x02F3";


        List<Instruction> instructs = ASMCompiler.lexAndParse(test);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void directives() throws CodeCompileException {
        String test = "MYLABEL:\n#def MY_CONST 0x42\nMOV MY_CONST R0";
        List<Instruction> instructs = ASMCompiler.lexAndParse(test);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void labels() throws CodeCompileException {
        String test = "JMP MAIN\n"
        + "LABEL1:\n"
        + "NOP\n"
        + "B2BLabel1:\nB2BLabel2:\n"
        + "JMP LABEL1\n"
        + "MAIN: JMP B2BLabel1";
        List<Instruction> instructs = ASMCompiler.lexAndParse(test);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void stackvalues() throws CodeCompileException {
        String test = "#def DEFINED 4. MOV @4'DEFINED NIL. MOV @4'2 NIL. MOV @4'R0 NIL. MOV @4'UNDEFINED NIL.";
        List<Instruction> instructs = ASMCompiler.lexAndParse(test);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void undefinedDefines() throws CodeCompileException {
        List<Instruction> instructs = ASMCompiler.lexAndParse("MOV I_NEVER_DEFINED_THIS NIL");
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void parseHammingCode() throws Exception {
        URL path = ClassLoader.getSystemResource("HammingCode.vcc");
        String text = String.join("\n", Files.readAllLines(Paths.get(path.toURI())));
        List<Instruction> instructs = ASMCompiler.lexAndParse(text);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }

    @Test
    public void runWhateverIsInTheScratchPadBecauseImTiredOfMakingNewTestsForEverything() throws Exception {
        URL path = ClassLoader.getSystemResource("Scratchpad.vcc");
        String text = String.join("\n", Files.readAllLines(Paths.get(path.toURI())));

        List<Instruction> instructs = ASMCompiler.lexAndParse(text);
        String pretty = ASMCompiler.prettyPrintInstructions(instructs);
        System.out.println(pretty);
    }
}
