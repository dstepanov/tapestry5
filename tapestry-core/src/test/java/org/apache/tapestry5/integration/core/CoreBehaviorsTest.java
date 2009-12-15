// Copyright 2009 The Apache Software Foundation
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

package org.apache.tapestry5.integration.core;

import org.apache.tapestry5.corelib.mixins.RenderDisabled;
import org.apache.tapestry5.integration.TapestryCoreTestCase;
import org.apache.tapestry5.integration.app1.pages.RenderErrorDemo;
import org.testng.annotations.Test;

public class CoreBehaviorsTest extends TapestryCoreTestCase
{

    @Test
    public void embedded_components()
    {
        clickThru("Countdown Page");

        assertTextPresent("regexp:\\s+5\\s+4\\s+3\\s+2\\s+1\\s+");

        assertTextPresent("Brought to you by the org.apache.tapestry5.integration.app1.components.Count");
    }

    /**
     * Tests the ability to inject a Block, and the ability to use the block to
     * control rendering.
     */
    @Test
    public void block_rendering() throws Exception
    {
        clickThru("BlockDemo");

        assertTextPresent("[]");

        select("//select[@id='blockName']", "fred");
        waitForPageToLoad(PAGE_LOAD_TIMEOUT);

        assertTextPresent("[Block fred.]");

        select("//select[@id='blockName']", "barney");
        waitForPageToLoad(PAGE_LOAD_TIMEOUT);

        assertTextPresent("[Block barney.]");

        // TAPESTRY-1583

        assertTextPresent("before it is defined: [Block wilma].");
    }

    @Test
    public void environmental()
    {
        clickThru("Environmental Annotation Usage");

        assertSourcePresent("[<strong>A message provided by the RenderableProvider component.</strong>]");
    }

    @Test
    public void exception_report()
    {
        // mismatched tag.
        clickThru("BadTemplate Page");

        assertTextPresent(
                "org.apache.tapestry5.ioc.internal.util.TapestryException",
                "Failure parsing template classpath:org/apache/tapestry5/integration/app1/pages/BadTemplate.tml",
                "The element type \"t:foobar\" must be terminated by the matching end-tag \"</t:foobar>\"",
                "classpath:org/apache/tapestry5/integration/app1/pages/BadTemplate.tml, line 6",
                "<t:foobar>content from template</foobar>");
    }

    @Test
    public void expansion()
    {
        clickThru("Expansion Page");

        assertTextPresent("[value provided by a template expansion]");
    }

    /**
     * {@link org.apache.tapestry5.internal.transform.InjectContainerWorker} is
     * largely tested by the forms tests
     * ({@link RenderDisabled} is built on it). test is for the failure case,
     * where a mixin class is used with the wrong
     * type of component.
     */
    @Test
    public void inject_container_failure() throws Exception
    {
        clickThru("InjectContainerMismatch");

        // And exception message:

        assertTextPresent("Component InjectContainerMismatch is not assignable to field org.apache.tapestry5.corelib.mixins.RenderDisabled.field (of type org.apache.tapestry5.Field).");
    }

    @Test
    public void inject_component_failure() throws Exception
    {
        clickThru("InjectComponentMismatch");

        assertTextPresent(
                "Unable to inject component 'form' into field form of component InjectComponentMismatch. Class org.apache.tapestry5.corelib.components.BeanEditForm is not assignable to a field of type org.apache.tapestry5.corelib.components.Form.",
                "ClassCastException");
    }

    @Test
    public void injection() throws Exception
    {
        clickThru("Inject Demo");

        // is a test for a named @Inject:
        assertTextPresent("<Proxy for Request(org.apache.tapestry5.services.Request)>");

        // is a test for an anonymous @Inject and
        // ComponentResourcesInjectionProvider
        assertTextPresent("ComponentResources[InjectDemo]");

        // Another test, DefaultInjectionProvider
        assertTextPresent("<Proxy for BindingSource(org.apache.tapestry5.services.BindingSource)>");

        // Prove that injection using a marker annotation (to match against a
        // marked service) works.

        assertTextPresent("Injection via Marker: Bonjour!");

        assertText("viaInjectService", "1722 tracks in music library");
    }

