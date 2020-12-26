import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import me.gammadelta.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class ByteArrayAdditionTest {
    @Test
    public void testNoCarry() {
        ByteList l1 = ByteArrayList.wrap(new byte[]{0x12, 0x34});
        ByteList l2 = ByteArrayList.wrap(new byte[]{0x56, 0x78});
        ByteList _remainder = Utils.addMut(l1, l2);

        Assertions.assertEquals(ByteArrayList.wrap(new byte[]{0x68, (byte) 0xac}), l1);
    }

    @Test
    public void testCarry() {
        ByteList l1 = ByteArrayList.wrap(new byte[]{(byte) 0xf0});
        ByteList l2 = ByteArrayList.wrap(new byte[]{(byte) 0x20});
        ByteList remainder = Utils.addMut(l1, l2);

        Assertions.assertEquals(ByteLists.singleton((byte) 0x10), l1);
        Assertions.assertEquals(ByteLists.singleton((byte) 0x01), remainder);
    }

    @Test
    public void testProperties() {
        // yknow when i learned about property based testing, they used addition as a super-basic example.
        // never thought I would actually write a pbt for addition...

        // random but consistent seed
        Random rand = new Random(123456);
        int LENGTH = 20;

        for (int testIdx = 0; testIdx < 1000; testIdx++) {
            // Check commutativity: a + b == b + a
            {
                // these are the same length because if they are different lengths
                // addition is NOT commutative.
                int length = rand.nextInt(LENGTH);
                byte[] arr1 = new byte[length];
                byte[] arr2 = new byte[length];
                rand.nextBytes(arr1);
                rand.nextBytes(arr2);

                ByteList lhs1 = new ByteArrayList(arr1);
                ByteList rhs1 = new ByteArrayList(arr2);
                ByteList rem1 = Utils.addMut(lhs1, rhs1);

                ByteList lhs2 = new ByteArrayList(arr2);
                ByteList rhs2 = new ByteArrayList(arr1);
                ByteList rem2 = Utils.addMut(lhs2, rhs2);

                Assertions.assertEquals(lhs1, lhs2);
                Assertions.assertEquals(rem1, rem2);
            }

            // We can't do associative or distributive with limited precision ;-;

            // Identity property
            {
                byte[] arr1 = new byte[rand.nextInt(LENGTH)];
                rand.nextBytes(arr1);

                ByteList original = new ByteArrayList(arr1);

                ByteList test = new ByteArrayList(arr1);
                byte[] zeroArray = new byte[rand.nextInt(LENGTH)];
                ByteList zeroList = new ByteArrayList(zeroArray);

                Utils.addMut(test, zeroList);
                Assertions.assertEquals(original, test);
            }
        }
    }

    @Test
    public void testBigCarry() {
        // 0xfffefdfcfbfaf9f8
        ByteList lhs = new ByteArrayList(new byte[]{0, -1, -2, -3, -4, -5, -6, -7, -8});
        ByteList rhs = new ByteArrayList(new byte[]{0, -8, -7, -6, -5, -4, -3, -2, -1});
        Utils.addMut(lhs, rhs);
        // looking for 01_f8_f8_f8_f8_f8_f8_f8_f7
        Assertions.assertEquals(
                new ByteArrayList(new byte[]{1, (byte) 0xf8, (byte) 0xf8, (byte) 0xf8, (byte) 0xf8, (byte) 0xf8, (byte) 0xf8, (byte) 0xf8, (byte) 0xf7}), lhs);
    }

    @Test
    public void testOverflow() {
        // 0x7f7f
        ByteList lhs = new ByteArrayList(new byte[]{-1, -1});
        ByteList rhs = new ByteArrayList(new byte[]{2});
        ByteList remainder = Utils.addMut(lhs, rhs);
        Assertions.assertEquals(ByteLists.singleton((byte) 1), remainder);
        Assertions.assertEquals(new ByteArrayList(new byte[]{0, 1}), lhs);
    }
}
