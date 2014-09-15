/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.Compartment;
import org.arachna.netweaver.dc.types.CompartmentState;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.arachna.velocity.VelocityHelper;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import com.ibm.icu.util.Calendar;

/**
 * Unittest for {@link SonarPomGenerator}.
 * 
 * @author Dirk Weigenand
 */
public class SonarPomGeneratorTest extends XMLTestCase {
    /**
     * instance under test.
     */
    private SonarPomGenerator generator;

    /**
     * development component to generate pom for.
     */
    private DevelopmentComponent component;

    /**
     * compartment containing the DC above.
     */
    private Compartment compartment;

    /**
     * development configuration containing the compartment above.
     */
    private DevelopmentConfiguration configuration;

    /**
     * Build helper.
     */
    private AntHelper antHelper;

    private DevelopmentComponentFactory dcFactory;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        setUpNamespaceMapping();
        setUpExampleComponents();

        dcFactory = new DevelopmentComponentFactory();
        antHelper = Mockito.mock(AntHelper.class);
        generator =
            new SonarPomGenerator(antHelper, dcFactory, new VelocityHelper().getVelocityEngine(), Calendar.getInstance().getTimeInMillis());
    }

    /**
     * Set up fixture with example components (development configuration, SC, DC).
     */
    private void setUpExampleComponents() {
        configuration = new DevelopmentConfiguration("DI_XMPL_D");
        compartment = Compartment.create("org.arachna", "SC", CompartmentState.Source, "caption");
        configuration.add(compartment);
        component = new DevelopmentComponent("dc", "org.arachna", DevelopmentComponentType.Java);
        compartment.add(component);
    }

    /**
     * initialize XMLUtil with name space mapping used in pom (needed because default name space in XPath & XML with name spaces didn't work
     * out for testing).
     */
    private void setUpNamespaceMapping() {
        final Map<String, String> nameSpaceMappings = new LinkedHashMap<String, String>();
        nameSpaceMappings.put("", "http://maven.apache.org/POM/4.0.0");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(nameSpaceMappings));
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.sonar.SonarPomGenerator#execute(org.arachna.netweaver.dc.types.DevelopmentComponent, java.io.Writer)} .
     * 
     * @throws IOException
     */
    @Test
    public final void testExecuteWithOneSourceFolder() throws IOException {
        final String loneSourceDir = "packages";
        Mockito.when(antHelper.createSourceFileSets(component)).thenReturn(Arrays.asList(loneSourceDir));
        assertXpathEvaluatesTo(loneSourceDir, "/project/build/sourceDirectory/text()");
    }

    private void assertXpathEvaluatesTo(final String expected, final String xPath) {
        try {
            final StringWriter result = new StringWriter();
            generator.execute(component, result);
            System.err.println(result);
            assertXpathEvaluatesTo(expected, xPath, result.toString());
        }
        catch (final XpathException e) {
            fail(e);
        }
        catch (final SAXException e) {
            fail(e);
        }
        catch (final IOException e) {
            fail(e);
        }
    }

    private void fail(final Throwable t) {
        if (t.getCause() != null) {
            fail(t.getCause());
        }
        else {
            fail(t.getLocalizedMessage());
        }
    }
}
