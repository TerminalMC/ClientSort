package util

import org.gradle.api.provider.ProviderFactory

class EnvUtil {
    final ProviderFactory providers

    EnvUtil(ProviderFactory providers) {
        this.providers = providers
    }

    /**
     @return {@code true} if the environment variable is set.
     */
    boolean has(String variableName) {
        return !safe(variableName).isBlank()
    }

    /**
     @return the value of the environment variable if it is set, an empty string otherwise.
     */
    String safe(String variableName) {
        return providers.environmentVariable(variableName).getOrElse("")
    }
}
