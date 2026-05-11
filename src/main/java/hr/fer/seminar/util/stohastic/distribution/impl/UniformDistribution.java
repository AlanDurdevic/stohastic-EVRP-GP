package hr.fer.seminar.util.stohastic.distribution.impl;

import java.util.Random;

import hr.fer.seminar.util.stohastic.distribution.Distribution;

public class UniformDistribution implements Distribution{
	
	private final double CV;
	
	private final Random random = new Random();

	public UniformDistribution(double CV) {
		this.CV = CV;
	}

	@Override
	public double generate(double value) {
		double lambda = random.nextDouble(1 - CV, 1 + CV);
		return lambda * value;
	}

	@Override
	public double getCV() {
		return CV;
	}

	@Override
	public void setSeed(long seed) {
		random.setSeed(seed);
	}

	@Override
	public Distribution copy() {
		return new UniformDistribution(CV);
	}

}
