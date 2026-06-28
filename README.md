# Stormie's Spiders 🕷️
This mod is a fork of [Nyf's Spiders](https://github.com/Nyfaria/NyfsSpiders)
and [Spiders 2.0](https://github.com/TheCyberBrick/Spiders-2.0) 
leveraging Architectury Loom for multiloader support. 

---

Enhances spider's pathfinding and allows them to crawl realistically up walls and even across ceilings! **Compatible with most resource packs and mods.**

*Mod is required on both clients and servers for expected functionality.*

---

### Additions
- Updated for newer versions
- Readded spiders unable to climb during rain feature from Spiders 2.0 (experimental)
- Added config screen (*Mod Menu recommended to change clientside settings on Fabric*)
- Added option to **disable crawling** on ceilings
- Added orientation fallback on servers without the mod (so spiders still rotate)
- Added **disabled blocks** config/menu (for making traps, spider-proof bases, etc)
    - By default Spiders will not be able to climb over trapdoors. Any block can be added here
- Spiders now **avoid** water while chasing target
- If in water, spiders will attempt to swim back to shore
- Reduced spider attack range (togglable)
- Toggle to disable spider swimming (they will just sink in water)
- (3.3.0+) Added a **Rotation Overrides** config menu that allows applying spider rotations to
any mob desired (note, only adds rotations, no pathfinding changes)
- (3.3.0+) Via Rotation Overrides menu, spider entities can now be **disabled** so that the mod doesn't effect
them anymore (useful for disabling certain broken modded monsters that break when using this mod)
### Fixes
- TPS timeout crashes (if you have any TPS related issues, 
[please let me know I would like to fix them if possible!](https://discord.gg/7uttzPbTGq))
- Spiders randomly pausing while in chase
- Stuck detection is now more robust (spiders can get unstuck easier)
- Mildly improved performance while pathfinding
- Spiders are now able to climb up/down stairs
### Compatibility
- **Valkyrien Skies 2 compatibility**
- **Sable/Aeronautics compatibility** (not perfect, spiders cannot climb up walls 
reliably but still a huge improvement over vanilla) *(1.21.1)*
#### Incompatibilities
- **PotsandMimics** (works but crashes have been reported during gameplay)

*While with the fallback, spiders do rotate without needing the mod on the server, it is still highly recommended to have it on the server for the best experience.*

*Trapdoors are disabled by default in the disabled blocks config.*

---

## Contact
For any questions/issues/ideas, feel free to contact me on [Discord](https://discord.gg/7uttzPbTGq) 
or [Matrix](https://matrix.to/#/#stormcommunity:matrix.org) :)

---

### Actively Supported Versions
**1.20.1, 1.21.1, 1.21.10, 1.21.11, 26.1, 26.2+**

---

<img width="1080" height="1080" alt="stormiespiders" src="https://github.com/user-attachments/assets/4a5eb0f3-dca6-4a4e-a3f8-546edff721d6" />
