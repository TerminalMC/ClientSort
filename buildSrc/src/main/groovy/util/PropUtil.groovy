package util

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency

import java.util.function.BiFunction

class PropUtil {
    final Project project

    PropUtil(Project project) {
        this.project = project
    }

    /**
     @return {@code true} if the property is set.
     */
    boolean has(String propertyName) {
        return project.hasProperty(propertyName) && !get(propertyName).toString().isBlank()
    }

    /**
     @return the value of the property.
     */
    String get(String propertyName) {
        return project.property(propertyName)
    }

    /**
     @return the value of the property if it exists, an empty string otherwise.
     */
    String safe(String propertyName) {
        return has(propertyName) ? get(propertyName) : ""
    }

    /**
     @return the value of the property, CSV-split.
     */
    String[] list(String propertyName) {
        return get(propertyName).toString().split(",")
                .findAll { !it.isBlank() }
                .collect { it.strip() }
    }

    /**
     @return the value of the property CSV-split if it is set, an empty array otherwise.
     */
    String[] safeList(String propertyName) {
        return has(propertyName) ? list(propertyName) : []
    }

    /**
     * Applies configured dependencies for a subproject.
     * @param pName the subproject name.
     * @param selector the dependency configuration selector.
     */
    void applyDependencies(
            String pName,
            BiFunction<String, Object, Closure<ExternalModuleDependency>> selector
    ) {
        safeList("${pName}_deps").each { dep ->
            try {
                final depData = list("d_${pName}_${dep}")
                if (depData[0] != "-") {
                    final mavenData = depData[0].split(":")
                    final mavenGroup = mavenData[3]
                    final mavenProject = mavenData[4]
                    final subVersion = ((mavenData.length > 6 && mavenData[6] != "-")
                            ? project.property(mavenData[6])
                            : project.property("v_${dep}")).toString()
                    final mavenVersion = "${mavenData[5]}".replace("\$v", subVersion)
                    def gradleDep = "${mavenGroup}:${mavenProject}:${mavenVersion}"
                    for (int i = 2; i >= 0; i--) {
                        gradleDep = selector.apply(mavenData[i], gradleDep)
                    }
                }
            } catch (Exception ex) {
                logger.error("Error processing Gradle dependency '${dep}' for subproject "
                        + "'${pName}'. Check dependency property format.")
                throw ex
            }
        }
    }
}
