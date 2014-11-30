package edu.iastate.coms641;

import java.util.Map;

import soot.SceneTransformer;

public class AndersenTransformer extends SceneTransformer {
	
	private AndersenPointsTo pointsTo;

	public AndersenTransformer() {
		pointsTo = new AndersenPointsTo();
	}

	@Override
	protected void internalTransform(String phase, Map<String, String> opts) {
		pointsTo.build();
	}

}
