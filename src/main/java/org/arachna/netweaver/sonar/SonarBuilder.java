/**
 * 
 */
package org.arachna.netweaver.sonar;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tools.ToolInstallation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.arachna.netweaver.hudson.util.FilePathHelper;
import org.arachna.velocity.VelocityHelper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Jenkins builder that executes the maven sonar plugin for NetWeaver development components.
 * 
 * @author Dirk Weigenand
 */
public class SonarBuilder extends Builder {
    /**
     * Descriptor for {@link SonarBuilder}.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Data bound constructor. Used for populating a {@link SonarBuilder} instance from form fields in <code>config.jelly</code>.
     */
    @DataBoundConstructor
    public SonarBuilder() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
        throws InterruptedException, IOException {
        boolean result = true;
        final NWDIBuild nwdiBuild = (NWDIBuild)build;
        final AntHelper antHelper =
            new AntHelper(FilePathHelper.makeAbsolute(build.getWorkspace()), nwdiBuild.getDevelopmentComponentFactory());
        final SonarPomGenerator pomGenerator =
            new SonarPomGenerator(antHelper, nwdiBuild.getDevelopmentComponentFactory(), new VelocityHelper().getVelocityEngine(),
                nwdiBuild.getNumber());
        String pomLocation = "";

        final MavenInstallation maven = getRequiredMavenInstallation(launcher);

        if (maven != null) {
            // FIXME: get JVM options from configuration.
            final String jvmOptions = "";
            final String properties = "";

            for (final DevelopmentComponent component : nwdiBuild.getAffectedDevelopmentComponents(new DCWithJavaSourceAcceptingFilter())) {
                if (component.getCompartment() != null) {
                    if (!antHelper.createSourceFileSets(component).isEmpty() || !component.getResourceFolders().isEmpty()) {
                        try {
                            pomLocation = String.format("%s/sonar-pom.xml", antHelper.getBaseLocation(component));
                            pomGenerator.execute(component, new FileWriter(pomLocation));

                            result |=
                                new Maven("test sonar:sonar", maven.getName(), pomLocation, properties, jvmOptions).perform(nwdiBuild,
                                    launcher, listener);
                            
                            listener.getLogger().println(String.format("Component %s is of type %s.",component.getName(), component.getType()));
                            
                            if (component.getType().equals(DevelopmentComponentType.J2EEWebModule)) {
                                result |=
                                    new Maven("sonar:sonar", maven.getName(), pomLocation, properties, jvmOptions + " -Dsonar.language=js").perform(nwdiBuild,
                                        launcher, listener);
                            } else {
                            	listener.getLogger().println(String.format("Component %s is not a web module.",component.getName()));
                            }
                        }
                        catch (final IOException ioe) {
                            Logger.getLogger("NWDI-Sonar-Plugin").warning(
                                String.format("Could not create %s:\n%s", pomLocation, ioe.getMessage()));
                        }
                    } else {
                    	listener.getLogger().println(String.format("Component %s has empty source or resource folders.",component.getName()));
                    }
                }
                else {
                    listener.getLogger().println(String.format("%s:%s has no compartment!", component.getVendor(), component.getName()));
                }
            }
        }
        else {
            listener.getLogger().println("No Maven installation found!");
        }

        return result;
    }

    private MavenInstallation getRequiredMavenInstallation(final Launcher launcher) throws IOException, InterruptedException {
        final MavenInstallation.DescriptorImpl descriptor = ToolInstallation.all().get(MavenInstallation.DescriptorImpl.class);
        final MavenInstallation[] installations = descriptor.getInstallations();
        MavenInstallation maven = null;

        for (final MavenInstallation installation : installations) {
            if (installation.getExists() && installation.meetsMavenReqVersion(launcher, MavenInstallation.MAVEN_30)) {
                maven = installation;
            }
        }

        if (installations.length > 0) {
            maven = installations[0];
        }

        return maven;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor for {@link SonarBuilder}.
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Create descriptor for NWDI-CheckStyle-Builder and load global configuration data.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return NWDIProject.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "NWDI Sonar Builder";
        }
    }
}
