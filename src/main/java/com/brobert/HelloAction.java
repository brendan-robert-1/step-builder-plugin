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
        addPrivateConstructorIfNeccesary(project, currentClass);
        PsiType builderType = addBuilderClassIfNecessary(project, currentClass);
        addBuilderFactoryIfNecessary(project, currentClass, builderType);
    }



    /**
     * Checks whether or not a private constructor exists and adds it if so.
     *
     * @param project
     * @param currentClass
     */
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



    /**
     * Adds a private constructor to a class
     *
     * @param project
     * @param currentClass
     */
    private void addPrivateConstructor(Project project, PsiClass currentClass) {
        PsiMethod method = javaElFactory.createConstructor();
        method.getModifierList().setModifierProperty("private", true);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            currentClass.add(method);
        });
    }



    /**
     * Adds the entire builder class if it does not already exist.
     *
     * @param project
     * @param currentClass
     * @return
     */
    private PsiType addBuilderClassIfNecessary(Project project, PsiClass currentClass) {
        PsiClass builderClass = javaElFactory.createClass("Builder");
        builderClass.getModifierList().setModifierProperty("static", true);
        PsiType builderType = javaElFactory.createType(builderClass);
        PsiType mainClassType = javaElFactory.createType(currentClass);
        PsiField field = getInstanceField(mainClassType);
        builderClass.add(field);

        addPrivateConstructorIfNeccesary(project, builderClass);

        List<PsiField> existingFields = getAllFields(currentClass);
        List<PsiClass> stepClasses = getSteps(existingFields, builderType, javaElFactory.createType(currentClass));
        for (PsiClass psiClass : stepClasses) {
            builderClass.add(psiClass);
        }
        boolean builderClassExists = builderClassExists(currentClass);
        if (!builderClassExists) {

            WriteCommandAction.runWriteCommandAction(project, () -> {
                currentClass.add(builderClass);
            });
        }
        return builderType;
    }



    /**
     * Checks whether or not there is already a public static builder class.
     *
     * @param currentClass
     * @return
     */
    private boolean builderClassExists(PsiClass currentClass) {
        boolean builderClassExists = false;
        PsiClass[] subClasses = currentClass.getInnerClasses();
        for (PsiClass subClass : subClasses) {
            boolean isPublic = subClass.getModifierList().hasModifierProperty("public");
            boolean isBuilder = subClass.getName().equals("Builder");
            boolean isStatic = subClass.getModifierList().hasModifierProperty("static");
            builderClassExists = isPublic && isBuilder && isStatic;
            if (builderClassExists == true) {
                break;
            }
        }
        return builderClassExists;
    }



    /**
     * Builds an private static instance field for a given type. Initializes it with an empty constructor as well.
     *
     * @param type
     * @return
     */
    private PsiField getInstanceField(PsiType type) {
        PsiField field = javaElFactory.createField("instance", type);
        field.getModifierList().setModifierProperty("private", true);
        field.getModifierList().setModifierProperty("static", true);
        PsiExpression expression = javaElFactory.createExpressionFromText("new " + type.getCanonicalText() + "()", field);
        field.setInitializer(expression);
        return field;
    }



    /**
     * Builds a list of the inner classes to builder. These classes represent a 'step' in the builder.
     * @param existingFields
     * @param builderType
     * @param mainClassType
     * @return
     */
    private List<PsiClass> getSteps(List<PsiField> existingFields, PsiType builderType, PsiType mainClassType) {
        List<PsiClass> steps = new ArrayList<>();
        int cur = 1;
        for (PsiField field : existingFields) {
            String stepClassName = StringUtils.getPascalCase(field.getName());

            PsiClass stepClass = javaElFactory.createClass(stepClassName);
            stepClass.getModifierList().setModifierProperty("public", true);
            PsiField builder = javaElFactory.createField("b", builderType);
            builder.getModifierList().setModifierProperty("private", true);
            stepClass.add(builder);

            if (cur++ == existingFields.size()) {
                PsiMethod build = javaElFactory.createMethod("build", mainClassType);
                PsiStatement psiStatement = javaElFactory.createStatementFromText("return instance;", build);
                build.getBody().add(psiStatement);
                stepClass.add(build);
            } else {
                PsiMethod stepMethod = createStepMethod(field);
                stepClass.add(stepMethod);
            }
            steps.add(stepClass);
        }
        return steps;
    }



    private PsiMethod createStepMethod(PsiField field) {
        String methodName = StringUtils.getCamelCase(field.getName());
        PsiMethod method = javaElFactory.createMethod(methodName, field.getType());
        method.getModifierList().setModifierProperty("public", true);
        return method;
    }



    private void addBuilderFactoryIfNecessary(Project project, PsiClass currentClass, PsiType builderType) {
        PsiMethod method = javaElFactory.createMethod("getBuilder", builderType);
        method.getModifierList().setModifierProperty("public", true);

        PsiStatement statement = javaElFactory.createStatementFromText("return new Builder();", method);
        boolean builderFactoryExists = builderFactoryExists(currentClass);
        if (!builderFactoryExists) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                method.getBody().add(statement);
                currentClass.add(method);
            });
        }
    }



    private boolean builderFactoryExists(PsiClass currentClass) {
        boolean builderFactoryExists = false;
        for (PsiMethod method : currentClass.getAllMethods()) {
            boolean isPublic = method.getModifierList().hasModifierProperty("public");
            boolean isGetBuilder = method.getName().equals("getBuilder");
            builderFactoryExists = isPublic && isGetBuilder;
            if (builderFactoryExists) {
                break;
            }
        }
        return builderFactoryExists;
    }



    private List<PsiField> getAllFields(PsiClass currentClass) {
        return Arrays.asList(currentClass.getAllFields());
    }

}