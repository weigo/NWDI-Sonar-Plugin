/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;

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

    SonarPomGenerator(final AntHelper antHelper, final VelocityEngine engine) {
        this.antHelper = antHelper;
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
        final List<String> sources = new ArrayList<String>(antHelper.createSourceFileSets(component));
        context.put("source", sources.get(0));

        if (sources.size() > 1) {
            context.put("sources", sources.subList(1, sources.size()));
        }

        final DevelopmentConfiguration config = component.getCompartment().getDevelopmentConfiguration();
        context.put("targetVersion", config.getSourceVersion());

        return context;
    }

    private String getGroupId(final DevelopmentComponent component) {
        return String.format("%s.%s", component.getCompartment().getDevelopmentConfiguration().getName(), component.getCompartment()
            .getName());
    }

    private String getArtifactId(final DevelopmentComponent component) {
        return component.getNormalizedName(".").replace('~', '.');
    }
}
