package com.brobert;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelloAction extends AnAction {

    public HelloAction() {
        super("Hello");
    }



    private PsiElementFactory javaElFactory;



    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        javaElFactory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile psiFile = (PsiJavaFile) event.getData(CommonDataKeys.PSI_FILE);
        PsiClass currentClass = psiFile.getClasses()[0];
        List<StepBuilderTask> tasks = getTasks(currentClass);
        addPrivateConstructorIfNeccesary(project, currentClass);
        PsiType builderType = addBuilderClassIfNecessary(project, currentClass);
        addBuilderFactoryIfNecessary(project, currentClass, builderType);
    }



    private void addPrivateConstructorIfNeccesary(Project project, PsiClass currentClass) {
        PsiMethod[] constructors = currentClass.getConstructors();
        for (PsiMethod constructor : constructors) {
            PsiModifierList modifiers = constructor.getModifierList();
            boolean isPrivate = modifiers.hasExplicitModifier("private");
            if (!isPrivate) {
                addPrivateConstructor(project, currentClass);
            }
        }
        if (constructors.length == 0) {
            addPrivateConstructor(project, currentClass);
        }
    }



    private void addPrivateConstructor(Project project, PsiClass currentClass) {
        PsiMethod method = javaElFactory.createConstructor();
        method.getModifierList().setModifierProperty("private", true);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            currentClass.add(method);
        });
    }



    private PsiType addBuilderClassIfNecessary(Project project, PsiClass currentClass) {
        PsiClass builderClass = javaElFactory.createClass("Builder");
        builderClass.getModifierList().setModifierProperty("static", true);
        PsiType type = javaElFactory.createType(builderClass);
        PsiField field = javaElFactory.createField("instance", type);
        field.getModifierList().setModifierProperty("private", true);
        field.getModifierList().setModifierProperty("static", true);
        boolean builderClassExists = builderClassExists(currentClass);
        if(!builderClassExists){
            WriteCommandAction.runWriteCommandAction(project, () -> {
                builderClass.add(field);
                currentClass.add(builderClass);
            });
        }
        return type;
    }



    private boolean builderClassExists(PsiClass currentClass) {
        boolean builderClassExists = false;
        PsiClass[] subClasses = currentClass.getInnerClasses();
        for (PsiClass subClass : subClasses) {
            boolean isPublic = subClass.getModifierList().hasModifierProperty("public");
            boolean isBuilder = subClass.getName().equals("Builder");
            boolean isStatic = subClass.getModifierList().hasModifierProperty("static");
            builderClassExists = isPublic && isBuilder && isStatic;
            if(builderClassExists == true){
                break;
            }
        }
        return builderClassExists;
    }



    private void addBuilderFactoryIfNecessary(Project project, PsiClass currentClass, PsiType builderType) {
        PsiMethod method = javaElFactory.createMethod("getBuilder", builderType);
        method.getModifierList().setModifierProperty("public", true);

        PsiStatement statement = javaElFactory.createStatementFromText("return new Builder();", method);
        boolean builderFactoryExists = builderFactoryExists(currentClass);
        if(!builderFactoryExists){
            WriteCommandAction.runWriteCommandAction(project, () -> {
                method.getBody().add(statement);
                currentClass.add(method);
            });
        }
    }



    private boolean builderFactoryExists(PsiClass currentClass) {
        boolean builderFactoryExists = false;
        for(PsiMethod method : currentClass.getAllMethods()){
            boolean isPublic = method.getModifierList().hasModifierProperty("public");
            boolean isGetBuilder = method.getName().equals("getBuilder");
            builderFactoryExists = isPublic && isGetBuilder;
            if(builderFactoryExists){
                break;
            }
        }
        return builderFactoryExists;
    }



    private List<StepBuilderTask> getTasks(PsiClass currentClass) {
        List<StepBuilderTask> tasks = new ArrayList<>();
        List<PsiField> allFields = getAllFields(currentClass);
        for (PsiField field : allFields) {
            StepBuilderTask task = new StepBuilderTask();
            task.anonClassName = field.getName();
            tasks.add(task);
        }
        return tasks;
    }



    private List<PsiField> getAllFields(PsiClass currentClass) {
        return Arrays.asList(currentClass.getAllFields());
    }

}