/* TODO: license */
package org.github.gentlewake.util;

import java.util.Random;

/**
 * @author lorenz.fischer@gmail.com
 */
public final class Utils {

    /** Utility classes should never be instantiated. */
    private Utils(){}

    /**
     * Generates a random key with the given length.
     * @param length the length of the key, the longer the smaller the chance of having duplicates.
     * @return the generated key.
     */
    public static String randomKey(int length) {
        char[] chars = "1234567890abcdefghijklmnopqrstuvwxyz".toCharArray();
        Random rand = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < length; i++) {
            key.append(chars[rand.nextInt(chars.length)]);
        }
        return key.toString();
    }

}
