CoordinatesObfuscator
=====================

**Hide the real coordinates to the players.**

This plugin helps the owner to make sure that players with modded clients do not take unfair advantage of vanilla
players when `/gamerule reducedDebugInfo` is set to `true`.

How it works
------------
Every time a player changes position, dies, joins a server or changes a world, the **X** and **Z** coordinates will be
shifted by a random number, making useless every tool that uses absolute coordinates systems.

Requirements
------------
ProtocolLib 5.0.0 Snapshot or higher

Permissions
-----------
Set `coordinatesobfuscator.bypass` to disable obfuscation for a group of players or a specific player
