package org.warp.coordinatesobfuscator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

public class CoordinateOffsetTest {

	@Test
	void test() {
		CoordinateOffset co;
		co = CoordinateOffset.of(0, 0);
		co.validate();
		co = CoordinateOffset.of(0, 0);
		co.validate();
		for (int i = 0; i < 100_000; i++) {
			co = PlayerManager.generateOffset(null);
			co.validate();
		}
		for (int i = 0; i < 100_000; i++) {
			co = CoordinateOffset.of(getRandomChunkOffset() * 16, getRandomChunkOffset() * 16);
			co.validate();
		}
	}

	private static int getRandomChunkOffset() {
		return getMinRandomChunkOffsetBase() + getMinRandomChunkOffsetBase();
	}

	private static int getMinRandomChunkOffsetBase() {
		int number = 64 + ThreadLocalRandom.current().nextInt((int) Math.floor(496000d / 16d) - 64);
		if (ThreadLocalRandom.current().nextBoolean()) {
			number *= -1;
		}
		return number;
	}
}