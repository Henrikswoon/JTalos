package soot.jtalos;

import soot.*;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StringConstant;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SWRRInject {
    static Jimple jimple = Jimple.v();
    static Unit assignStmt;
    static Unit invokeStmt;
    static Local r1;
    static Unit throwStmt;
    public static void main(String[] args){
        System.out.println("Entered JTalos...");
        String[] SWRR_OPTIONS;
        try (FileReader fr = new FileReader("SWRR_OPTIONS");
             BufferedReader br = new BufferedReader(fr)){

            List<String> lines = br.lines().toList();
            SWRR_OPTIONS = lines.toArray(new String[0]);

            int i = 0;
            String option = br.readLine();
            while(option != null){
                SWRR_OPTIONS[i] = option;
                option = br.readLine();
                i++;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("SWRR_OPTIONS not found, if this is missing please create one in the jtalos root directory...");
        } catch (IOException e) {
            throw new RuntimeException("Unable to access SWRR_OPTIONS, see error:\n" + e);
        }
        System.out.println("Options read into memory...");

        for (int j = 0; j < SWRR_OPTIONS.length; j++)
            System.out.println("\t " + SWRR_OPTIONS[j]);

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
                                    String uid = sc.getName() + "." + sm.getName();
                                    if(Arrays.asList(SWRR_OPTIONS).contains(uid)){
                                        System.out.println("Instrumenting " + uid + " ...");
                                        b.getLocals().addFirst(r1);
                                        PatchingChain<Unit> units = b.getUnits();
                                        Unit head = units.getFirst();
                                        units.insertBefore(assignStmt, head);
                                        units.insertAfter(invokeStmt, assignStmt);
                                        units.insertAfter(throwStmt, invokeStmt);
                                    }
                                    System.out.println("Jimple body: "+ b);
                                } catch (RuntimeException e){
                                    System.err.println(e);
                                }
                            }
                        }
                    }
                }));

        System.out.println("Finished adding Pack:jtalos");
        Main.main(args);
    }
}
