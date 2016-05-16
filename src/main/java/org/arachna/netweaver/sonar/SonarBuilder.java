/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.arachna.netweaver.hudson.util.FilePathHelper;
import org.arachna.velocity.VelocityHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tools.ToolInstallation;

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
        final SonarPomGenerator pomGenerator = new SonarPomGenerator(antHelper, nwdiBuild.getDevelopmentComponentFactory(),
            new VelocityHelper().getVelocityEngine(), nwdiBuild.getNumber());

        final MavenInstallation maven = getRequiredMavenInstallation(launcher);

        if (maven != null) {
            SonarCubeAnalysisExecutor analysisExecutor =
                new SonarCubeAnalysisExecutor(nwdiBuild, antHelper, pomGenerator, maven, launcher, listener);

            for (final DevelopmentComponent component : nwdiBuild.getAffectedDevelopmentComponents(new DCWithJavaSourceAcceptingFilter())) {
                if (component.getCompartment() != null) {
                    if (!antHelper.createSourceFileSets(component).isEmpty() || !component.getResourceFolders().isEmpty()) {
                        for (SonarLanguage language : SonarLanguage.getSonarLanguages(component)) {
                            result |= analysisExecutor.execute(component, language);
                        }
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

    private static class SonarCubeAnalysisExecutor {
        private final NWDIBuild nwdiBuild;
        private final AntHelper antHelper;
        private final SonarPomGenerator pomGenerator;
        private final MavenInstallation maven;
        private Launcher launcher;
        private BuildListener listener;

        SonarCubeAnalysisExecutor(final NWDIBuild nwdiBuild, final AntHelper antHelper, final SonarPomGenerator pomGenerator,
            final MavenInstallation maven, final Launcher launcher, final BuildListener listener) {
            this.nwdiBuild = nwdiBuild;
            this.antHelper = antHelper;
            this.pomGenerator = pomGenerator;
            this.maven = maven;
            this.launcher = launcher;
            this.listener = listener;
        }

        boolean execute(DevelopmentComponent component, final SonarLanguage sonarLanguage) throws InterruptedException {
            // FIXME: get JVM options from configuration.
            final String jvmOptions = "";
            final String properties = "";
            final String pomLocation =
                String.format("%s/sonar-pom%s.xml", antHelper.getBaseLocation(component), sonarLanguage.getProjectSuffix());
            boolean result = false;

            try {
                pomGenerator.execute(component, new FileWriter(pomLocation), sonarLanguage);

                result = new Maven(sonarLanguage.getMavenTargets(), maven.getName(), pomLocation, properties, jvmOptions).perform(nwdiBuild,
                    launcher, listener);
            }
            catch (IOException ioe) {
                Logger.getLogger("NWDI-Sonar-Plugin").warning(String.format("Could not create %s:\n%s", pomLocation, ioe.getMessage()));
            }

            return result;
        }
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
