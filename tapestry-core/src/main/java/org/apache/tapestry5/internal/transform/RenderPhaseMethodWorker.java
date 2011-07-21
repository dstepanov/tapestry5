// Copyright 2006, 2007, 2008, 2010 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.internal.transform;

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.func.F;
import org.apache.tapestry5.func.Flow;
import org.apache.tapestry5.func.Predicate;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.*;
import org.apache.tapestry5.runtime.Event;
import org.apache.tapestry5.services.*;
import org.apache.tapestry5.services.MethodInvocationResult;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Converts one of the methods of {@link org.apache.tapestry5.runtime.Component} into a chain of
 * command that, itself,
 * invokes certain methods (render phase methods) marked with an annotation, or named in a specific
 * way.
 */
@SuppressWarnings("all")
public class RenderPhaseMethodWorker implements ComponentClassTransformWorker2
{
    private final class RenderPhaseMethodAdvice implements ComponentMethodAdvice
    {
        private final boolean reverse;

        private final List<Invoker> invokers;

        private RenderPhaseMethodAdvice(boolean reverse, List<Invoker> invokers)
        {
            this.reverse = reverse;
            this.invokers = invokers;
        }

        public void advise(ComponentMethodInvocation invocation)
        {
            if (!reverse)
                invocation.proceed();

            // All render phase methods take the same two parameters (writer and event)

            Event event = (Event) invocation.getParameter(1);

            if (event.isAborted())
                return;

            Object instance = invocation.getInstance();
            MarkupWriter writer = (MarkupWriter) invocation.getParameter(0);

            for (Invoker invoker : invokers)
            {
                invoker.invoke(instance, writer, event);

                if (event.isAborted())
                    return;
            }

            // Parent class implementation goes last.

            if (reverse)
                invocation.proceed();
        }
    }

    private class Invoker
    {
        private final String methodIdentifier;

        private final MethodAccess access;

        Invoker(String methodIdentifier, MethodAccess access)
        {
            this.methodIdentifier = methodIdentifier;
            this.access = access;
        }

        void invoke(Object instance, MarkupWriter writer, Event event)
        {
            event.setMethodDescription(methodIdentifier);

            // As currently implemented, MethodAccess objects ignore excess parameters.

            MethodInvocationResult result = access.invoke(instance, writer);

            result.rethrow();

            event.storeResult(result.getReturnValue());
        }

    }

    private final Map<Class<? extends Annotation>, TransformMethodSignature> annotationToSignature = CollectionFactory
            .newMap();

    private final Map<Class<? extends Annotation>, MethodDescription> annotationToDescription = CollectionFactory.newMap();

    private final Map<String, Class<? extends Annotation>> nameToAnnotation = CollectionFactory.newCaseInsensitiveMap();

    private final Set<Class<? extends Annotation>> reverseAnnotations = CollectionFactory.newSet(AfterRenderBody.class,
            AfterRenderTemplate.class, AfterRender.class, CleanupRender.class);

    private final Set<TransformMethodSignature> lifecycleMethods = CollectionFactory.newSet();
    private final Set<MethodDescription> lifecycleMethods2 = CollectionFactory.newSet();

    {
        annotationToSignature.put(SetupRender.class, TransformConstants.SETUP_RENDER_SIGNATURE);
        annotationToSignature.put(BeginRender.class, TransformConstants.BEGIN_RENDER_SIGNATURE);
        annotationToSignature.put(BeforeRenderTemplate.class, TransformConstants.BEFORE_RENDER_TEMPLATE_SIGNATURE);
        annotationToSignature.put(BeforeRenderBody.class, TransformConstants.BEFORE_RENDER_BODY_SIGNATURE);
        annotationToSignature.put(AfterRenderBody.class, TransformConstants.AFTER_RENDER_BODY_SIGNATURE);
        annotationToSignature.put(AfterRenderTemplate.class, TransformConstants.AFTER_RENDER_TEMPLATE_SIGNATURE);
        annotationToSignature.put(AfterRender.class, TransformConstants.AFTER_RENDER_SIGNATURE);
        annotationToSignature.put(CleanupRender.class, TransformConstants.CLEANUP_RENDER_SIGNATURE);

        annotationToDescription.put(SetupRender.class, TransformConstants.SETUP_RENDER_DESCRIPTION);
        annotationToDescription.put(BeginRender.class, TransformConstants.BEGIN_RENDER_DESCRIPTION);
        annotationToDescription.put(BeforeRenderTemplate.class, TransformConstants.BEFORE_RENDER_TEMPLATE_DESCRIPTION);
        annotationToDescription.put(BeforeRenderBody.class, TransformConstants.BEFORE_RENDER_BODY_DESCRIPTION);
        annotationToDescription.put(AfterRenderBody.class, TransformConstants.AFTER_RENDER_BODY_DESCRIPTION);
        annotationToDescription.put(AfterRenderTemplate.class, TransformConstants.AFTER_RENDER_TEMPLATE_DESCRIPTION);
        annotationToDescription.put(AfterRender.class, TransformConstants.AFTER_RENDER_DESCRIPTION);
        annotationToDescription.put(CleanupRender.class, TransformConstants.CLEANUP_RENDER_DESCRIPTION);


        for (Entry<Class<? extends Annotation>, TransformMethodSignature> me : annotationToSignature.entrySet())
        {
            lifecycleMethods.add(me.getValue());
        }

        for (Entry<Class<? extends Annotation>, MethodDescription> me : annotationToDescription.entrySet())
        {
            nameToAnnotation.put(me.getValue().methodName, me.getKey());
            lifecycleMethods2.add(me.getValue());
        }

    }

