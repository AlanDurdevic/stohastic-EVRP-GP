package hr.fer.seminar.util.stohastic.distribution.impl;

import hr.fer.seminar.util.stohastic.distribution.Distribution;

public class NoDistribution implements Distribution{

	@Override
	public double generate(double value) {
		return value;
	}

	@Override
	public double getCV() {
		return 0;
	}

	@Override
	public void setSeed(long seed) {
		
	}

	@Override
	public Distribution copy() {
		return new NoDistribution();
	}

}
