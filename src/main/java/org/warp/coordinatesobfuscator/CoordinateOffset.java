package org.warp.coordinatesobfuscator;

import java.util.Arrays;

public final class CoordinateOffset {
	private final double x;
	private final double z;

	private CoordinateOffset(double x, double z) {
		this.x = x;
		this.z = z;
	}

	public static CoordinateOffset of(double x, double z) {
		return new CoordinateOffset(x, z);
	}

	public int getXInt() {
		return (int) x;
	}

	public int getZInt() {
		return (int) z;
	}

	public int getXChunk() {
		return ((int) x) / 16;
	}

	public int getZChunk() {
		return ((int) z) / 16;
	}

	public void validate() {
		if ((int) (Math.round(x / 16f) * 16) != x) {
			throw new IllegalArgumentException("x is not aligned with the chunks!");
		}
		if ((int) (Math.round(z / 16f) * 16) != z) {
			throw new IllegalArgumentException("z is not aligned with the chunks!");
		}
		var reconvertedX = (double) ((int) x);
		if (reconvertedX != x) {
			throw new IllegalArgumentException("x is not safe to convert between double and int");
		}
		var reconvertedZ = (double) ((int) z);
		if (reconvertedZ != z) {
			throw new IllegalArgumentException("z is not safe to convert between double and int");
		}
	}

	public double[] convertToOriginal(double[] maskedCoordinates) {
		if (maskedCoordinates.length == 3) {
			double[] originalCoordinates = Arrays.copyOf(maskedCoordinates, maskedCoordinates.length);
			originalCoordinates[0] = maskedCoordinates[0] - getX();
			originalCoordinates[2] = maskedCoordinates[2] - getZ();
			return originalCoordinates;
		} else if (maskedCoordinates.length == 2) {
			double[] originalCoordinates = Arrays.copyOf(maskedCoordinates, maskedCoordinates.length);
			originalCoordinates[0] = maskedCoordinates[0] - getX();
			originalCoordinates[1] = maskedCoordinates[1] - getZ();
			return originalCoordinates;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public int[] convertToOriginal(int[] maskedCoordinates) {
		if (maskedCoordinates.length == 3) {
			int[] originalCoordinates = Arrays.copyOf(maskedCoordinates, maskedCoordinates.length);
			originalCoordinates[0] = maskedCoordinates[0] - (int) getX();
			originalCoordinates[2] = maskedCoordinates[2] - (int) getZ();
			return originalCoordinates;
		} else if (maskedCoordinates.length == 2) {
			int[] originalCoordinates = Arrays.copyOf(maskedCoordinates, maskedCoordinates.length);
			originalCoordinates[0] = maskedCoordinates[0] - (int) getX();
			originalCoordinates[1] = maskedCoordinates[1] - (int) getZ();
			return originalCoordinates;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public int[] convertChunkToOriginal(int[] maskedCoordinates) {
		if (maskedCoordinates.length != 2) {
			throw new IllegalArgumentException();
		}
		int[] originalCoordinates = Arrays.copyOf(maskedCoordinates, maskedCoordinates.length);
		originalCoordinates[0] = maskedCoordinates[0] - ((int) getX()) / 16;
		originalCoordinates[1] = maskedCoordinates[1] - ((int) getZ()) / 16;
		return originalCoordinates;
	}

	public double[] convertToMasked(double[] originalCoordinates) {
		if (originalCoordinates.length == 3) {
			double[] maskedCoordinates = Arrays.copyOf(originalCoordinates, originalCoordinates.length);
			maskedCoordinates[0] = originalCoordinates[0] + getX();
			maskedCoordinates[2] = originalCoordinates[2] + getZ();
			return maskedCoordinates;
		} else if (originalCoordinates.length == 2) {
			double[] maskedCoordinates = Arrays.copyOf(originalCoordinates, originalCoordinates.length);
			maskedCoordinates[0] = originalCoordinates[0] + getX();
			maskedCoordinates[1] = originalCoordinates[1] + getZ();
			return maskedCoordinates;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public int[] convertToMasked(int[] originalCoordinates) {
		if (originalCoordinates.length == 3) {
			int[] maskedCoordinates = Arrays.copyOf(originalCoordinates, originalCoordinates.length);
			maskedCoordinates[0] = originalCoordinates[0] + (int) getX();
			maskedCoordinates[2] = originalCoordinates[2] + (int) getZ();
			return maskedCoordinates;
		} else if (originalCoordinates.length == 2) {
			int[] maskedCoordinates = Arrays.copyOf(originalCoordinates, originalCoordinates.length);
			maskedCoordinates[0] = originalCoordinates[0] + (int) getX();
			maskedCoordinates[1] = originalCoordinates[1] + (int) getZ();
			return maskedCoordinates;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public int[] convertChunkToMasked(int[] originalCoordinates) {
		if (originalCoordinates.length != 2) {
			throw new IllegalArgumentException();
		}
		int[] maskedCoordinates = Arrays.copyOf(originalCoordinates, originalCoordinates.length);
		maskedCoordinates[0] = originalCoordinates[0] + ((int) getX()) / 16;
		maskedCoordinates[1] = originalCoordinates[1] + ((int) getZ()) / 16;
		return maskedCoordinates;
	}

	public double getX() {
		return this.x;
	}

	public double getZ() {
		return this.z;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof CoordinateOffset)) {
			return false;
		}
		final CoordinateOffset other = (CoordinateOffset) o;
		if (Double.compare(this.getX(), other.getX()) != 0) {
			return false;
		}
		if (Double.compare(this.getZ(), other.getZ()) != 0) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final long $x = Double.doubleToLongBits(this.getX());
		result = result * PRIME + (int) ($x >>> 32 ^ $x);
		final long $z = Double.doubleToLongBits(this.getZ());
		result = result * PRIME + (int) ($z >>> 32 ^ $z);
		return result;
	}

	public String toString() {
		return "CoordinateOffset(x=" + this.getX() + ", z=" + this.getZ() + ")";
	}
}
