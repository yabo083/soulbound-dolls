package com.yabo.soulbounddolls.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DollSkinDiagnosticsTest {
    @Test
    void profileSummaryRedactsSkinPayload() {
        PlayerDollProfile profile = PlayerDollProfile.of(
                UUID.fromString("12345678-1234-1234-1234-123456789abc"),
                "Alex",
                "secret-skin-payload",
                "secret-signature-payload",
                true,
                42L);

        String summary = DollSkinDiagnostics.profileSummary(profile);

        assertEquals("uuid=12345678 name=Alex hasSkin=true skin=len19/hashfa72113b sig=len24/hash7791e79c slim=true updated=42", summary);
        assertFalse(summary.contains("secret-skin-payload"));
        assertFalse(summary.contains("secret-signature-payload"));
    }

    @Test
    void textureSummaryIncludesLocationOnly() {
        assertEquals(
                "texture=minecraft:skins/alex",
                DollSkinDiagnostics.textureSummary(ResourceLocation.withDefaultNamespace("skins/alex")));
    }
}
