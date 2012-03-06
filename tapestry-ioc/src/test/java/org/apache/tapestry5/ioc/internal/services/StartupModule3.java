// Copyright 2010 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.ioc.internal.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.apache.tapestry5.ioc.services.ThreadLocale;
import org.slf4j.Logger;

public class StartupModule3
{
    public static List<String> startupOrder = new ArrayList<String>();
    
    @Startup(id = "third", constraints = {"after:second"})
    public void third(ObjectLocator locator, Logger logger)
    {
    	startupOrder.add("third");

    	logger.info("StartupModule2.third invoked");
    }
    
    @Startup(id = "first")
    public void first(ObjectLocator locator, Logger logger, ThreadLocale threadLocale)
    {
    	startupOrder.add("first");
    	
    	threadLocale.getLocale();

    	logger.info("StartupModule2.first invoked");
    }
    
    @Startup(id = "second", constraints = {"after:first"})
    public void second(ObjectLocator locator, Logger logger, RegistryShutdownHub registryShutdownHub)
    {
    	startupOrder.add("second");
    	
    	registryShutdownHub.addRegistryShutdownListener(new Runnable() {
			
			public void run()
			{
			}
		});
    	
    	logger.info("StartupModule2.second invoked");
    }
    
}
