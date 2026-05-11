package hr.fer.seminar.operators;

import java.util.function.Function;

import io.jenetics.prog.op.Op;

public enum MyMathOp implements Op<Double> {

	DIV("div", 2, v -> {
		if (Math.abs(v[1]) < 10e-9) {
			return 0.;
		}
		return v[0] / v[1];
	}), POW2("pow2", 1, v -> Math.pow(v[0], 2)),
	LOG("log", 1, v -> {
		if (v[0] <= 0) {
			return 0.;
		}
		return Math.log(v[0]);
	}),
	MAX0("max0", 1, v -> Math.max(v[0], 0)),
	MIN0("min0", 1, v -> Math.min(v[0], 0)),
	
	SQR("sqr", 1, v -> {
		if (v[0] <= 0) {
			return 0.;
		}
		return Math.sqrt(v[0]);
	});

	private final String name;
	private final int arity;
	private final Function<Double[], Double> function;

	private MyMathOp(final String name, final int arity, final Function<Double[], Double> function) {
		this.name = name;
		this.function = function;
		this.arity = arity;
	}

	@Override
	public Double apply(final Double[] args) {
		return function.apply(args);
	}

	@Override
	public int arity() {
		return arity;
	}

	@Override
	public String toString() {
		return name;
	}

}
