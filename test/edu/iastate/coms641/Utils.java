package edu.iastate.coms641;

import java.util.Collections;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

public class Utils {
	public static void setupSoot(String sootClassPath, String mainClass) {
		PackManager.v().getPack("cg")
			.add(new Transform("cg.andersen", new AndersenTransformer()));
		Options.v().set_main_class(mainClass);
		Options.v().set_output_format(Options.output_format_jimple);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_app(true);
		//Options.v().set_prepend_classpath(true);
		Options.v().set_full_resolver(true);
		Options.v().set_include_all(true);
		//Options.v().set_verbose(true);
		Options.v().set_soot_classpath(sootClassPath);
		Options.v().setPhaseOption("cg.andersen", "on");
		soot.Main.v().autoSetOptions();
		
		
		SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
		c.setApplicationClass();
		SootMethod method = c.getMethodByName("main");
		Scene.v().setEntryPoints(Collections.singletonList(method));
		Scene.v().loadNecessaryClasses();
		PackManager.v().runPacks();
	}
}
