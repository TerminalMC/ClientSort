package util

class StaticUtil {
    /**
     * Converts a lowercase mod loader name into its formal version.
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
     @returns the latest changelog from the file, verified to match the version.
     */
    static String versionChangelog(File file, String version) {
        final lines = file.readLines()
        final builder = new StringBuilder()
        // Changelog version header is on the third line of the file; check it
        if (version != lines.get(2).substring(3)) {
            throw new IllegalArgumentException(String.format(
                    "Mod version '%s' does not match changelog version '%s'",
                    version,
                    lines.get(2).substring(3)
            ))
        } else {
            // Iterate over content lines
            for (int i = 4; i < lines.size(); i++) {
                final line = lines.get(i)
                if (line.startsWith("## ")) {
                    // Encountered next changelog header; finish
                    break
                } else {
                    // Append the content line, respecting blank lines
                    if (!builder.isEmpty()) builder.append("\n")
                    if (!line.isBlank()) builder.append(line)
                }
            }
        }
        return builder.toString()
    }
}
