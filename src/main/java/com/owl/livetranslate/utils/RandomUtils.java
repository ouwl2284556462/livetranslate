package com.owl.livetranslate.utils;

import java.util.Random;

public class RandomUtils {
    private static final Random random = new Random();

    public static int nextInt(int bound){
        return random.nextInt(bound);
    }
}
