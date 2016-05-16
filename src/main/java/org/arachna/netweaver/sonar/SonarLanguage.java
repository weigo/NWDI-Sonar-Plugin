/**
 * 
 */
package org.arachna.netweaver.sonar;

import java.util.ArrayList;
import java.util.Collection;

import org.arachna.netweaver.dc.types.DevelopmentComponent;

/**
 * Enumeration of languages supported by SonarQube that are relevant for analysis of SAP NetWeaver development components.
 * 
 * @author Dirk Weigenand
 */
public enum SonarLanguage {
    /**
     * Java analsysis.
     */
    JAVA("java", "", "test sonar:sonar"),
    /**
     * JavaScript analsysis.
     */
    JAVA_SCRIPT("js", "-JS"),

    /**
     * Web project analysis.
     */
    WEB("web", "-WEB");

    /**
     * value to assign to <code>sonar.language</code> for a specific analysis.
     */
    private final String key;

    /**
     * analysis profile specific suffix.
     */
    private final String profileSuffix;

    /**
     * Maven targets to invoke for this language analysis.
     */
    private final String mavenTargets;

    /**
     * Constructor. Create a specific analysis profile descriptor.
     * 
     * @param key
     *            sonar language key.
     * @param profileSuffix
     *            suffix to use for profile.
     * @param mavenTargets
     *            maven targets to run for analysis.
     */
    private SonarLanguage(final String key, final String projectSuffix, final String mavenTargets) {
        this.key = key;
        this.profileSuffix = projectSuffix;
        this.mavenTargets = mavenTargets;
    }

    /**
     * Constructor. Create a specific analysis profile descriptor.
     * 
     * @param key
     *            sonar language key.
     * @param profileSuffix
     *            suffix to use for profile.
     */
    private SonarLanguage(final String key, final String projectSuffix) {
        this(key, projectSuffix, "sonar:sonar");
    }

    /**
     * Get sonar language key.
     * 
     * @return The sonar language key to use for analysis.
     */
    public String getKey() {
        return key;
    }

    /**
     * Suffix to use to identify a specific analysis profile/project.
     * 
     * @return suffix to use for development components when generating an analysis.
     */
    public String getProjectSuffix() {
        return profileSuffix;
    }

    public String getMavenTargets() {
        return mavenTargets;
    }

    /**
     * Determine the SonarQube languages to analyze for this particular development component.
     * 
     * @param component
     *            development component to determine SonarQube analysis languages for.
     * @return a list of SonarQube analysis languages to execute on the given DC, probably empty.
     */
    public static Collection<SonarLanguage> getSonarLanguages(DevelopmentComponent component) {
        Collection<SonarLanguage> languages = new ArrayList<SonarLanguage>();

        switch (component.getType()) {
            case J2EEEjbModule:
            case Java:
            case WebDynpro:
            case WebServicesDeployableProxy:
                languages.add(JAVA);
                break;

            case J2EEWebModule:
            case PortalApplicationModule:
            case PortalApplicationStandalone:
                languages.add(JAVA);
                languages.add(JAVA_SCRIPT);
                languages.add(WEB);

                break;
            default:
                break;

        }

        return languages;
    }
}
