package me.gammadelta.vcc.common.utils;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;

import java.math.BigInteger;

public final class BinaryUtils {
    public static String hexdump(Iterable<Byte> bytes) {
        StringBuilder bob = new StringBuilder();
        StringBuilder thisLine = new StringBuilder();

        int lineIdx = 0;
        int idx = 0;
        for (Byte theByte : bytes) {
            lineIdx = idx % 16;
            if (lineIdx == 0) {
                // start of line
                String region = String.format("%08x  ", idx);
                bob.append(region);
                thisLine.append('|');
            }
            bob.append(String.format("%02x ", theByte));
            if ((theByte & 0xff) >= 0x20 && (theByte & 0xff) < 0x7f) {
                thisLine.append((char) theByte.byteValue());
            } else {
                thisLine.append('.');
            }

            if (lineIdx == 7) {
                // put spacer between 8 bytes
                bob.append(' ');
            } else if (lineIdx == 15) {
                // end of line!
                thisLine.append('|');
                bob.append(String.format("  %s\n", thisLine.toString()));
                thisLine = new StringBuilder();
            }

            idx++;
        }
        // Append the total length
        if (lineIdx != 15) {
            // Figure out how many characters we lack
            int missingChars = 3 * (15 - lineIdx);
            if (lineIdx < 7) {
                // extra space for the ' ' expected between bytes 7 and 8
                missingChars++;
            }
            for (int i = 0; i < missingChars; i++) {
                bob.append(' ');
            }
            thisLine.append('|');
            bob.append(String.format("  %s\n", thisLine.toString()));
        }
        bob.append(String.format("%1$08x  (%1$d bytes)", idx));

        return bob.toString();
    }

    public static String smolHexdump(Iterable<Byte> bytes) {
        StringBuilder bob = new StringBuilder();
        int count = 0;
        for (byte b : bytes) {
            bob.append(String.format("%02x ", b));
            count++;
        }
        // i think 8 bytes is too many to count at a glance
        if (count > 8) {
            bob.append(String.format("(%d bytes)", count));
        }


        return bob.toString();
    }

    /**
     * Turn a byte array into a long
     */
    public static long toLong(ByteList bytes) {
        long out = 0;
        for (byte b : bytes) {
            out <<= 8;
            out |= 0xff & b;
        }
        return out;
    }

    /**
     * Turn a byte array into a long
     */
    public static long toLong(byte[] bytes) {
        long out = 0;
        for (byte b : bytes) {
            out <<= 8;
            out |= 0xff & b;
        }
        return out;
    }

    /**
     * Write the long into the byte array, truncating the MSBs and filling with zeros.
     * Return 0 if there was no stretch/squish, 1 if there was stretch, and -1 if there was squish
     */
    public static int writeLong(long val, byte[] destination) {
        // gotta love java
        BigInteger imLazy = new BigInteger(Long.toString(val));
        byte[] bigBytes = imLazy.toByteArray();
        for (int i = 0; i < destination.length; i++) {
            int destIdx = destination.length - i - 1;
            int bbIdx = bigBytes.length - i - 1;
            if (bbIdx >= 0) {
                destination[destIdx] = bigBytes[bbIdx];
            } else {
                destination[destIdx] = 0;
            }
        }

        return Integer.compare(destination.length, bigBytes.length);
    }

