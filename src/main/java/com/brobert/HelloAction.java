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
        PsiType builderType = addBuilderClass(project, currentClass);
        addBuilderFactory(project, currentClass, builderType);

/*
        for (StepBuilderTask task : tasks) {
            PsiMethod method = JavaPsiFacade.getElementFactory(project).createMethod("hello", PsiType.BOOLEAN);
            PsiClass anon = JavaPsiFacade.getElementFactory(project).createClass(task.anonClassName);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                anon.add(method);
                currentClass.add(anon);
            });
        }
*/

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



    private PsiType addBuilderClass(Project project, PsiClass currentClass) {
        PsiClass builderClass = javaElFactory.createClass("Builder");
        PsiType type = javaElFactory.createType(currentClass);
        PsiField field = javaElFactory.createField("instance", type);
        field.getModifierList().setModifierProperty("private", true);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            builderClass.add(field);
            currentClass.add(builderClass);
        });
        return type;
    }



    private void addBuilderFactory(Project project, PsiClass currentClass, PsiType builderType) {
        PsiMethod method = javaElFactory.createMethod("getBuilder", builderType);
        method.getModifierList().setModifierProperty("public", true);
        method.getModifierList().setModifierProperty("static", true);

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