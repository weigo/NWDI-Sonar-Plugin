/**
 * 
 */
package org.arachna.netweaver.sonar;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.PackageDeclaration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.javaparser.ClassNameResolver;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
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

    private final ExcludesFactory excludesFactory = new ExcludesFactory();

    SonarPomGenerator(final AntHelper antHelper, final DevelopmentComponentFactory dcFactory,
        final VelocityEngine engine) {
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.engine = engine;
    }

    void execute(final DevelopmentComponent component, final Writer writer) throws IOException {
        engine.evaluate(createContext(component), writer, "", getTemplate());
        writer.close();
    }

    /**
     * @return
     */
    private Reader getTemplate() {
        final InputStream resource = getClass().getResourceAsStream("/org/arachna/netweaver/sonar/pom.vm");
        return new InputStreamReader(resource);
    }

    private Context createContext(final DevelopmentComponent component) {
        final Context context = new VelocityContext();
        context.put("component", component);
        context.put("groupId", getGroupId(component));
        context.put("artifactId", getArtifactId(component));
        context.put("targetFolder", component.getOutputFolder());
        context.put("sonarExclusions", createExclusions(component));
        final List<String> sources = new ArrayList<String>(antHelper.createSourceFileSets(component));
        final String testFolder = determineTestFolder(sources);

        if (testFolder != null) {
            sources.remove(testFolder);
            context.put("testSourceDirectory", testFolder);
        }

        context.put("source", sources.get(0));

        if (sources.size() > 1) {
            context.put("sources", sources.subList(1, sources.size()));
        }

        final DevelopmentConfiguration config = component.getCompartment().getDevelopmentConfiguration();
        context.put("targetVersion", config.getSourceVersion());
        context.put("dependencies", createClassPath(component));

        return context;
    }

    /**
     * Create the 'sonar.exclusions' property to a comma separated list of files
     * to exclude from analysis.
     * 
     * @param component
     *            development component to generate exclusions for.
     * @return comma separated list of exclusions for the given development
     *         component.
     */
    private String createExclusions(final DevelopmentComponent component) {
        return StringUtils.join(excludesFactory.create(component, Collections.<String> emptyList()), ',');
    }

    private String determineTestFolder(final List<String> sources) {
        for (final String sourceFolder : sources) {
            final FileFinder finder = new FileFinder(new File(sourceFolder), ".*\\.java");

            for (final File file : finder.find()) {
                try {
                    final CompilationUnit compilationUnit = JavaParser.parse(file, "UTF-8");
                    final PackageDeclaration packageDescriptor = compilationUnit.getPackage();

                    if (packageDescriptor != null) {
                        final TestPackageResolver testPackageResolver =
                            new TestPackageResolver(new ClassNameResolver(packageDescriptor.getName().toString(),
                                compilationUnit.getImports()));
                        compilationUnit.accept(testPackageResolver, null);

                        if (testPackageResolver.isTestFolder()) {
                            return sourceFolder;
                        }
                    }
                }
                catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private String getGroupId(final DevelopmentComponent component) {
        return String.format("%s.%s", component.getCompartment().getDevelopmentConfiguration().getName(), component
            .getCompartment().getName());
    }

    private String getArtifactId(final DevelopmentComponent component) {
        return component.getNormalizedName(".").replace('~', '.');
    }

    public Set<DependencyDto> createClassPath(final DevelopmentComponent component) {
        final Set<DependencyDto> paths = new HashSet<DependencyDto>();

        for (final PublicPartReference ppRef : component.getUsedDevelopmentComponents()) {
            final DevelopmentComponent referencedDC = dcFactory.get(ppRef);

            if (referencedDC != null) {
                final File baseDir = new File(antHelper.getBaseLocation(referencedDC, ppRef.getName()));

                if (baseDir.exists()) {
                    final FileFinder finder = new FileFinder(baseDir, ".*\\.jar");
                    final DependencyDto dependency =
                        new DependencyDto(getGroupId(referencedDC), getArtifactId(referencedDC));

                    for (final File path : finder.find()) {
                        dependency.addPath(path.getAbsolutePath());
                    }

                    paths.add(dependency);
                }
            }
        }

        return paths;
    }

    public class DependencyDto {
        private final Collection<String> paths = new HashSet<String>();
        private final String groupId;
        private final String artifactId;

        DependencyDto(final String groupId, final String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        void addPath(final String path) {
            paths.add(path);
        }

        /**
         * @return the path
         */
        public Collection<String> getPaths() {
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
    }
}
