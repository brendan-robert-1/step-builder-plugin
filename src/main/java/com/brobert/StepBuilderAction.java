package com.brobert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.LinkedList;

public class StepBuilderAction extends AnAction {

    public StepBuilderAction() {
        super("Hello");
    }



    private PsiElementFactory javaElFactory;
    private Project project;



    public void actionPerformed(AnActionEvent event) {
        project = event.getProject();
        javaElFactory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile psiFile = (PsiJavaFile) event.getData(CommonDataKeys.PSI_FILE);
        PsiClass currentClass = psiFile.getClasses()[0];
        addPrivateConstructorIfNecessary(currentClass);
        PsiType builderType = addBuilderClassIfNecessary(currentClass);
        addBuilderFactoryIfNecessary(currentClass, builderType);
    }



    /**
     * Checks whether or not a private constructor exists and adds it if so.
     *
     * @param currentClass
     */
    private void addPrivateConstructorIfNecessary(PsiClass currentClass) {
        PsiMethod[] constructors = currentClass.getConstructors();
        for (PsiMethod constructor : constructors) {
            PsiModifierList modifiers = constructor.getModifierList();
            boolean isPrivate = modifiers.hasExplicitModifier("private");
            if (!isPrivate) {
                addPrivateConstructor(currentClass);
            }
        }
        if (constructors.length == 0) {
            addPrivateConstructor(currentClass);
        }
    }



    /**
     * Adds a private constructor to a class
     *
     * @param currentClass
     */
    private void addPrivateConstructor(PsiClass currentClass) {
        PsiMethod method = javaElFactory.createConstructor();
        method.getModifierList().setModifierProperty("private", true);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            currentClass.add(method);
        });
    }



    /**
     * Adds a private constructor to a class, overloaded method that accepts a type and sets it.
     *
     * @param currentClass
     */
    private void addPrivateConstructor(PsiClass currentClass, PsiType param) {
        PsiMethod method = javaElFactory.createConstructor();
        method.getModifierList().setModifierProperty("private", true);
        PsiParameterList parameters = javaElFactory.createParameterList(new String[]{"b"}, new PsiType[]{param});
        method.getParameterList().replace(parameters);
        PsiStatement assignStmt = javaElFactory.createStatementFromText("this.b = b;", method);
        method.getBody().add(assignStmt);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            currentClass.add(method);
        });
    }



    /**
     * Adds the entire builder class if it does not already exist.
     *
     * @param currentClass
     * @return
     */
    private PsiType addBuilderClassIfNecessary(PsiClass currentClass) {
        PsiClass builderClass = buildBuilderClass(currentClass);
        boolean builderClassExists = builderClassExists(currentClass);
        if (!builderClassExists) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                currentClass.add(builderClass);
            });
        }
        return javaElFactory.createType(builderClass);
    }



    private PsiClass buildBuilderClass(PsiClass currentClass) {
        PsiClass builderClass = javaElFactory.createClass("Builder");
        builderClass.getModifierList().setModifierProperty("static", true);
        PsiType builderType = javaElFactory.createType(builderClass);
        PsiType mainClassType = javaElFactory.createType(currentClass);
        PsiField field = getInstanceField(mainClassType);
        builderClass.add(field);

        addPrivateConstructorIfNecessary(builderClass);

        LinkedList<PsiField> existingFields = getAllFields(currentClass);
        LinkedList<PsiClass> stepClasses = getSteps(existingFields, builderType, javaElFactory.createType(currentClass));

        PsiType firstType = javaElFactory.createType(stepClasses.getFirst());
        PsiMethod entryMethod = createStepMethod(existingFields.getFirst(), firstType, true);
        builderClass.add(entryMethod);

        for (PsiClass psiClass : stepClasses) {
            builderClass.add(psiClass);
        }
        return builderClass;
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
     * Builds a list of the inner classes to builder. These classes represent a 'step' in the builder.
     *
     * @param existingFields
     * @param builderType
     * @param mainClassType
     * @return
     */
    private LinkedList<PsiClass> getSteps(LinkedList<PsiField> existingFields, PsiType builderType, PsiType mainClassType) {
        LinkedList<PsiClass> steps = new LinkedList<>();
        int cur = 0;
        for (PsiField field : existingFields) {
            String stepClassName = StringUtils.getPascalCase(field.getName()) + "Step";
            PsiClass stepClass = javaElFactory.createClass(stepClassName);
            stepClass.getModifierList().setModifierProperty("public", true);
            PsiField builder = javaElFactory.createField("b", builderType);
            builder.getModifierList().setModifierProperty("private", true);
            addPrivateConstructor(stepClass, builderType);
            stepClass.add(builder);

            if (cur == existingFields.size() - 1) {
                PsiMethod build = javaElFactory.createMethod("build", mainClassType);
                PsiStatement psiStatement = javaElFactory.createStatementFromText("return instance;", build);
                build.getBody().add(psiStatement);
                stepClass.add(build);
            } else {
                String nextStepClassName = StringUtils.getPascalCase(existingFields.get(cur + 1).getName()) + "Step";
                PsiClass nextStopClass = javaElFactory.createClass(nextStepClassName);
                PsiType nextStepType = javaElFactory.createType(nextStopClass);
                PsiField nextField = existingFields.get(cur + 1);
                PsiMethod stepMethod = createStepMethod(nextField, nextStepType, false);
                stepClass.add(stepMethod);
            }
            cur++;
            steps.add(stepClass);
        }
        return steps;
    }



    private PsiMethod createStepMethod(PsiField field, PsiType type, boolean firstStep) {
        String methodName = StringUtils.getCamelCase(type.getCanonicalText());
        methodName = removeStep(methodName);
        PsiMethod method = javaElFactory.createMethod(methodName, type);
        method.getModifierList().setModifierProperty("public", true);

        PsiParameterList parameter = javaElFactory.createParameterList(new String[]{field.getName()}, new PsiType[]{field.getType()});
        method.getParameterList().replace(parameter);
        String builderVarName;
        if (firstStep) {
            builderVarName = "this";
        } else {
            builderVarName = "b";
        }
        PsiStatement instantiateStmt =
                javaElFactory.createStatementFromText(type.getCanonicalText() + " x = new " + type.getCanonicalText() + "(" + builderVarName + ");", method);
        PsiStatement varSetStmt = javaElFactory.createStatementFromText(builderVarName + ".instance." + field.getName() + " = " + field.getName() + ";",
                method);
        PsiStatement returnStmt = javaElFactory.createStatementFromText("return x;", method);
        method.getBody().add(instantiateStmt);
        method.getBody().add(varSetStmt);
        method.getBody().add(returnStmt);
        return method;
    }



    private String removeStep(String methodName) {
        return methodName.substring(0, methodName.length() - 4);
    }



    private void addBuilderFactoryIfNecessary(PsiClass currentClass, PsiType builderType) {
        PsiMethod method = javaElFactory.createMethod("getBuilder", builderType);
        method.getModifierList().setModifierProperty("public", true);
        method.getModifierList().setModifierProperty("static", true);
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



    private LinkedList<PsiField> getAllFields(PsiClass currentClass) {
        PsiField[] fields = currentClass.getFields();
        LinkedList<PsiField> fieldList = new LinkedList<>();
        for (PsiField field : fields) {
            fieldList.add(field);
        }
        return fieldList;
    }

}