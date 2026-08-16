package util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern

class StaticUtil {
    /**
     Converts a lowercase mod loader name into its formal version.
     */
    static String capsLoader(String loader) {
        switch (loader) {
            case "fabric": return "Fabric"
            case "quilt": return "Quilt"
            case "forge": return "Forge"
            case "neoforge": return "NeoForge"
            default: return loader
        }
    }

    /**
     Throws {@link IllegalArgumentException} if the specified date does not represent the current
     date in ISO-8601 form (YYYY-mm-dd).
     */
    static void validateChangelogDate(String date) throws IllegalArgumentException {
        final String currentDate = LocalDate.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (date != currentDate) {
            throw new IllegalArgumentException(String.format(
                    "Changelog date '%s' does not match current date '%s'",
                    date,
                    currentDate
            ))
        }
    }

    /**
     @returns the latest changelog from the file, verified to match the version.
     */
    static Changelog versionChangelog(File file, String version) {
        final Iterator<String> lines = file.readLines().iterator()

        while (lines.hasNext()) {
            final String line = lines.next()
            if (line.startsWith("##")) {
                if (line != "## Unreleased") {
                    throw new IllegalArgumentException("First H2 in changelog must be 'Unreleased'")
                }
                break
            }
        }

        final Pattern pattern = Pattern.compile("## ((?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:-(?:alpha|beta)\\.(?:0|[1-9]\\d*))?) \\[(\\d{4}-\\d{2}-\\d{2})]")
        while (lines.hasNext()) {
            String line = lines.next()
            if (line.startsWith("##")) {
                final Matcher matcher = pattern.matcher(line)
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Second H2 in changelog must be a version and date of the form '<major>.<minecraft>.<minor>[-<alpha|beta>.<build>] \\[yyyy-MM-dd\\]'")
                }

                if (matcher.group(1) != version) {
                    throw new IllegalArgumentException(String.format(
                            "Changelog version '%s' does not match build version '%s'",
                            matcher.group(1),
                            version
                    ))
                }

                final List<String> changelist = new ArrayList()
                while (lines.hasNext()) {
                    line = lines.next()
                    if (line.startsWith("##")) {
                        break
                    } else if (!line.isBlank()) {
                        changelist.add(line)
                    }
                }

                return new Changelog(String.join("\n", changelist), matcher.group(2))
            }
        }

        throw new IllegalArgumentException("Changelog must contain H2 with 'Unreleased' followed by H2 with version and date")
    }

    static class Changelog {
        final String changelist
        final String date

        Changelog(String changelist, String date) {
            this.changelist = changelist
            this.date = date
        }
    }
}
