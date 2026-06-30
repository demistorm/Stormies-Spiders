### Version 3.3.1 (1.21.1)
- Added version check to Sable compat, requires Sable Companion 1.6.0+
- Fixes sable compat related crashes when version was incorrect

## Version 3.3.0
#### Fixes:
- Spiders can go down stairs properly now
- Spiders no longer stick *at all* to disabled blocks
- Clumping in groups no long occurs to that extreme (goodbye thousand legged monster lol)
- Fixed scrollbar in Disabled Blocks menu (1.20.1)
#### Additions:
- Functional enough Sable/Aeronautics support (1.21.1)
- Can now apply 3d rotations to any mob (not pathfinding though) via the new **Rotation Overrides** config menu
- Broken modded spider mobs can now be **disabled** from this mod's effects via the Rotation Overrides menu
- **26.2** support :D

## Version 3.2.0
#### Fixes:
- Added loaded chunks check to prevent timeout crashes (might fix TPS issues?)
- Spiders no longer randomly pause while in chase
- Spiders are now able to swim
- Valkyrien Skies 2 compatibility
- Improved pathfinding performance
#### Additions: 
- Spiders now jump when stuck to free themselves
- Reduced spider attack range (can be toggled off in config)
- Added toggle to disable spider swimming
- New "Extras" config screen for Spider swimming and reduced spider range toggles
- Touched up disabled blocks config screen
- Spiders swim to nearest shore when in water
- Spiders now avoid water when chasing target
- 1.20.1 and 26.1 support

## Version 3.1.0
- Added vanilla server fallback, so now rotations work even without the mod on the server! (Note: They however look better if the mod is also on the server)
- Added disabled blocks menu/config, spiders will be unable to climb over blocks in this config (trapdoors enabled by default as an example)
- Fixed player models rotating in various ways when spiders are present on (1.21.4-1.21.8)
- Moved all configs to the same folder