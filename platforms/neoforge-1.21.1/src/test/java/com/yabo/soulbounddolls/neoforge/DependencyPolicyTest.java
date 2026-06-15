package com.yabo.soulbounddolls.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DependencyPolicyTest {
    private static final Pattern DEPENDENCY_BLOCK = Pattern.compile("(?ms)^\\s*\\[\\[dependencies\\.[^\\]]+\\]\\]\\s*(.*?)(?=^\\s*\\[\\[|\\z)");
    private static final Pattern FORBIDDEN_GRADLE_DEPENDENCY = Pattern.compile("(?ms)^\\s*(implementation|runtimeOnly|jarJar)\\s*(?:\\((.*?)\\)|([^\\r\\n]*))");
    private static final Pattern DIRECT_CURIOS_IMPORT = Pattern.compile("(?m)^\\s*import\\s+top\\.theillusivec4\\.curios(?:\\.|;)");

    @Test
    void integrationDependencyMetadataIsNeverRequired() throws IOException {
        Set<String> knownIntegrationMods = knownIntegrationModIds();
        Set<String> requiredIntegrationMods = dependencyBlocks(readModuleFile("src/main/resources/META-INF/neoforge.mods.toml"))
                .stream()
                .filter(block -> "required".equals(block.type()))
                .map(DependencyBlock::modId)
                .filter(knownIntegrationMods::contains)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(Set.of(), requiredIntegrationMods, "Integration mods must not be required dependencies");
    }

    @Test
    void jadeDependencyMetadataRemainsOptional() throws IOException {
        List<DependencyBlock> jadeBlocks = dependencyBlocks(readModuleFile("src/main/resources/META-INF/neoforge.mods.toml"))
                .stream()
                .filter(block -> "jade".equals(block.modId()))
                .toList();

        assertEquals(1, jadeBlocks.size(), "Jade metadata should stay declared once");
        assertEquals("optional", jadeBlocks.getFirst().type(), "Jade metadata must remain optional");
    }

    @Test
    void gradleDoesNotUseBundlingOrRuntimeConfigurationsForIntegrations() throws IOException {
        Set<String> knownIntegrationMods = knownIntegrationModIds();
        List<String> forbiddenDeclarations = forbiddenIntegrationDependencyDeclarations(readModuleFile("build.gradle"), knownIntegrationMods);

        assertEquals(List.of(), forbiddenDeclarations, "Use compileOnly/reflection for optional integrations, not implementation/runtimeOnly/jarJar");
    }

    @Test
    void curiosLookupDoesNotDirectlyImportCuriosApi() throws IOException {
        String curiosLookup = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/compat/curios/CuriosDollLookup.java");

        assertFalse(DIRECT_CURIOS_IMPORT.matcher(curiosLookup).find(), "Curios integration must use reflection instead of direct imports");
    }

    @Test
    void runtimeEventsDoNotUseGlobalEntityTickMaintenanceForZombieDolls() throws IOException {
        String runtimeEvents = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java");

        assertFalse(runtimeEvents.contains("EntityTickEvent"), "Zombie doll behavior should stay event-driven, not global entity-tick maintenance");
    }

    @Test
    void curiosLookupReflectsLivingEntityInventoryMethod() throws IOException {
        String curiosLookup = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/compat/curios/CuriosDollLookup.java");

        assertFalse(curiosLookup.contains("getMethod(\"getCuriosInventory\", Player.class)"),
                "Curios 1.21.1 exposes getCuriosInventory(LivingEntity), not Player");
        assertTrue(curiosLookup.contains("getMethod(\"getCuriosInventory\", LivingEntity.class)"),
                "Curios lookup must reflect getCuriosInventory(LivingEntity.class)");
    }

    @Test
    void curiosItemTagUsesMinecraftItemTagDirectory() {
        assertTrue(Files.isRegularFile(modulePath("src/main/resources/data/curios/tags/item/head.json")),
                "Curios head item tag must live under tags/item for Minecraft 1.21.1");
        assertFalse(Files.exists(modulePath("src/main/resources/data/curios/tags/items/head.json")),
                "Plural tags/items is ignored by Minecraft 1.21.1 item tags");
    }

    private static List<DependencyBlock> dependencyBlocks(String toml) {
        Matcher matcher = DEPENDENCY_BLOCK.matcher(toml);
        List<DependencyBlock> blocks = new ArrayList<>();
        while (matcher.find()) {
            String block = matcher.group(1);
            blocks.add(new DependencyBlock(fieldValue(block, "modId"), fieldValue(block, "type")));
        }
        return blocks;
    }

    private static String fieldValue(String block, String fieldName) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(fieldName) + "\\s*=\\s*\"([^\"]+)\"").matcher(block);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<String> forbiddenIntegrationDependencyDeclarations(String buildFile, Set<String> integrationModIds) {
        Matcher matcher = FORBIDDEN_GRADLE_DEPENDENCY.matcher(stripGradleComments(buildFile));
        List<String> declarations = new ArrayList<>();
        while (matcher.find()) {
            String configuration = matcher.group(1);
            String dependency = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (integrationModIds.stream().anyMatch(modId -> containsDependencyId(dependency, modId))) {
                declarations.add(configuration + " " + dependency.trim());
            }
        }
        return declarations;
    }

    private static boolean containsDependencyId(String dependency, String modId) {
        return Pattern.compile("(?i)(^|[^a-z0-9_-])" + Pattern.quote(modId) + "([^a-z0-9_-]|$)")
                .matcher(dependency)
                .find();
    }

    private static String stripGradleComments(String buildFile) {
        return buildFile.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static Set<String> knownIntegrationModIds() throws IOException {
        try (Stream<Path> entries = Files.list(modulePath("src/main/java/com/yabo/soulbounddolls/neoforge/compat"))) {
            return entries.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static String readModuleFile(String relativePath) throws IOException {
        return Files.readString(modulePath(relativePath), StandardCharsets.UTF_8);
    }

    private static Path modulePath(String relativePath) {
        Path workingDirectory = Path.of("").toAbsolutePath();
        for (Path candidate : List.of(workingDirectory, workingDirectory.resolve("platforms/neoforge-1.21.1"))) {
            if (Files.isRegularFile(candidate.resolve("build.gradle"))
                    && Files.isRegularFile(candidate.resolve("src/main/resources/META-INF/neoforge.mods.toml"))) {
                return candidate.resolve(relativePath);
            }
        }
        throw new IllegalStateException("Cannot locate NeoForge 1.21.1 project directory from " + workingDirectory);
    }

    private record DependencyBlock(String modId, String type) {
    }
}
