# Soulbound Dolls User Guide

Soulbound Dolls is a NeoForge 1.21.1 mod that creates player-bound dolls for players who join a world or server. Each doll stores a player profile and can be placed as a small decorative entity.

## What It Adds

- `Player Doll`: a craftable, stackable doll item that can bind to a player profile.
- Dynamic player-skin item rendering for bound `Player Doll` stacks, with larger inventory/hand display transforms and a static fallback model when no bound skin data is available.
- `Doll Catalog`: a creative-tab item reserved as a collection/catalog surface.
- `Soulbound Dolls` creative tab.
- A placed `Player Doll` entity with sitting, standing, and cute idle poses.
- Server-side known-player registry saved in world data.
- Admin command `/sbdoll` for listing, giving, and refreshing known player dolls.
- Common config file `soulbound_dolls-common.toml`.

## Basic Player Flow

1. Join the world/server.
2. The mod records your UUID, name, and any available skin texture data.
3. If `autoGiveOwnDoll` is enabled, you receive your own bound doll when logging in, unless you already have one in inventory.
4. Craft or receive a `Player Doll`.
5. Right-click a block with the doll to place it.

If the doll item is unbound, the first placement binds it to the placing player before spawning the entity.

## Crafting

Craft a blank `Player Doll` with:

```text
W S W
P E P
W W W
```

- `W`: any wool
- `S`: string
- `P`: paper
- `E`: ender pearl

The crafted item starts unbound and stacks up to 16. When placed, the spawned doll binds to the placing player; remaining unbound items in the stack stay unbound.

## Doll Interactions

After placing a doll:

- Placed dolls have three poses: sitting, standing, and cute idle. The default pose is sitting.
- Empty-hand right-click: pat the doll. It plays a chime and, if enabled, emits heart particles.
- Plain right-click while holding an item: cycle the doll pose between sitting, standing, and cute idle, with an actionbar prompt.
- Shift + empty-hand right-click: pick up the doll and preserve its bound player profile.
- Shift + right-click while holding an item: shake the doll.
- Middle-click in creative mode: copy a bound `Player Doll` item for the placed doll.
- Left-click: shake the doll without damaging it.
- Shift + left-click: pick up the doll with the same permission rules as shift + empty-hand right-click.

Pickup is creator-only by default. Server config can allow anyone to pick up placed dolls.

## Skin Behavior

- The mod stores skin texture data when available from the player's profile.
- The placed doll model uses player-skin UVs and renders as a Q-style doll when skin data is available.
- If no skin is available, the doll uses the built-in fallback texture.
- The `Player Doll` item uses dynamic player-skin rendering when its ItemStack has bound skin data; otherwise, it uses the static fallback item model.
- Bound dolls for the same player UUID can stack together up to 16. Dolls bound to different players do not merge into the same stack.
- Server-side online skin refresh can be enabled or disabled by config.

## Admin Commands

All `/sbdoll` commands require permission level 2.

```text
/sbdoll list
```

Lists known players recorded by the server.

```text
/sbdoll give <target> <prototype>
```

Gives `<target>` a doll bound to a known player. `<prototype>` can be a known player name or UUID.

Example:

```text
/sbdoll give Steve Alex
```

```text
/sbdoll refresh <prototype>
```

Attempts to refresh skin data for a known player. `<prototype>` can be a known player name or UUID.

Example:

```text
/sbdoll refresh Alex
```

## Config

Config file:

```text
config/soulbound_dolls-common.toml
```

Options:

- `autoGiveOwnDoll = true`: give players their own doll on login if they do not already have one.
- `enableOnlineSkinRefresh = true`: refresh known player skin textures from Mojang services.
- `allowPatParticles = true`: allow pat interactions to emit heart particles.
- `allowPickupByAnyone = false`: allow any player to pick up placed dolls. If false, only the creator can pick them up.

## Testing Checklist

For a first in-game test:

1. Launch a NeoForge 1.21.1 instance with the jar in `mods`.
2. Create or enter a world.
3. Confirm `Soulbound Dolls` appears in the mod list.
4. Join as a player and check whether a bound doll is auto-given.
5. Craft a blank `Player Doll` and place it.
6. Verify the placed doll can be patted, pose-cycled with a held item, shaken while sneaking with a held item, and picked up while sneaking empty-handed.
7. Verify middle-click copying, left-click shaking, shift-left-click pickup, and stack behavior for unbound/same-bound/different-bound dolls.
8. Run `/sbdoll list` as an operator.
9. Run `/sbdoll give <target> <known-player>` and place the result.
10. Run `/sbdoll refresh <known-player>` and watch for success/failure feedback.

## Current Limitations

- The placed doll uses a Q-style player-skin model with sitting, standing, and cute idle poses.
- Skin rendering depends on whether texture data is available or refresh succeeds.
- Dynamic `Player Doll` item rendering is available for bound stacks with skin data. GUI and hand display transforms have been enlarged, but final visual polish still depends on in-game feedback.
- The `Doll Catalog` item is registered but does not yet open a full catalog UI.
- There are no in-game GameTest tests yet; final behavior should be confirmed manually in a real client/server session.