    @Test
    public void instance_mixin()
    {
        clickThru("InstanceMixin");

        final String[] dates =
        { "Jun 13, 1999", "Jul 15, 2001", "Dec 4, 2005" };

        for (String date : dates)
        {
            String snippet = String.format("[%s]", date);

            assertSourcePresent(snippet);
        }

        clickAndWait("link=Toggle emphasis");

        for (String date : dates)
        {
            String snippet = String.format("[<em>%s</em>]", date);
            assertSourcePresent(snippet);
        }
    }

    @Test
    public void localization()
    {
        clickThru("Localization");

        assertTextPresent("Via injected Messages property: [Accessed via injected Messages]");
        assertTextPresent("Via message: binding prefix: [Accessed via message: binding prefix]");
        assertTextPresent("From Application Message Catalog: [Application Catalog Working]");
        assertTextPresent("Page locale: [en]");
        clickAndWait("link=French");
        assertTextPresent("Page locale: [fr]");
        clickAndWait("link=English");
        assertTextPresent("Page locale: [en]");
    }

    @Test
    public void page_injection() throws Exception
    {
        clickThru("Inject Demo");

        clickAndWait("link=Fred");

        assertTextPresent("You clicked Fred.");

        clickAndWait("link=Back");
        clickAndWait("link=Barney");

        assertTextPresent("You clicked Barney.");

        clickAndWait("link=Back");
        clickAndWait("link=Wilma");
        assertTextPresent("You clicked Wilma.");
    }

    @Test
    public void passivate_activate() throws Exception
    {
        clickThru("NumberSelect");

        clickAndWait("link=5");

        assertTextPresent("You chose 5.");
    }

    @Test
    public void render_phase_method_returns_a_component() throws Exception
    {
        clickThru("RenderComponentDemo");

        assertText("//span[@id='container']", "[]");

        // Sneak in a little test for If and parameter else:

        assertTextPresent("Should be blank:");

        clickAndWait("enabled");

        // After clicking the link (which submits the form), the page re-renders
        // and shows us
        // the optional component from inside the NeverRender, resurrected to
        // render on the page
        // after all.

        assertText("//span[@id='container']/span", "Optional Text");

        assertTextPresent("Should now show up:");
    }

    @Test
    public void render_phase_order()
    {
        clickThru("RenderPhaseOrder");

        assertTextPresent("[BEGIN-TRACER-MIXIN BEGIN-ABSTRACT-TRACER BEGIN-TRACER BODY AFTER-TRACER AFTER-ABSTRACT-TRACER AFTER-TRACER-MIXIN]");
    }

    @Test
    public void simple_component_event()
    {
        final String YOU_CHOSE = "You chose: ";

        clickThru("Action Page");

        assertFalse(isTextPresent(YOU_CHOSE));

        for (int i = 2; i < 5; i++)
        {
            clickAndWait("link=" + i);

            assertTextPresent(YOU_CHOSE + i);
        }
    }

    @Test
    public void subclass_inherits_parent_template()
    {
        clickThru("ExpansionSubclass");

        assertTextPresent("[value provided, in the subclass, via a template expansion]");
    }

    @Test
    public void template_overridden()
    {
        clickThru("Template Overridden by Class Page");

        assertTextPresent("Output: ClassValue");
    }

    @Test
    public void pageloaded_lifecycle_method_invoked()
    {
        clickThru("PageLoaded Demo");

        assertTextPresent("[pageLoaded() was invoked.]");
    }

    @Test
    public void navigation_response_from_page_activate() throws Exception
    {
        clickThru("Protected Page");

        assertText("pagetitle", "Security Alert");

        // The message is set by Protected, but is rendered by SecurityAlert.

        assertTextPresent("Access to Protected page is denied");
    }

    @Test
    public void mixed_page_activation_context_and_component_context()
    {
        clickThru("Kicker");

        clickAndWait("link=kick target");

        assertTextSeries("//li[%d]", 1, "betty", "wilma", "betty/wilma", "\u82B1\u5B50");
        assertTextPresent("No component context.");

        clickAndWait("link=go");

        assertTextSeries("//li[%d]", 1, "betty", "wilma", "betty/wilma", "\u82B1\u5B50");
        assertTextSeries("//ul[2]/li[%d]", 1, "fred", "barney", "clark kent", "fred/barney",
                "\u592A\u90CE");
    }

