/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.Compartment;
import org.arachna.netweaver.dc.types.CompartmentState;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.arachna.velocity.VelocityHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unittest for {@link SonarPomGenerator}.
 * 
 * @author Dirk Weigenand
 */
public class SonarPomGeneratorTest {
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
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        configuration = new DevelopmentConfiguration("DI_XMPL_D");
        compartment = Compartment.create("org.arachna", "SC", CompartmentState.Source, "caption");
        configuration.add(compartment);
        component = new DevelopmentComponent("dc", "org.arachna", DevelopmentComponentType.Java);
        compartment.add(component);
        final AntHelper helper = Mockito.mock(AntHelper.class);
        generator = new SonarPomGenerator(helper, new VelocityHelper(new PrintStream(System.out)).getVelocityEngine());
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.sonar.SonarPomGenerator#execute(org.arachna.netweaver.dc.types.DevelopmentComponent, java.io.Writer)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public final void testExecute() throws IOException {
        generator.execute(component, new PrintWriter(System.out));
    }
}
