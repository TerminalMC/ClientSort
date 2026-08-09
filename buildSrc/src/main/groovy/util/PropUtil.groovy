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
     Applies configured dependencies for a subproject.
     @param subprojectName the subproject name.
     @param selector the dependency configuration selector.
     */
    void applyDependencies(
            String subprojectName,
            BiFunction<String, Object, Closure<ExternalModuleDependency>> selector
    ) {
        safeList("${subprojectName}_deps").each { String dep ->
            try {
                final String[] depData = list("d_${subprojectName}_${dep}")
                if (depData[0] != "-") {
                    final String[] mavenData = depData[0].split(":")
                    final String mavenGroup = mavenData[3]
                    final String mavenProject = mavenData[4]
                    final String subVersion = ((mavenData.length > 6 && mavenData[6] != "-")
                            ? project.property(mavenData[6])
                            : project.property("v_${dep}")).toString()
                    final String mavenVersion = "${mavenData[5]}".replace("\$v", subVersion)
                    String gradleDep = "${mavenGroup}:${mavenProject}:${mavenVersion}"
                    for (int i = 2; i >= 0; i--) {
                        gradleDep = selector.apply(mavenData[i], gradleDep)
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing Gradle dependency '${dep}' for subproject "
                        + "'${subprojectName}'. Check dependency property format.")
                throw e
            }
        }
    }
}
