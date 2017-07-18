/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.util.io.FileFinder;

/**
 * Generator for pom.xml files with dependencies for sonar configured.
 * 
 * @author Dirk Weigenand
 */
public class SonarPomGenerator {
    /**
     * template engine for generating pom files.
     */
    private final VelocityEngine engine;
    private final AntHelper antHelper;
    private final DevelopmentComponentFactory dcFactory;
    private final long buildNumber;

    private final ExcludesFactory excludesFactory = new ExcludesFactory();

    SonarPomGenerator(final AntHelper antHelper, final DevelopmentComponentFactory dcFactory, final VelocityEngine engine,
        final long buildNumber) {
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.engine = engine;
        this.buildNumber = buildNumber;
    }

    void execute(final DevelopmentComponent component, final Writer writer) throws IOException {
        engine.evaluate(createContext(component), writer, "", getTemplate());
        writer.close();
    }

    /**
     * @return
     */
    private Reader getTemplate() {
        return new InputStreamReader(getClass().getResourceAsStream("/org/arachna/netweaver/sonar/pom.vm"));
    }

    private Context createContext(final DevelopmentComponent component) {
        final Context context = new VelocityContext();
        context.put("component", component);
        context.put("groupId", getGroupId(component));
        context.put("artifactId", getArtifactId(component));
        context.put("targetFolder", component.getOutputFolder());
        context.put("sonarExclusions", createExclusions(component));
        context.put("sonarSources", createSonarSources(component));
        context.put("sources", antHelper.createSourceFileSets(component));
        context.put("testSources", component.getTestSourceFolders());
        context.put("resources", antHelper.createResourceFileSets(component));
        context.put("buildNumber", buildNumber);
        context.put("dcName", component.getCompartment().getDevelopmentConfiguration().getName());

        final DevelopmentConfiguration config = component.getCompartment().getDevelopmentConfiguration();
        context.put("targetVersion", config.getSourceVersion());
        context.put("dependencies", createClassPath(component));

        return context;
    }

    private String createSonarSources(DevelopmentComponent component) {
    	
    	DevelopmentComponentType componentType = component.getType();
    	if (DevelopmentComponentType.J2EEEjbModule.equals(componentType)) {
    		return "ejbModule";
    	}
    	
    	if (DevelopmentComponentType.J2EEWebModule.equals(componentType)) {
    		return "WebContent,source";
    	}

    	if (DevelopmentComponentType.J2EE.equals(componentType)) {
    		return "source";
    	}
    	
		return "";
	}

	/**
     * Create the 'sonar.exclusions' property to a comma separated list of files to exclude from analysis.
     * 
     * @param component
     *            development component to generate exclusions for.
     * @return comma separated list of exclusions for the given development component.
     */
    private String createExclusions(final DevelopmentComponent component) {
        return StringUtils.join(excludesFactory.create(component, Collections.<String> emptyList()), ',');
    }

    private String getGroupId(final DevelopmentComponent component) {
        return String.format("%s.%s", component.getCompartment().getDevelopmentConfiguration().getName(), component.getCompartment()
            .getName());
    }

    private String getArtifactId(final DevelopmentComponent component) {
        return component.getNormalizedName(".").replace('~', '.');
    }

    public Collection<DependencyDto> createClassPath(final DevelopmentComponent component) {
        final Map<String, DependencyDto> dependencies = new HashMap<String, DependencyDto>();

        for (final PublicPartReference ppRef : component.getUsedDevelopmentComponents()) {
            final DevelopmentComponent referencedDC = dcFactory.get(ppRef);

            if (referencedDC != null) {
                final File baseDir = new File(antHelper.getBaseLocation(referencedDC, ppRef.getName()));

                if (baseDir.exists()) {
                    final FileFinder finder = new FileFinder(baseDir, ".*\\.jar");
                    DependencyDto dependency = dependencies.get(getGroupId(referencedDC));

                    if (dependency == null) {
                        dependency = new DependencyDto(getGroupId(referencedDC), getArtifactId(referencedDC));
                        dependencies.put(dependency.getGroupId(), dependency);
                    }

                    for (final File path : finder.find()) {
                        dependency.addPath(path);
                    }
                }
            }
        }

        return dependencies.values();
    }

    public class Path {
        private final String path;
        private final String name;

        Path(final File path) {
            this.path = path.getAbsolutePath();
            name = path.getName().replaceAll("~", "-");
        }

        /**
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public int hashCode() {
            return path.hashCode();
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Path)) {
                return false;
            }
            final Path other = (Path)obj;

            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            }
            else if (!path.equals(other.path)) {
                return false;
            }
            return true;
        }

        private SonarPomGenerator getOuterType() {
            return SonarPomGenerator.this;
        }
    }

    public class DependencyDto {
        private final Collection<Path> paths = new HashSet<Path>();
        private final String groupId;
        private final String artifactId;

        /**
         * {@inheritdoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + artifactId.hashCode();
            result = prime * result + groupId.hashCode();
            return result;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof DependencyDto)) {
                return false;
            }
            final DependencyDto other = (DependencyDto)obj;

            if (artifactId == null) {
                if (other.artifactId != null) {
                    return false;
                }
            }
            else if (!artifactId.equals(other.artifactId)) {
                return false;
            }
            if (groupId == null) {
                if (other.groupId != null) {
                    return false;
                }
            }
            else if (!groupId.equals(other.groupId)) {
                return false;
            }
            return true;
        }

        DependencyDto(final String groupId, final String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        void addPath(final File path) {
            paths.add(new Path(path));
        }

        /**
         * @return the path
         */
        public Collection<Path> getPaths() {
            return paths;
        }

        /**
         * @return the groupId
         */
        public String getGroupId() {
            return groupId;
        }

        /**
         * @return the artifactId
         */
        public String getArtifactId() {
            return artifactId;
        }

        public String getArtifactId(final Path path) {
            return artifactId + "-" + path.getName();
        }
    }
}
