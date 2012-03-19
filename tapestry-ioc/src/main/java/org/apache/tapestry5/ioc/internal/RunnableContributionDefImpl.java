// Copyright 2006, 2007, 2008, 2009, 2010, 2011 The Apache Software Foundation
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

package org.apache.tapestry5.ioc.internal;

import org.apache.tapestry5.ioc.*;
import org.apache.tapestry5.ioc.def.ContributionDef3;
import org.apache.tapestry5.ioc.internal.util.*;
import org.apache.tapestry5.ioc.services.PlasticProxyFactory;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

public class RunnableContributionDefImpl implements ContributionDef3
{
    private final String serviceId;

    private final Method contributorMethod;

    private final boolean optional;

    private final PlasticProxyFactory proxyFactory;

    private final Set<Class> markers;

    private final Class serviceInterface;
    
    private final String id;
    
    private final String[] constraints;

    public RunnableContributionDefImpl(String serviceId, Method contributorMethod, boolean optional, PlasticProxyFactory proxyFactory,
                               Class serviceInterface, Set<Class> markers, String id, String[] constraints)
    {
        this.serviceId = serviceId;
        this.contributorMethod = contributorMethod;
        this.optional = optional;
        this.proxyFactory = proxyFactory;
        this.serviceInterface = serviceInterface;
        this.markers = markers;
        this.id = id;
        this.constraints = constraints == null ? new String[0] : constraints;
    }

    @Override
    public String toString()
    {
        return String.format("RunnableContribution[%s]", InternalUtils.asString(contributorMethod, proxyFactory));
    }

    public boolean isOptional()
    {
        return optional;
    }

    public String getServiceId()
    {
        return serviceId;
    }

    public void contribute(ModuleBuilderSource moduleSource, ServiceResources resources, Configuration configuration)
    {
    	configuration.add(createRunnable(moduleSource, resources));
    }

    public void contribute(ModuleBuilderSource moduleSource, ServiceResources resources,
                           OrderedConfiguration configuration)
    {
    	configuration.add(getContributionId(), createRunnable(moduleSource, resources), constraints);
    }

    public void contribute(ModuleBuilderSource moduleSource, ServiceResources resources,
                           MappedConfiguration configuration)
    {
        configuration.add(getContributionId(), createRunnable(moduleSource, resources));
    }

    private String getContributionId()
    {
    	if(id == null || id.isEmpty())
    	{
    		return contributorMethod.getDeclaringClass().getSimpleName() + "." + contributorMethod.getName();
    	}
    	return id;
    }
    
    private Runnable createRunnable(ModuleBuilderSource source, ServiceResources resources)
    {
        Map<Class, Object> resourceMap = CollectionFactory.newMap();

        resourceMap.put(ObjectLocator.class, resources);
        resourceMap.put(Logger.class, resources.getLogger());

        InjectionResources injectionResources = new MapInjectionResources(resourceMap);

        final Object moduleInstance = InternalUtils.isStatic(contributorMethod) ? null : source.getModuleBuilder();

        try
        {
        	ObjectCreator[] parameters = InternalUtils.calculateParametersForMethod(contributorMethod, resources,
                     injectionResources, resources.getTracker());

	    	return createRunnable(moduleInstance, InternalUtils.realizeObjects(parameters));
	    }
        catch (Exception ex)
	    {
	        throw new RuntimeException(IOCMessages.contributionMethodError(contributorMethod, ex), ex);
	    }
    }

    private Runnable createRunnable(final Object instance, final Object[] parameters)
    {
    	return new Runnable() {
			
			public void run()
			{
				Throwable fail = null;
	            try {
	            	contributorMethod.invoke(instance, parameters);
		        }
	            catch (InvocationTargetException ex)
		        {
		        	fail = ex.getTargetException();
		        }
	            catch (Exception ex)
		        {
		        	fail = ex;
		        }
	            if (fail != null)
	    	        throw new RuntimeException(IOCMessages.contributionMethodError(contributorMethod, fail), fail);
			}
		};
    }
    
    public Set<Class> getMarkers()
    {
        return markers;
    }

    public Class getServiceInterface()
    {
        return serviceInterface;
    }
}
