package hr.fer.seminar.util.stohastic.distribution.impl;

import java.util.Random;

import hr.fer.seminar.util.stohastic.distribution.Distribution;

public class LognormalDistribution implements Distribution{
	
	private final double mean;
	
	private final double stddev;
	
	private final Random random = new Random();
	
	private final double CV;

	public LognormalDistribution(double CV) {
		this.stddev = Math.sqrt(Math.log(1 + CV * CV));
		this.mean = -(stddev * stddev) / 2;
		this.CV = CV;
	}

	@Override
	public double generate(double value) {
		double stdNormal = random.nextGaussian();
		double lambda = Math.exp(stddev * stdNormal + mean);
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
		return new LognormalDistribution(CV);
	}

}
