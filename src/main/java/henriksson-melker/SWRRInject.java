package soot.jtalos;

import soot.*;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;

import java.util.Collections;
import java.util.Map;

public class SWRRInject {
    static Jimple jimple = Jimple.v();
    static Unit assignStmt;
    static Unit invokeStmt;
    static Local r1;
    static Unit throwStmt;
    public static void main(String[] args){
        System.out.println("Entered JTalos...");
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.JTalos", new SceneTransformer() {
                    protected void internalTransform(String phaseName,
                                                     Map options) {
                        System.out.println("Entered JTalos internalTransform...");
                        try{
                            String className = "java.lang.IllegalStateException";
                            SootClass c = Scene.v().loadClassAndSupport(className);
                            RefType refException = c.getType();
                            r1 = jimple.newLocal("$r1", refException);
                            NewExpr newExpr = Jimple.v().newNewExpr(refException);
                            assignStmt = jimple.newAssignStmt(r1, newExpr);
                            SootMethod constructor = c.getMethod("<init>", Collections.emptyList());
                            SpecialInvokeExpr invokeExpr = jimple.newSpecialInvokeExpr(r1, constructor.makeRef());
                            invokeStmt = jimple.newInvokeStmt(invokeExpr);
                            throwStmt = jimple.newThrowStmt(r1);
                        } catch(ExceptionInInitializerError e){
                            e.printStackTrace();
                            throw e;
                        }
                        System.out.println("Finished initializing instructions..." +
                                "\n\tassignStmt: " + assignStmt.toString() +
                                "\n\tinvokeStmt: " + invokeStmt.toString() +
                                "\n\tthrowStmt: "  + throwStmt.toString()
                        );
                        for (SootClass sc: Scene.v().getApplicationClasses()) {
                            System.out.println(sc);
                            for (SootMethod sm: sc.getMethods()) {
                                System.out.println("  "+ sm);
                                try{
                                    Body b = sm.retrieveActiveBody();
                                    b.getLocals().addFirst(r1);
                                    PatchingChain<Unit> units = b.getUnits();
                                    Unit head = units.getFirst();
                                    units.insertBefore(head, assignStmt);
                                    units.insertAfter(assignStmt, invokeStmt);
                                    units.insertAfter(invokeStmt, throwStmt);
                                    System.out.println("Jimple body: "+ b);
                                } catch (RuntimeException e){
                                    System.err.println(e);
                                }
                            }
                        }
                    }

                }));

        System.out.println("Finished adding Pack:jtalos");
        soot.Main.main(args);
    }
}