    @Test
    public void page_link_with_explicit_empty_context()
    {
        clickThru("Kicker");

        clickAndWait("link=kick target");

        assertTextSeries("//li[%d]", 1, "betty", "wilma", "betty/wilma", "\u82B1\u5B50");

        clickAndWait("link=Target base, no context");

        assertTextPresent("No activation context.");
    }

    @Test
    public void page_link_with_explicit_activation_context()
    {
        clickThru("PageLink Context Demo", "no context");

        assertTextPresent("No activation context.");

        clickAndWait("link=PageLink Context Demo");

        clickAndWait("link=literal context");

        assertText("//li[1]", "literal context");

        clickAndWait("link=PageLink Context Demo");

        clickAndWait("link=computed context");

        assertTextSeries("//li[%d]", 1, "fred", "7", "true");

        clickAndWait("link=PageLink Context Demo");

        clickAndWait("link=unsafe characters");

        assertText("//li[1]", "unsafe characters: !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");

        clickAndWait("link=PageLink Context Demo");

        clickAndWait("link=japanese kanji");

        assertText("//li[1]", "japanese kanji: \u65E5\u672C\u8A9E");

        // TAPESTRY-2221

        clickAndWait("link=PageLink Context Demo");

        clickAndWait("link=Null in context");

        assertText("//li[1]", "NULL");
    }

    @Test
    public void recursive_components_are_identified_as_errors()
    {
        clickThru("Recursive Demo");

        assertTextPresent(
                "An unexpected application exception has occurred.",
                "The template for component org.apache.tapestry5.integration.app1.components.Recursive is recursive (contains another direct or indirect reference to component org.apache.tapestry5.integration.app1.components.Recursive). This is not supported (components may not contain themselves).",
                "component is <t:recursive>recursive</t:recursive>, so we\'ll see a failure.");
    }

    @Test
    public void render_phase_method_may_return_renderable()
    {
        clickThru("Renderable Demo");

        assertTextPresent("Renderable Demo", "[This proves it works.]");
    }

    @Test
    public void verify_event_handler_invocation_order_and_circumstance()
    {
        String clear = "link=clear";

        clickThru("EventHandler Demo");

        clickAndWait(clear);

        clickAndWait("link=No Context");
        assertText("methodNames",
                "[parent.eventHandlerZero(), parent.onAction(), child.eventHandlerZeroChild()]");

        clickAndWait(clear);
        clickAndWait("link=Single context value");

        assertText(
                "methodNames",
                "[parent.eventHandlerOne(String), parent.eventHandlerZero(), parent.onAction(String), parent.onAction(), child.eventHandlerOneChild(), child.eventHandlerZeroChild()]");

        clickAndWait(clear);
        clickAndWait("link=Two value context");

        assertText(
                "methodNames",
                "[parent.eventHandlerOne(String), parent.eventHandlerZero(), parent.onAction(String), parent.onAction(), child.eventHandlerOneChild(), child.eventHandlerZeroChild()]");

        clickAndWait(clear);
        clickAndWait("link=Two value context (from fred)");

        assertText(
                "methodNames",
                "[parent.eventHandlerOne(String), parent.eventHandlerZero(), parent.onAction(String), parent.onAction(), child.eventHandlerForFred(), child.eventHandlerOneChild(), child.eventHandlerZeroChild(), child.onActionFromFred(String), child.onActionFromFred()]");
    }

    @Test
    public void inherited_bindings()
    {
        clickThru("Inherited Bindings Demo");

        assertTextPresent("Bound: [ value: the-bound-value, bound: true ]",
                "Unbound: [ value: null, bound: false ]");
    }

    @Test
    public void client_persistence()
    {
        clickThru("Client Persistence Demo");
        // can't assume session won't exist because other tests use form
        // components w/ defaults, which means
        // session creation to store the ValidationTracker. So we explicitly
        // clear the session here.
        clickAndWait("link=nix session");

        assertTextPresent("Persisted value: []", "Session: [false]");

        clickAndWait("link=store string");

        assertTextPresent("Persisted value: [A String]", "Session: [false]");
    }

