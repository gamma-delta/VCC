import me.gammadelta.common.program.compilation.ASMCompiler;
import me.gammadelta.common.program.compilation.CodeCompileException;
import me.gammadelta.common.program.compilation.Instruction;
import org.junit.jupiter.api.Test;

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
    public void parseHammingCode() throws CodeCompileException {

    }
}