    public void transform(ClassTransformation transformation, MutableComponentModel model)
    {
        Map<Class, List<TransformMethod>> methods = mapRenderPhaseAnnotationToMethods(transformation);

        for (Class renderPhaseAnnotation : methods.keySet())
        {
            mapMethodsToRenderPhase(transformation, model, renderPhaseAnnotation, methods.get(renderPhaseAnnotation));
        }
    }

    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        Map<Class, List<PlasticMethod>> methods = mapRenderPhaseAnnotationToMethods(plasticClass);

        for (Class renderPhaseAnnotation : methods.keySet())
        {
            mapMethodsToRenderPhase(plasticClass, support.isRootTransformation(), renderPhaseAnnotation, methods.get(renderPhaseAnnotation));

            model.addRenderPhase(renderPhaseAnnotation);
        }

    }

    private void mapMethodsToRenderPhase(ClassTransformation transformation, MutableComponentModel model,
                                         Class annotationType, List<TransformMethod> methods)
    {
        ComponentMethodAdvice renderPhaseAdvice = createAdviceForMethods(annotationType, methods);

        TransformMethodSignature renderPhaseSignature = annotationToSignature.get(annotationType);

        transformation.getOrCreateMethod(renderPhaseSignature).addAdvice(renderPhaseAdvice);

        model.addRenderPhase(annotationType);
    }

    private InstructionBuilderCallback JUST_RETURN = new InstructionBuilderCallback()
    {
        public void doBuild(InstructionBuilder builder)
        {
            builder.returnDefaultValue();
        }
    };

    private void mapMethodsToRenderPhase(final PlasticClass plasticClass, final boolean isRoot, Class annotationType, List<PlasticMethod> methods)
    {

        // The method, defined by Component, that will in turn invoke the other methods.

        final MethodDescription interfaceMethodDescription = annotationToDescription.get(annotationType);
        PlasticMethod interfaceMethod = plasticClass.introduceMethod(interfaceMethodDescription);

        final boolean reverse = reverseAnnotations.contains(annotationType);

        final Flow<PlasticMethod> orderedMethods =
                reverse ? F.flow(methods).reverse()
                        : F.flow(methods);

        interfaceMethod.changeImplementation(new InstructionBuilderCallback()
        {
            private void addSuperCall(InstructionBuilder builder)
            {
                builder.loadThis().loadArguments().invokeSpecial(plasticClass.getSuperClassName(), interfaceMethodDescription);

            }

            private void invokeMethod(InstructionBuilder builder, PlasticMethod method)
            {
                // First, tell the Event object what method is being invoked.

                builder.loadArgument(1);
                builder.loadConstant(
                        String.format("%s.%s", plasticClass.getClassName(),
                                method.getDescription().toShortString()));
                builder.invoke(Event.class, void.class, "setMethodDescription", String.class);

                builder.loadThis();

                // Methods either take no parameters, or take a MarkupWriter parameter.

                if (method.getParameters().size() > 0)
                {
                    builder.loadArgument(0);
                }

                builder.invokeVirtual(method);

                // Non-void methods will pass a value to the event.

                if (!method.getDescription().returnType.equals("void"))
                {
                    builder.boxPrimitive(method.getDescription().returnType);
                    builder.loadArgument(1).swap();

                    builder.invoke(Event.class, boolean.class, "storeResult", Object.class);

                    builder.when(Condition.NON_ZERO, JUST_RETURN);
                }
            }

            public void doBuild(InstructionBuilder builder)
            {
                if (!reverse && !isRoot)
                {
                    addSuperCall(builder);

                    builder.loadArgument(1).invoke(Event.class, boolean.class, "isAborted");

                    builder.when(Condition.NON_ZERO, JUST_RETURN);
                }

                for (PlasticMethod invokedMethod : orderedMethods)
                {
                    invokeMethod(builder, invokedMethod);
                }

                if (reverse && !isRoot)
                {
                    addSuperCall(builder);
                }

                builder.returnDefaultValue();
            }
        });
    }


    private ComponentMethodAdvice createAdviceForMethods(Class annotationType, List<TransformMethod> methods)
    {
        boolean reverse = reverseAnnotations.contains(annotationType);

        List<Invoker> invokers = toInvokers(annotationType, methods, reverse);

        return new RenderPhaseMethodAdvice(reverse, invokers);
    }

    private List<Invoker> toInvokers(Class annotationType, List<TransformMethod> methods, boolean reverse)
    {
        List<Invoker> result = CollectionFactory.newList();

        for (TransformMethod method : methods)
        {
            MethodAccess methodAccess = toMethodAccess(method);

            Invoker invoker = new Invoker(method.getMethodIdentifier(), methodAccess);

            result.add(invoker);
        }

        if (reverse)
            Collections.reverse(result);

        return result;
    }

    private MethodAccess toMethodAccess(TransformMethod method)
    {
        validateAsRenderPhaseMethod(method);

        return method.getAccess();
    }

    private void validateAsRenderPhaseMethod(TransformMethod method)
    {
        String[] parameterTypes = method.getSignature().getParameterTypes();

        switch (parameterTypes.length)
        {
            case 0:
                break;

            case 1:
                if (parameterTypes[0].equals(MarkupWriter.class.getName()))
                    break;
            default:
                throw new RuntimeException(
                        String
                                .format(
                                        "Method %s is not a valid render phase method: it should take no parameters, or take a single parameter of type MarkupWriter.",
                                        method.getMethodIdentifier()));
        }
    }

    private Map<Class, List<TransformMethod>> mapRenderPhaseAnnotationToMethods(final ClassTransformation transformation)
    {
        Map<Class, List<TransformMethod>> map = CollectionFactory.newMap();

        List<TransformMethod> matches = matchAllMethodsNotOverriddenFromBaseClass(transformation);

        for (TransformMethod method : matches)
        {
            addMethodToRenderPhaseCategoryMap(map, method);
        }

        return map;
    }


    private Map<Class, List<PlasticMethod>> mapRenderPhaseAnnotationToMethods(PlasticClass plasticClass)
    {
        Map<Class, List<PlasticMethod>> map = CollectionFactory.newMap();

        Flow<PlasticMethod> matches = matchAllMethodsNotOverriddenFromBaseClass(plasticClass);

        for (PlasticMethod method : matches)
        {
            addMethodToRenderPhaseCategoryMap(map, method);
        }

        return map;
    }


    private void addMethodToRenderPhaseCategoryMap(Map<Class, List<TransformMethod>> map, TransformMethod method)
    {
        Class categorized = categorizeMethod(method);

        if (categorized != null)
            InternalUtils.addToMapList(map, categorized, method);
    }


    private void addMethodToRenderPhaseCategoryMap(Map<Class, List<PlasticMethod>> map, PlasticMethod method)
    {
        Class categorized = categorizeMethod(method);

        if (categorized != null)
        {
            validateAsRenderPhaseMethod(method);

            InternalUtils.addToMapList(map, categorized, method);
        }
    }

    private Class categorizeMethod(TransformMethod method)
    {
        for (Class annotationClass : annotationToSignature.keySet())
        {
            if (method.getAnnotation(annotationClass) != null)
                return annotationClass;
        }

        return nameToAnnotation.get(method.getName());
    }

    private Class categorizeMethod(PlasticMethod method)
    {
        for (Class annotationClass : annotationToDescription.keySet())
        {
            if (method.hasAnnotation(annotationClass))
                return annotationClass;
        }

        return nameToAnnotation.get(method.getDescription().methodName);
    }

    private List<TransformMethod> matchAllMethodsNotOverriddenFromBaseClass(final ClassTransformation transformation)
    {
        return transformation.matchMethods(new Predicate<TransformMethod>()
        {
            public boolean accept(TransformMethod method)
            {
                return !method.isOverride() && !lifecycleMethods.contains(method.getSignature());
            }
        });

    }

    private void validateAsRenderPhaseMethod(PlasticMethod method)
    {
        final String[] argumentTypes = method.getDescription().argumentTypes;

        switch (argumentTypes.length)
        {
            case 0:
                break;

            case 1:
                if (argumentTypes[0].equals(MarkupWriter.class.getName()))
                    break;
            default:
                throw new RuntimeException(
                        String.format(
                                "Method %s is not a valid render phase method: it should take no parameters, or take a single parameter of type MarkupWriter.",
                                method.toString()));
        }
    }


    private Flow<PlasticMethod> matchAllMethodsNotOverriddenFromBaseClass(final PlasticClass plasticClass)
    {
        return F.flow(plasticClass.getMethods()).filter(new Predicate<PlasticMethod>()
        {
            public boolean accept(PlasticMethod method)
            {
                return !method.isOverride() && !lifecycleMethods2.contains(method.getDescription());
            }
        });
    }

}
