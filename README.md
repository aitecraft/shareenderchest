# ShareEnderChest

A simple Minecraft mod that provides a shared inventory for all players, accessible using an ender chest.

<a href="https://modrinth.com/mod/fabric-api">
    <img alt="Fabric API Required" src="https://i.imgur.com/Ol1Tcf8.png" width="143" height="50">
</a>

To function, the mod is required on the server. Additional features are enabled if installed on the client.

There are multiple ways to open the Shared Ender Chest, listed below:

1. Shift + Right-clicking on an Ender Chest block [Sneak + Use]
    - Server installation is enough for this

2. Right-clicking with an Ender Chest in your hand [Use Item]
    - Make sure to not aim at any block or you'll just place the Ender Chest!
    - Server installation is enough for this

3. Ctrl + Click or Alt + Click an Ender Chest item in your inventory
    - Mod must be present on both server and client for this
    - Both Left-click and Right-click work

The shared inventory is saved in a `shareenderchest.sav` file in your world folder. An autosave is performed every 5 minutes to protect the inventory in case of a crash.