    /**
     * Add two byte lists together, mutating `lhs`.
     * `rhs` is considered to be extended with zeroes.
     * If `rhs` is longer than `lhs`, it will only add the LSBs.
     * Returns any overflow.
     */
    public static ByteList addMut(ByteList lhs, ByteList rhs) {
        // We always want to pad with one zero to the front to avoid
        // things being treated as negative and not overflowing properly.
        BigInteger bigLhs;
        if (lhs.size() > 0) {
            byte[] bytes = new byte[lhs.size() + 1];
            lhs.getElements(0, bytes, 1, lhs.size());
            bigLhs = new BigInteger(bytes);
        } else {
            bigLhs = BigInteger.ZERO;
        }
        BigInteger bigRhs;
        if (rhs.size() > 0) {
            byte[] bytes = new byte[rhs.size() + 1];
            rhs.getElements(0, bytes, 1, rhs.size());
            bigRhs = new BigInteger(bytes);
        } else {
            bigRhs = BigInteger.ZERO;
        }
        BigInteger bigSum = bigLhs.add(bigRhs);
        byte[] sum = bigSum.toByteArray();
        for (int i = 0; i < lhs.size(); i++) {
            int lhsIdx = lhs.size() - i - 1;
            int sumIdx = sum.length - i - 1;
            if (sumIdx >= 0) {
                lhs.set(lhsIdx, sum[sum.length - i - 1]);
            } else {
                // lhs is longer than the sum.
                // check if it's negative?
                if (bigSum.compareTo(BigInteger.ZERO) >= 0) {
                    // pad with 0s
                    lhs.set(lhsIdx, (byte) 0);
                } else {
                    // pad with FFs
                    lhs.set(lhsIdx, (byte) 0xff);
                }
            }

        }
        // we will have remainder if rhs >= lhs
        if (sum.length >= lhs.size()) {
            int remainderSize = sum.length - lhs.size();
            if (remainderSize > 0) {
                return new ByteArrayList(sum, 0, remainderSize);
            } else {
                return ByteLists.EMPTY_LIST;
            }
        } else {
            // no remainder
            return ByteLists.EMPTY_LIST;
        }
    }

    /**
     * Add two byte arrays together. Returns the (overflow, sum).
     */
    public static Pair<ByteList, ByteList> add(byte[] lhs, byte[] rhs) {
        ByteList lhsList = new ByteArrayList(lhs);
        ByteList rem = addMut(lhsList, new ByteArrayList(rhs));
        return new Pair<>(rem, lhsList);
    }

    /**
     * Perform lhs -= rhs. Return any underflow (aka, how much rhs was left over after subtracting).
     */
    public static ByteList subMut(ByteList lhs, ByteList rhs) {
        // We always want to pad with one zero to the front to avoid
        // things being treated as negative and not overflowing properly.
        BigInteger bigLhs;
        if (lhs.size() > 0) {
            byte[] bytes = new byte[lhs.size() + 1];
            lhs.getElements(0, bytes, 1, lhs.size());
            bigLhs = new BigInteger(bytes);
        } else {
            bigLhs = BigInteger.ZERO;
        }
        BigInteger bigRhs;
        if (rhs.size() > 0) {
            byte[] bytes = new byte[rhs.size() + 1];
            rhs.getElements(0, bytes, 1, rhs.size());
            bigRhs = new BigInteger(bytes);
        } else {
            bigRhs = BigInteger.ZERO;
        }

        BigInteger bigDifference = bigLhs.subtract(bigRhs);
        byte[] diff = bigDifference.toByteArray();

        for (int i = 0; i < lhs.size(); i++) {
            int lhsIdx = lhs.size() - i - 1;
            int diffIdx = diff.length - i - 1;
            if (diffIdx >= 0) {
                lhs.set(lhsIdx, diff[diff.length - i - 1]);
            } else {
                // lhs is longer than the sum.
                // check if it's negative?
                if (bigDifference.compareTo(BigInteger.ZERO) >= 0) {
                    // pad with 0s
                    lhs.set(lhsIdx, (byte) 0);
                } else {
                    // pad with FFs
                    lhs.set(lhsIdx, (byte) 0xff);
                }
            }

        }
        // we will have remainder if rhs >= lhs
        if (diff.length >= lhs.size()) {
            int remainderSize = diff.length - lhs.size();
            if (remainderSize > 0) {
                return new ByteArrayList(diff, 0, remainderSize);
            } else {
                return ByteLists.EMPTY_LIST;
            }
        } else {
            // no remainder
            return ByteLists.EMPTY_LIST;
        }
    }

    /**
     * Perform lhs - rhs. Return the (underflow, sum)
     */
    public static Pair<ByteList, ByteList> sub(byte[] lhs, byte[] rhs) {
        ByteList lhsList = new ByteArrayList(lhs);
        ByteList rem = subMut(lhsList, new ByteArrayList(rhs));
        return new Pair<>(rem, lhsList);
    }
}
