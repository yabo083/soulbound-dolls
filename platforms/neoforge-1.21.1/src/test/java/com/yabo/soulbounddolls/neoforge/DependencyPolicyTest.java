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
    void modrinthRepositoryOnlyServesModrinthCoordinates() throws IOException {
        String buildFile = readModuleFile("build.gradle");
        int modrinthRepository = buildFile.indexOf("name = \"Modrinth\"");
        int dependencies = buildFile.indexOf("dependencies {");

        assertTrue(modrinthRepository >= 0, "Modrinth repository should stay explicit for optional integrations");
        assertTrue(dependencies > modrinthRepository, "Dependencies block should follow repositories block");
        String repositoryBlock = buildFile.substring(modrinthRepository, dependencies);

        Matcher includeGroupMatcher = Pattern.compile("includeGroup\\s+\"([^\"]+)\"").matcher(repositoryBlock);
        List<String> includedGroups = new ArrayList<>();
        while (includeGroupMatcher.find()) {
            includedGroups.add(includeGroupMatcher.group(1));
        }
        assertEquals(List.of("maven.modrinth"), includedGroups,
                "Modrinth maven repository should only include maven.modrinth");
    }

    @Test
    void copyJarToTestTargetCanBeConfiguredWithGradleProperty() throws IOException {
        String buildFile = readModuleFile("build.gradle");

        assertTrue(buildFile.contains("testModsDir"),
                "copyJarToTest should support -PtestModsDir=... instead of requiring build file edits");
        assertFalse(buildFile.contains("def copyJarToTestTargetDir = \"E:\\\\SteamLibrary"),
                "Machine-specific default path should not be the only copyJarToTest target source");
    }

    @Test
    void copyJarToTestTargetSuffixCheckOnlyAppliesToDefaultPathMode() throws IOException {
        String buildFile = readModuleFile("build.gradle");

        assertTrue(buildFile.contains("!project.hasProperty(\"testModsDir\")"),
                "Custom test mods directories should bypass the hardcoded Mechanomania suffix guard");
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

    @Test
    void playerDollItemDelegatesNonSneakUseToEquipableSwap() throws IOException {
        String playerDollItem = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/item/PlayerDollItem.java");

        assertTrue(playerDollItem.contains("return swapWithEquipmentSlot(this, level, player, hand);"),
                "Non-sneak use should preserve vanilla right-click helmet equip behavior");
        assertFalse(playerDollItem.contains("return InteractionResultHolder.pass(stack);"),
                "Returning PASS bypasses Item/Equipable right-click equipment swap");
    }

    @Test
    void runtimeEventsDoNotAddDuplicateZombieDollGoals() throws IOException {
        String runtimeEvents = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsRuntimeEvents.java");

        assertTrue(runtimeEvents.contains("hasZombieMoveToDollGoal"),
                "EntityJoinLevelEvent can run on reload, so zombie doll goals must be deduplicated");
        assertTrue(runtimeEvents.contains("getAvailableGoals()"),
                "Deduplication should inspect existing GoalSelector entries before addGoal");
    }

    @Test
    void runtimeEventsConfigTextDoesNotClaimPhantomSpawnPrevention() throws IOException {
        String config = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/SoulboundDollsConfig.java");

        assertFalse(config.contains("preventing them from spawning"),
                "Phantom repel currently cancels targeting, not spawning");
    }

    @Test
    void teleportPacketDoesNotFallbackToUnsafeCenter() throws IOException {
        String packet = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/network/TeleportToDollPlayerPacket.java");

        assertFalse(packet.contains("return center;"),
                "Safe teleport lookup must return null when no validated location exists");
        assertTrue(packet.contains("return null;"),
                "No-safe-location path should remain reachable");
    }

    @Test
    void dollSkinResolveDoesNotComputeDiagnosticsBeforeDebugGuard() throws IOException {
        String manager = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/client/DollSkinManager.java");
        String resolveBody = methodBody(manager, "public ResourceLocation resolve");

        assertFalse(resolveBody.isEmpty(), "resolve() body should be located for the policy check");
        assertFalse(resolveBody.contains("DollSkinDiagnostics.profileSummary"),
                "resolve() is render-hot and should not compute diagnostic summaries before debug is enabled");
        assertTrue(manager.contains("LOGGER.isDebugEnabled()"),
                "Debug diagnostics should be guarded before summaries are computed");
    }

    @Test
    void teleportPacketCapturesOriginBeforeTeleportSound() throws IOException {
        String packet = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/network/TeleportToDollPlayerPacket.java");
        int originIndex = packet.indexOf("BlockPos originPos = sender.blockPosition();");
        int teleportIndex = packet.indexOf("sender.teleportTo(");
        int soundIndex = packet.indexOf("senderLevel.playSound(null, originPos");

        assertTrue(originIndex >= 0 && originIndex < teleportIndex,
                "Origin position must be captured before teleport mutates sender position");
        assertTrue(soundIndex > teleportIndex,
                "Departure sound should use captured origin position after teleport");
    }

    @Test
    void projectileProfileCacheInvalidatesWhenSyncedProfileDataChanges() throws IOException {
        String projectile = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/entity/DollProjectileEntity.java");

        assertTrue(projectile.contains("public void onSyncedDataUpdated(EntityDataAccessor<?> key)"),
                "DollProjectileEntity should invalidate its memoized profile when client synced data updates");
        assertTrue(projectile.contains("profile = null;"),
                "Projectile profile cache should be cleared so getProfile() rebuilds from updated synced data");
    }

    @Test
    void playerDollSyncedProfileRebuildPreservesLastUpdated() throws IOException {
        String entity = readModuleFile("src/main/java/com/yabo/soulbounddolls/neoforge/entity/PlayerDollEntity.java");
        String syncedUpdateBody = methodBody(entity, "public void onSyncedDataUpdated");

        assertTrue(syncedUpdateBody.contains("lastUpdated"),
                "Synced profile rebuild should preserve existing lastUpdated metadata");
        assertFalse(syncedUpdateBody.contains(",\n                    0L);"),
                "Synced profile rebuild should not hardcode lastUpdated to 0L");
    }

    @Test
    void changelogDocumentsEnderMaskAndZombieCarryFeatures() throws IOException {
        String changelog = readRootFile("CHANGELOG.md");

        assertTrue(changelog.contains("Enderman"),
                "Changelog should mention Enderman look protection from worn dolls");
        assertTrue(changelog.contains("up to 3") || changelog.contains("max 3"),
                "Changelog should mention zombies can carry up to 3 dolls");
        assertTrue(changelog.contains("drop"),
                "Changelog should mention carried dolls drop on zombie death");
    }

    @Test
    void projectArchitectureDocumentsEnderMaskAndZombieCarryFeatures() throws IOException {
        String architecture = readRootFile("PROJECT_ARCHITECTURE.md");

        assertTrue(architecture.contains("enableEnderMaskProtection"),
                "Architecture config table should document the Ender mask toggle");
        assertTrue(architecture.contains("最多 3")
                        || architecture.contains("up to 3")
                        || architecture.contains("max 3"),
                "Architecture should document the zombie doll carry cap");
        assertTrue(architecture.contains("Enderman") || architecture.contains("末影人"),
                "Architecture should document Enderman look protection when wearing dolls");
    }

    private static String methodBody(String source, String signatureStart) {
        int signatureIndex = source.indexOf(signatureStart);
        if (signatureIndex < 0) {
            return "";
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, index + 1);
                }
            }
        }
        return source.substring(bodyStart);
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

    private static String readRootFile(String relativePath) throws IOException {
        return Files.readString(rootPath(relativePath), StandardCharsets.UTF_8);
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

    private static Path rootPath(String relativePath) {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))
                    && Files.isDirectory(candidate.resolve("platforms"))) {
                return candidate.resolve(relativePath);
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }

    private record DependencyBlock(String modId, String type) {
    }
}