    @Test
    public void attribute_expansions()
    {
        clickThru("Attribute Expansions Demo");

        assertAttribute("//div[@id='mixed-expansion']/@style", "color: blue;");
        assertAttribute("//div[@id='single']/@class", "red");
        assertAttribute("//div[@id='consecutive']/@class", "goober-red");
        assertAttribute("//div[@id='trailer']/@class", "goober-green");
        assertText("//div[@id='formal']",
                "ALERT-expansions work inside formal component parameters as well");

        // An unrelated test, but fills in a bunch of minor gaps.

        assertSourcePresent("<!-- A comment! -->");
    }

    @Test
    public void event_handler_return_types()
    {
        openBaseURL();

        assertTextPresent("Tapestry 5 Integration Application 1");

        clickAndWait("link=Return Types");
        assertTextPresent("Return Type Tests");

        clickAndWait("link=null");
        assertTextPresent("Return Type Tests");

        clickAndWait("link=string");
        assertTextPresent("Tapestry 5 Integration Application 1");
        goBack();
        waitForPageToLoad();

        clickAndWait("link=class");
        assertTextPresent("Tapestry 5 Integration Application 1");
        goBack();
        waitForPageToLoad();

        clickAndWait("link=page");
        assertTextPresent("Tapestry 5 Integration Application 1");
        goBack();
        waitForPageToLoad();

        clickAndWait("link=link");
        assertTextPresent("Tapestry 5 Integration Application 1");
        goBack();
        waitForPageToLoad();

        clickAndWait("link=stream");
        assertTextPresent("Success!");
        goBack();
        waitForPageToLoad();

        /*
         * clickAndWait("link=URL");
         * assertTextPresent("Google>");
         * goBack();
         * waitForPageToLoad();
         */

        clickAndWait("link=bad");
        assertTextPresent(
                "An unexpected application exception has occurred.",
                "A component event handler method returned the value 20. Return type java.lang.Integer can not be handled.",
                "context:ReturnTypes.tml, line 50");
    }

    @Test
    public void missing_template_for_page()
    {
        clickThru("Missing Template Demo");

        assertTextPresent("Page MissingTemplate did not generate any markup when rendered. This could be because its template file could not be located, or because a render phase method in the page prevented rendering.");
    }

    /**
     * This basically checks that the services status page does not error.
     */
    @Test
    public void services_status()
    {
        open(getBaseURL() + "servicestatus");

        assertTextPresent("Tapestry IoC Services Status");
    }

    /**
     * Tests TAPESTRY-1934.
     */
    @Test
    public void base_class_must_be_in_controlled_package() throws Exception
    {
        open(getBaseURL() + "invalidsuperclass");

        assertTextPresent("Base class org.apache.tapestry5.integration.app1.WrongPackageForBaseClass (super class of org.apache.tapestry5.integration.app1.pages.InvalidSuperClass) is not in a controlled package and is therefore not valid. You should try moving the class to package org.apache.tapestry5.integration.app1.base.");
    }

    /**
     * Tests TAPESTRY-2005.
     */
    @Test
    public void components_passed_as_parameters() throws Exception
    {
        clickThru("ComponentParameter Demo");

        // This component is inside a block, and is only rendered because it is
        // passed as a parameter, of type ActionLink,
        // to an ActionLinkIndirect component.

        clickAndWait("link=click me");

        assertTextPresent("Link was clicked.");
    }

    /**
     * Tests TAPESTRY-1546
     */
    @Test
    public void inherit_informals() throws Exception
    {
        clickThru("Inherit Informal Parameters Demo");

        assertAttribute("//span[@id='target']/@class", "inherit");
    }

    /**
     * TAPESTRY-1830
     */
    @Test
    public void var_binding()
    {
        clickThru("Var Binding Demo");

        assertTextSeries("//li[%d]", 1, "1", "2", "3");
    }

