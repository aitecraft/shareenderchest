# ShareEnderChest

A simple Fabric mod that provides a shared inventory for all players, accessible using an Ender Chest.

<a href="https://modrinth.com/mod/fabric-api">
    <img alt="Fabric API Required" src="https://i.imgur.com/Ol1Tcf8.png" width="143" height="50">
</a>

---

To function, the mod is required on the server. Additional features are enabled if installed on the client.

There are multiple ways to open the Shared Ender Chest:

1. `Shift`+`Right-click` an Ender Chest block [Sneak + Use]

2. Press `Right-click` with an Ender Chest in your hand [Use Item]

3. `Ctrl`+`Click` or `Alt`+`Click` an Ender Chest item in your inventory

Option 3 requires the mod to be present on both server and client.


Size: 54 slots (same as a double chest)

An autosave is performed every 5 minutes to protect the inventory in case of a crash.

# Configuration

Starting with version 2.0.0, a new config file `shareenderchest.json` is created in the `config` directory. This allows server owners to modify a few aspects of the mod.

- `autosaveSeconds` - interval to autosave the chest contents (default: `300`)
- `requireSneak` - require sneaking to open the shared ender chest (default: `true` | warning: if set to `false`, you cannot access the vanilla ender chest!)
- `inventoryRows` - number of inventory rows in the shared chest (default: `6` | range: `1` to `6` | warning: reducing this value can cause loss of items, edit with caution)
- `openInHand` - allow opening ender chest from the player's hand (default: `true`)
- `openFromInventory` - allow opening ender chest from the player's inventory (default: `true`)
- `playOpenSound` - play the ender chest opening sound (default: `true`)
- `inventoryName` - the name of the shared ender chest displayed in the ui (default: `Shared Ender Chest`)
