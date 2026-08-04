<div align="center">
  <h1>PvP Toggle</h1>
  <p>Controlled player-versus-player combat for NeoForge servers.</p>
  <p>
    <a href="https://github.com/Brassworks-smp/PvP-Toggle">Source Code</a>
    ·
    <a href="https://brassworks.opnsoc.org/">Brassworks SMP</a>
  </p>
</div>

PvP Toggle is a lightweight Minecraft mod that gives players control over their PvP status without adding unnecessary complexity. Players can enable or disable PvP, manage exceptions for individual players, and immediately see who can be attacked through compact nametag icons.

The mod is designed for Minecraft 1.21.1 with NeoForge. All core functionality is server-controlled and remains available through commands. The graphical BrassUI management menu is entirely optional.

## Features

- Enable or disable PvP with simple commands.
- Configurable delay before PvP protection becomes active.
- Block or allow PvP with individual players.
- Small nametag icons showing whether another player can be attacked.
- Optional protection against knockback-only attacks and player pushing.
- Administrative commands for forcing player PvP states.
- Configurable feedback when a protected interaction is blocked.
- Persistent PvP data stored separately for each world or server.
- Simple Voice Chat integration that prevents nametag icons from overlapping.
- Optional BrassUI management menu with no required server-side UI dependency.
- Compatibility with legacy PvP Toggle clients using the original icon protocol.

## Optional PvP Management Menu

Run `/pvp menu` to open the optional BrassUI management screen.

The menu provides:

- A compact desktop-style window.
- Live PvP status and pending-protection timer updates.
- A live online-player list that reacts to joins, leaves, and state changes.
- Search and scrolling without replacing or reopening the entire screen.
- Player heads with names, PvP relationships, and available actions on hover.
- Direct controls for enabling PvP, disabling PvP, allowing players, and blocking players.
- Green highlighting for allowed players and red highlighting for blocked or pending states.

For the UI a compatible KotlinForForge installation are required only on clients that want to use this menu. If they are missing, `/pvp menu` displays an availability message while every regular PvP command continues to work.

## Icons

PvP Toggle renders small icons beside player nametags:

- <img src="https://raw.githubusercontent.com/Brassworks-smp/PvP-Toggle/refs/heads/main/src/main/resources/assets/pvptoggle/textures/gui/pvp_on.png" alt="PvP enabled" width="16" height="16"> **PvP On** — The player can be attacked.
- <img src="https://raw.githubusercontent.com/Brassworks-smp/PvP-Toggle/refs/heads/main/src/main/resources/assets/pvptoggle/textures/gui/pvp_off.png" alt="PvP disabled" width="16" height="16"> **PvP Off** — PvP against the player is blocked.

## Player Commands

| Command | Description |
| --- | --- |
| `/pvp on` | Enables PvP immediately. |
| `/pvp off` | Disables PvP after the configured delay. |
| `/pvp block <player>` | Blocks PvP between you and another player. |
| `/pvp unblock <player>` | Allows PvP with a previously blocked player again. |
| `/pvp status` | Shows your current PvP status. |
| `/pvp menu` | Opens the optional BrassUI management menu. |
| `/pvp help` | Lists the available PvP commands. |

## Admin Commands

Administrative commands require permission level 2.

| Command | Description |
| --- | --- |
| `/pvpadmin forceon <player>` | Forces PvP on for a player. |
| `/pvpadmin forceoff <player>` | Forces PvP off for a player. |
| `/pvpadmin reload` | Reloads the stored PvP data. |

## Configuration

PvP Toggle creates a server-side configuration for controlling protection behavior.

| Option | Description | Default |
| --- | --- | --- |
| `one-sided-toggle` | Only protects the player from incoming PvP when `/pvp off` is active. | `false` |
| `take-effect-time` | Delay in minutes before `/pvp off` becomes active. | `10` |
| `cancel-pending-off-on-attack` | Cancels pending protection when the player attacks another player. | `true` |
| `send-action-messages` | Sends feedback when PvP damage, knockback, or pushing is blocked. | `false` |
| `block-knockback` | Blocks knockback-only weapons and effects during protected interactions. | `true` |
| `block-player-pushing` | Prevents protected players from being physically pushed by other players. | `true` |

## Installation

### Server

1. Install NeoForge for Minecraft 1.21.1.
2. Place PvP Toggle in the server's `mods` directory.
3. Start the server once to generate the configuration.

BrassUI and KotlinForForge are not required on the server.

### Client

Install PvP Toggle on the client to display nametag icons.

To use the graphical management menu, additionally install:

- BrassUI
- A BrassUI-compatible KotlinForForge version

Clients without PvP Toggle or without the optional UI dependencies may still connect because the network channels are optional. Legacy clients using protocol version 1 retain icon support.

## Simple Voice Chat Support

When Simple Voice Chat nametag icons are visible, PvP Toggle automatically moves its icons to prevent overlap. The PvP icons return to their normal position when voice chat icons are hidden. Simple Voice Chat remains optional.

## License

PvP Toggle is licensed under the [MIT License](LICENSE). You may use, modify, and redistribute the software under the terms of that license.

## Credits

- Development and design by **DerErneuerer**
- Created for [Brassworks SMP](https://brassworks.opnsoc.org/)

## Links

- [PvP Toggle on GitHub](https://github.com/Brassworks-smp/PvP-Toggle)
- [Brassworks SMP](https://brassworks.opnsoc.org/)
- [BrassUI documentation](https://brassworks-smp.github.io/BrassUi/)
- [Simple Voice Chat on Modrinth](https://modrinth.com/plugin/simple-voice-chat)
