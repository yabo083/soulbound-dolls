# Zombie Doll And Ender Mask Design

## Goal

Finish the expanded 0.1.2 a/d behavior:

- Zombies can break placed player dolls, pick up dropped player doll items, carry at most three dolls, become sunlight-immune while carrying any doll, and always drop carried dolls on death.
- Players wearing a player doll on the head or in a Curios slot do not anger Endermen by looking at them; attacking an Enderman still creates normal hostility.
- The doll tooltip communicates the Enderman protection flavor line: `我很可爱，请不要生气~`.

## Existing Context

- `SoulboundDollsRuntimeEvents` already owns runtime behavior for login auto-give, phantom repel, and undead doll attraction.
- `PlayerDollEntity` is the placed doll entity and can be picked up by players into a bound doll item.
- `PlayerDollItem` is already equipable in the head slot through `Equipable` when `allowDollAsHelmet` is enabled.
- `SoulboundDollsConfig` already has 0.1.2 feature switches for helmet, throw, undead attraction, and phantom repel.
- NeoForge exposes `EnderManAngerEvent`, which cancels Enderman targeting caused by looking. Active attacks still use normal target events and should not be canceled.

## Optional Dependency Policy

All third-party mod integrations are optional. Jade, Curios, and any future compatibility target must never become a required dependency:

- Gradle dependencies for third-party mod APIs must be `compileOnly` only.
- `neoforge.mods.toml` entries for third-party mods must use `type = "optional"`.
- Runtime code must check whether the target mod is loaded before touching its API.
- API imports must stay isolated in compat classes so the base mod loads when the other mod is absent.
- Do not add `implementation`, `runtimeOnly`, bundled jars, or required dependency metadata for Jade, Curios, or other optional integration mods.

## Zombie Doll Behavior

Scope is zombie-type mobs only, not all undead. Implement for `Zombie` and subclasses such as zombie villagers and husks.

Zombie interaction rules:

- A zombie can carry at most three bound player dolls.
- A zombie never overwrites existing vanilla/modded equipment.
- If the head slot is empty, the first carried doll is equipped there and marked as guaranteed drop.
- If the main hand is empty, the next carried doll is equipped there and marked as guaranteed drop.
- If the offhand is empty, the next carried doll is equipped there and marked as guaranteed drop.
- If all visible slots are occupied, additional carried dolls are stored in persistent entity NBT, up to the same total cap of three.
- Any zombie with at least one carried doll is immune to sunlight burning.
- On death, all carried dolls are guaranteed to drop. Visible equipment uses guaranteed drop chance; internally stored dolls are appended to `LivingDropsEvent`.

Placed-doll rules:

- When a zombie has room and reaches a nearby `PlayerDollEntity`, it removes that entity and converts it to the same bound doll stack players would receive.
- This should be implemented as a zombie AI goal added from `EntityJoinLevelEvent`, matching the existing event-driven style.
- The goal should reuse the existing undead attraction range config unless a separate config is added later.

Dropped-item rules:

- When a zombie has room and sees a nearby dropped player doll `ItemEntity`, it pathfinds toward the item.
- On close range, it consumes one doll item from the item entity and carries it.
- This behavior should not rely on player-only item pickup events.

## Ender Mask Behavior

Enderman look protection uses `EnderManAngerEvent`:

- If the event player wears a player doll in the head equipment slot, cancel the event.
- If Curios is present and the player has a player doll in any Curios slot, cancel the event.
- If Curios is absent, the helper returns false without loading Curios classes.
- Do not cancel `LivingChangeTargetEvent` for Endermen. Active attacks and damage retaliation must still work.

Curios integration should be optional:

- Add a compile-only Curios API dependency if available through configured repositories.
- Keep Curios-specific imports isolated in a `compat.curios` class.
- Check `ModList.get().isLoaded("curios")` before invoking Curios APIs.
- Declare Curios as optional in `neoforge.mods.toml` only if metadata is needed; never mark it required.

## Tooltip Behavior

Bound or unbound player dolls should include this flavor/effect tooltip when Enderman protection is enabled:

- `我很可爱，请不要生气~`

Add English and Chinese translation keys. The Chinese line should match exactly.

## Testing Strategy

Use small pure helpers for behavior that can be tested without a full Minecraft world:

- Doll stack recognition helper: detects `SoulboundDollsComponents.PLAYER_DOLL_PROFILE`.
- Zombie carry helper: counts visible and stored dolls, respects the cap, chooses the first empty visible slot, and never replaces existing equipment.
- Ender mask helper: detects head-slot doll; Curios path can be tested by injecting a predicate rather than loading Curios.

Use runtime events as thin adapters over helpers. This keeps tests fast and avoids bootstrapping worlds.

## Non-Goals

- Do not create a custom zombie entity.
- Do not override or replace existing zombie equipment.
- Do not add per-tick global scans outside normal AI goals.
- Do not cancel Enderman retaliation after the player attacks.
- Do not require Curios at runtime.
- Do not require Jade, Curios, or any other integration mod at runtime.