    /**
     * TAPESTRY-1724
     */
    @Test
    public void component_event_errors()
    {
        clickThru("Exception Event Demo", "enable", "force invalid activation context");

        assertTextPresent("Exception: Exception in method org.apache.tapestry5.integration.app1.pages.ExceptionEventDemo.onActivate(float)");

        clickAndWait("link=force invalid event context");

        assertTextPresent("Exception: Exception in method org.apache.tapestry5.integration.app1.pages.ExceptionEventDemo.onActionFromFail(float)");

        // Revert to normal handling: return null from the onException() event
        // handler method.

        clickAndWait("link=disable");

        clickAndWait("link=force invalid event context");

        assertTextPresent("An unexpected application exception has occurred.",
                "org.apache.tapestry5.runtime.ComponentEventException",
                "java.lang.NumberFormatException");
    }

    /**
     * TAPESTRY-1518
     */
    @Test
    public void generic_page_type()
    {
        clickThru("Generic Page Class Demo");

        assertTextPresent("Editor for org.apache.tapestry5.integration.app1.data.Track");

        assertText("//label[@id='title-label']", "Title");
        assertAttribute("//label[@id='title-label']/@for", "title");
    }

    /**
     * TAPESTRY-2097
     */
    @Test
    public void render_queue_exception()
    {
        clickThru("Render Error Demo");

        assertTextPresent("An unexpected application exception has occurred");

        // Just sample a smattering of the vast amount of data in the exception
        // report.

        assertTextPresent("RenderErrorDemo", "class " + RenderErrorDemo.class.getName(),
                "RenderErrorDemo:border", "RenderErrorDemo:echo");
    }

    /**
     * TAPESTRY-2088
     */
    @Test
    public void primitive_array_as_parameter_type()
    {
        clickThru("Primitive Array Parameter Demo");

        assertSourcePresent("<ul><li>1</li><li>3</li><li>5</li><li>7</li><li>9</li></ul>");
    }

    /**
     * TAPESTRY-1594
     */
    @Test
    public void ignored_paths_filter()
    {
        clickThru("Unreachable Page");

        // This message changes from one release of Jetty to the next sometimes
        assertText("//title", "Error 404 Not Found");
    }

    /**
     * TAPESTRY-2085
     */
    @Test
    public void render_phase_methods_may_throw_checked_exceptions()
    {
        clickThru("Render Phase Method Exception Demo");

        assertTextPresent("Render queue error in BeginRender[RenderPhaseMethodExceptionDemo]: java.sql.SQLException: Simulated JDBC exception while rendering.");
    }

    /**
     * TAPESTRY-2114
     */
    @Test
    public void boolean_properties_can_use_get_or_is_as_method_name_prefix()
    {
        clickThru("Boolean Property Demo", "clear");

        assertText("usingGet", "false");
        assertText("usingIs", "false");

        clickAndWait("link=set");

        assertText("usingGet", "true");
        assertText("usingIs", "true");
    }

    /**
     * TAPESTRY-1475
     */
    @Test
    public void discard_persistent_field_changes()
    {
        clickThru("Persistent Demo");

        assertText("message", "");

        clickAndWait("link=Update the message field");

        assertText("message", "updated");

        clickAndWait("link=Refresh page");

        assertText("message", "updated");

        clickAndWait("link=Discard persistent field changes");

        assertText("message", "");
    }

    /**
     * TAPESTRY-2150. Also demonstrates how to add a ValueEncoder for an entity
     * object, to allow seamless encoding of
     * the entity's id into the URL.
     */
    @Test
    public void nested_page_names()
    {
        clickThru("Music Page", "2");

        assertText("activePageName", "Music");

        clickAndWait("link=The Gift");

        assertText("activePageName", "music/Details");
    }

    /**
     * TAPESTRY-2235
     */
    @Test
    public void generated_activation_context_handlers()
    {
        clickThru("Music Page", "69");

        assertText("activePageName", "Music");

        clickAndWait("link=Wake Me Up (Copy)");

        assertText("activePageName", "music/Details2");

        assertText("//dd[@class='title']", "Wake Me Up");

        assertText("//dd[@class='artist']", "Norah Jones");
    }

    /**
     * TAPESTRY-1999
     */
    @Test
    public void list_as_event_context()
    {
        clickThru("List Event Context Demo");

        assertTextSeries("//ul[@id='eventcontext']/li[%d]", 1, "1", "2", "3");
    }

