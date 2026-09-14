# <img src="https://github.com/senseiwells/CustomNameTags/blob/26.2/src/main/resources/assets/custom-nametags/icon.png?raw=true" align="center" width="64px"/> CustomNameTags

This mod provides you with a way to customize your player's
name tags completely server-side with no need for any client mods.

> This documentation covers the ***latest*** version of the mod, for versions prior to 2.0.0
> please see the [old documentation](https://github.com/senseiwells/CustomNameTags/blob/26.1/README.md)

![nametags.png](https://github.com/senseiwells/CustomNameTags/blob/HEAD/assets/nametags.png?raw=true)

## Commands

You are able to manage your nametags from within the game with the `/nametag`
command, this requires permission level 2 or if you are using a permissions mod
you can have the permission `"custom-nametags.commands.nametag"`.

Text arguments support [the placeholders api](https://placeholders.pb4.eu/user/default-placeholders/),
as well as [formatting tags](https://placeholders.pb4.eu/user/quicktext/).

- `/nametag create <id> <text>` Creates a nametag with the given id and text, shown on every player.
- `/nametag edit <id> text <text>` Changes the text of an existing nametag.
- `/nametag edit <id> for <who>` Changes the for selector of an existing nametag.
- `/nametag edit <id> priority <priority>` Changes the priority of an existing nametag (higher priorities are displayed higher).
- `/nametag delete <id>` Deletes the nametag with the given id.
- `/nametag list` Lists all the defined nametags.
- `/nametag player <players> give <id>` Gives the players the nametag, even if they wouldn't normally have it.
- `/nametag player <players> revoke <id>` Hides the nametag from the players, even if they would normally have it.
- `/nametag player <players> set <id> <text>` Sets the text of the nametag for just those players. If no nametag with
that id exists, this creates one that only those players have.
- `/nametag player <players> reset <id?>` Removes the players' overrides for the given nametag, or all of their overrides.
- `/nametag reload` Reloads the config file.

### Examples

If you wanted to create a nametag that displays player's names but in rainbow you can run
the following command:
```mcfunction
/nametag create rainbow_name <rainbow>%player:displayname_visual%</rainbow>
```
The `id` given here of `rainbow_name` can be any string of characters. The `<rainbow> ... </rainbow>`
tags indicate that whatever is inbetween should be rainbow colored, this is a [formatting tag](https://placeholders.pb4.eu/user/quicktext/). 
And `%player:displayname_visual%` means that the player's name should be inserted here,
this is a [placeholder](https://placeholders.pb4.eu/user/default-placeholders/).
There are many more different formatting tags and placeholders that you can use.

By default, nametags are attached to everyone, if you only wanted that nametag to be displayed 
for operators only then you can run
```mcfunction
/nametag edit rainbow_name for operators
```

In the case where you want to manually assign whether someone gets a nametag you can run
```mcfunction
/nametag edit rainbow_name for nobody
```
Which means by default *nobody* will have this nametag, you can then override this with the following:
```mcfunction
/nametag player senseiwells give rainbow_name
```
The player specified, in this case senseiwells, will always be given the nametag.

If you want to override the text of a specific nametag, or give a player a nametag only for them
you can use:
```mcfunction
/nametag player senseiwells set rainbow_name <red>Special Red Name</red>
```
If a nametag with the id `rainbow_name` exists then the specified player's nametag will now display
the override text "Special Red Name" which is red. If a nametag with that id doesn't exist it'll
be created just for that specified player.

The commands allow you to create a basic nametag system, but for finer control the nametags config
file must be manually edited...

## Configuration

The configuration file for this mod is located in `config/custom-nametags/config.json`.
The above image used the following configuration:

```json
{
  "version": 2,
  "nametags": {
    "admin_name": {
      "text": "[Admin] <rainbow>%player:displayname_visual%</rainbow>",
      "update_interval": -1,
      "for": "operators"
    },
    "name": {
      "text": "[Player] %player:displayname_visual%",
      "update_interval": -1,
      "for": { "not": "operators" }
    },
    "held_item": {
      "text": "Holding: <green>%player:equipment_slot mainhand%</green>",
      "update_interval": 1
    },
    "data": {
      "text": "%player:health%♥ %player:hunger%🍖 %player:statistic minecraft:killed minecraft:player%⚔ %player:statistic minecraft:deaths%\uD83D\uDC80",
      "update_interval": 1
    }
  },
  "players": {}
}
```

> Configs from versions prior to 2.0.0 of the mod will automatically be upgraded to the
> new format. Your original config will be backed up as `config.old.json`.

### Nametags

The `"nametags"` object contains all the nametags that will be displayed for your
players, keyed by their id. They are stacked in the order they are listed,
the first being at the top.

- `"text"` This is the text that will be displayed on your nametag. As previously mentioned this
supports [the placeholders api](https://placeholders.pb4.eu/user/default-placeholders/) 
as well as [formatting tags](https://placeholders.pb4.eu/user/quicktext/).
- `"update_interval"` This is the interval at which the nametag will be updated, in ticks. 
Set to `-1` if your nametag doesn't need to be updated. This is relevant if your nametag contains
a placeholder which contains changing data, e.g. `%player:health%`.
- `"for"`This decides which players have this nametag, see [Player Selectors](#player-selectors)
below. Defaults to `"everyone"`.
- `"visible_to"`This decides which players can see this nametag, see [Player Selectors](#player-selectors)
below. Defaults to `"everyone"`.
- `"visible_radius"`, `"hidden_radius"` These two settings specify a range where the nametag will 
be visible to other players. Set these to a negative value to disable them.
- `"visible_through_walls"` Whether the nametag can be seen through walls, defaults to `true`.
- `"visible_when_invisible"` Whether the nametag is shown while the player is invisible, defaults to `false`.
- `"visible_with_passengers"` Whether the nametag is shown while the player has passengers, defaults to `false`.
- `"shift_height"` The vertical space the nametag takes up, defaults to `0.275`, enough for a single line of text.
- `"priority"` Nametags with a higher priority are displayed above those with a lower one, defaults to `0`.

### Player Selectors

Selectors are used by `"for"` and `"visible_to"` to pick players.

The simplest selectors are `"everyone"`, `"nobody"`, and `"operators"`:
```json5
{
  "for": "operators"
}
```

For anything else, use an object with one or more of the following conditions,
all of which must match:

| Condition            | Matches players who...                                                                         |
|----------------------|------------------------------------------------------------------------------------------------|
| `"names"`            | have one of the given usernames                                                                |
| `"uuids"`            | have one of the given uuids                                                                    |
| `"teams"`            | are on one of the given teams                                                                  |
| `"tags"`             | have one of the given scoreboard tags                                                          |
| `"permission_level"` | have at least the given operator level (`0`-`4`)                                               |
| `"permission"`       | have the given permission node (requires a permissions mod)                                    |
| `"not"`              | do **not** match the given selector                                                            |
| `"any"`              | match **any** of the given list of selectors                                                   |
| `"all"`              | match **all** of the given list of selectors                                                   |
| `"predicate"`        | match a [predicate api](https://github.com/Patbox/PredicateAPI/blob/main/BUILTIN.md) predicate |

Every condition that takes a list also accepts a single value.

```json5
{
  // Players named senseiwells or SuperSanta
  "for": { "names": ["senseiwells", "SuperSanta"] },

  // Operators on the red team
  "for": { "permission_level": 2, "teams": "red" },

  // Anyone who is either an operator or a vip
  "visible_to": { "any": [ "operators", { "permission": "group.vip" } ] },

  // Everyone except spectators
  "visible_to": { "not": { "teams": "spectators" } }
}
```

### Players

The `"players"` object lets you customize nametags for specific players, keyed
by their username or uuid. Each entry maps nametag ids to an override:

```json5
{
  "nametags": {
    "rank": { "text": "<red>[Admin]</red>", "for": "operators" },
    "name": { "text": "%player:displayname_visual%" },
  },
  "players": {
    // You can also use uuids instead of usernames
    "senseiwells": {
      // Replaces the text of the "rank" nametag, just for this player
      "rank": "<gold>[Owner]</gold>",
    },
    "SuperSanta": {
      // We give the "rank" nametag no matter if SuperSanta is an operator or not
      "rank": true,
      // Hide the "name" nametag
      "name": false,
      // Only this player has this specific nametag
      "hearts": { "text": "%player:hearts%♥", "update_interval": 1 }
    }
  }
}
```

An override can be:
- `true` or `false` to give or hide the nametag,
- a string to replace the nametag's text,
- an object with any of the fields from [Nametags](#nametags) to replace those fields,
  plus `"enabled": false` to hide it.

Overriding a nametag gives the player that nametag regardless of its `"for"` selector,
unless the override specifies its own `"for"`. Ids that don't match a global
nametag define one that only that player has.

# Caveats

There are some limitations of CustomNameTags, while for almost all the cases
the nametags will behave as expected, there are inconsistencies with the
custom nametags.

- When players press F1 to hide their HUD player custom nametags will remain visible.
- If the player is part of a team, their normal nametag will render.
  - This can be solved by hiding all nametags for the given team:
  - `/team modify <team> nametagVisibility never`