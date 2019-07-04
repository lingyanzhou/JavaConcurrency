package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ParallelStreamTests {
    @Test
    public void testArraysParallelMethods() {
        final int ARRAY_SIZE = 100;
        int[] data = new int[ARRAY_SIZE];
        // Set data[i] = ARRAY_SIZE-i-1
        Arrays.parallelSetAll(data, (idx) -> ARRAY_SIZE-idx-1);
        for (int i=0; i<ARRAY_SIZE; ++i) {
            Assert.assertEquals(ARRAY_SIZE-i-1, data[i]);
        }

        // Sort data
        Arrays.parallelSort(data);
        for (int i=0; i<ARRAY_SIZE; ++i) {
            Assert.assertEquals(i, data[i]);
        }

        // Cumulation
        // Calculate integral array
        Arrays.parallelPrefix(data, (a, b) -> (a+b));

        int integral = 0;
        for (int i=0; i<ARRAY_SIZE; ++i) {
            integral += i;
            Assert.assertEquals(integral, data[i]);
        }
    }
}