    /**
     * TAPESTRY-2196
     */
    @Test
    public void protected_field_in_page_class()
    {
        clickThru("Protected Fields Demo", "Trigger the Exception");

        assertTextPresent(
                "An unexpected application exception has occurred.",
                "Class org.apache.tapestry5.integration.app1.pages.ProtectedFields contains field(s) (_field) that are not private. You should change these fields to private, and add accessor methods if needed.");
    }

    /**
     * TAPESTRY-2078
     */
    @Test
    public void noclassdeffound_exception_is_linked_to_underlying_cause()
    {
        clickThru("Class Transformation Exception Demo");

        assertTextPresent("Class org.apache.tapestry5.integration.app1.pages.Datum contains field(s) (_value) that are not private. You should change these fields to private, and add accessor methods if needed.");
    }

    /**
     * TAPESTRY-2338
     */
    @Test
    public void cached_properties_cleared_at_end_of_request()
    {
        clickThru("Clean Cache Demo");

        String time1_1 = getText("time1");
        String time1_2 = getText("time1");

        // Don't know what they are but they should be the same.

        assertEquals(time1_2, time1_1);

        click("link=update");

        sleep(250);

        String time2_1 = getText("time1");
        String time2_2 = getText("time1");

        // Check that @Cache is still working

        assertEquals(time2_2, time2_1);

        assertFalse(time2_1.equals(time1_1),
                "After update the nanoseconds time did not change, meaning @Cache was broken.");
    }

    @Test
    public void method_advice()
    {
        clickThru("Method Advice Demo");

        // @ReverseStrings intercepted and reversed the result:
        assertText("message", "!olleH");

        // @ReverseStrings doesn't do anything for non-Strings
        assertText("version", "5");

        // @ReverseStrings filtered the checked exception to a string result
        assertText(
                "cranky",
                "Invocation of method getCranky() failed with org.apache.tapestry5.integration.app1.services.DearGodWhyMeException.");

        // Now to check advice on a setter that manipulates parameters

        type("text", "Tapestry");
        clickAndWait(SUBMIT);

        assertText("output-text", "yrtsepaT");
    }

    @Test
    public void component_classes_may_not_be_directly_instantiated()
    {
        clickThru("Instantiate Page");

        assertTextPresent("Component class org.apache.tapestry5.integration.app1.pages.Music may not be instantiated directly.");
    }

    /**
     * TAPESTRY-2567
     */
    public void field_annotation_conflict()
    {
        clickThru("Field Annotation Conflict");

        assertTextPresent("Field flashDemo of class org.apache.tapestry5.integration.app1.pages.FieldAnnotationConflict is already claimed by @org.apache.tapestry5.annotations.InjectPage and can not be claimed by @org.apache.tapestry5.annotations.Parameter.");
    }

    /**
     * TAPESTRY-2610
     */
    public void access_to_informal_parameters()
    {
        clickThru("Informal Parameters Demo");

        assertTextSeries("//dl[@id='informals']/dt[%d]", 1, "barney", "fred", "pageName");
        assertTextSeries("//dl[@id='informals']/dd[%d]", 1, "rubble", "flintstone",
                "InformalParametersDemo");
    }

    /**
     * TAPESTRY-2517
     */
    public void cached_exception_for_loading_failed_page()
    {
        clickThru("Failed Field Injection Demo");

        assertTextPresent("Error obtaining injected value for field org.apache.tapestry5.integration.app1.pages.FailedInjectDemo.buffer: No service implements the interface java.lang.StringBuffer.");

        refresh();
        waitForPageToLoad(PAGE_LOAD_TIMEOUT);

        // Before this bug was fixed, this message would not appear; instead on
        // complaining about _$resources would appear which was very confusing.

        assertTextPresent("Error obtaining injected value for field org.apache.tapestry5.integration.app1.pages.FailedInjectDemo.buffer: No service implements the interface java.lang.StringBuffer.");
    }

    /**
     * TAPESTRTY-2644
     */
    public void create_page_link_via_page_class()
    {
        clickThru("PageLink via Class Demo");

        assertTextPresent("Demonstrates the use of the @Inject annotation.");
    }
}
