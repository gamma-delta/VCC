import me.gammadelta.common.program.compilation.ASMCompiler;
import me.gammadelta.common.program.compilation.BytecodeWriter;
import me.gammadelta.common.program.compilation.Instruction;
import me.gammadelta.common.program.compilation.Token;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class BytecodeWritingTest {
    @Test
    public void testLiteralSerialization() throws Exception {
        for (int test = 0; test < Integer.MAX_VALUE; test++) {
            String decStr = String.valueOf(test);
            String hexStr = "0x" + Integer.toString(test, 16);
            String binStr = "0b" + Integer.toString(test, 2);

            Instruction.Arg decArg = new Instruction.Arg(new Token(Token.Type.DECIMAL, decStr, -1, -1), Instruction.Arg.Type.IV);
            Instruction.Arg hexArg = new Instruction.Arg(new Token(Token.Type.HEXADECIMAL, hexStr, -1, -1), Instruction.Arg.Type.IV);
            Instruction.Arg binArg = new Instruction.Arg(new Token(Token.Type.BINARY, binStr, -1, -1), Instruction.Arg.Type.IV);

            byte[] decBytes = BytecodeWriter.writeLiteral(decArg);
            byte[] hexBytes = BytecodeWriter.writeLiteral(hexArg);
            byte[] binBytes = BytecodeWriter.writeLiteral(binArg);

            assert Arrays.equals(decBytes, hexBytes) : String.format("%d became %s vs %s", test, Arrays.toString(decBytes), Arrays.toString(hexBytes));
            assert Arrays.equals(hexBytes, binBytes) : String.format("%d became %s vs %s", test, Arrays.toString(hexBytes), Arrays.toString(binBytes));
        }
    }
}
