package soot.jtalos;

import soot.*;
import soot.jbco.util.BodyBuilder;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.Arrays;
import java.util.Map;

import static soot.SootClass.SIGNATURES;

public class EnumerateClasses {
    public static void log() {
        System.out.println("Enumerating classes/methods...");
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.myTransform", new SceneTransformer() {
                    protected void internalTransform(String phaseName,
                                                     Map options) {
                        try{
                            for (SootClass sc: Scene.v().getApplicationClasses()) {
                                for (SootMethod sm: sc.getMethods()) {
                                    System.out.println(sc.getName()+"."+sm.getName());
                                }
                            }
                        } catch (SootResolver.SootClassNotFoundException e) {
                            System.err.println(e);
                        }
                    }
                }));
    }
}