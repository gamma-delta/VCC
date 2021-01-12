import me.gammadelta.common.program.compilation.*;
import me.gammadelta.common.utils.BinaryUtils;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BytecodeWritingTest {
    @Test
    public void testPrelim() throws Exception {
        compileAndDump("ADD R0 10. NOP. NOP. NOP. NOP. SUB R0 R1. JLZ R0 3277");
    }

    @Test
    public void compileHammingCode() throws Exception {
        URL path = ClassLoader.getSystemResource("HammingCode.vcc");
        String program = String.join("\n", Files.readAllLines(Paths.get(path.toURI())));
        compileAndDump(program);
    }

    @Test
    public void compileWhateverIsInTheScratchPadBecauseImTiredOfMakingNewTestsForEverything() throws Exception {
        URL path = ClassLoader.getSystemResource("Scratchpad.vcc");
        String program = String.join("\n", Files.readAllLines(Paths.get(path.toURI())));
        compileAndDump(program);
    }

    private static void compileAndDump(String program) throws Exception {
        List<Token> tokens = new CodeLexer(new String[]{program}).slurp();
        for (Token token : tokens) {
            System.out.println(token);
        }
        List<Instruction> instructions = new CodeParser().parseInstructions(tokens);
        List<Byte> bytecode = new BytecodeWriter(instructions).writeProgramToBytecode();
        System.out.printf("=== Program ===\n%s\n\n"
                + "=== Intermediate ===\n%s\n\n"
                + "=== Bytecode ===\n%s\n",
                program, ASMCompiler.prettyPrintInstructions(instructions), BinaryUtils.hexdump(bytecode));
    }
}
