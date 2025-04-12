# Server-Side Waystones (sswaystones)

Polymer-based server sided Waystone mod, with Geyser and vanilla client support. (inspired by the now-archived [Wraith Waystones Polymer Port](https://modrinth.com/mod/polymer-ports-waystones))

![Picture of the in-game waystone, used for the project icon](src/main/resources/assets/sswaystones/icon.png)

## Features
- Server-side Waystone blocks that allow you to teleport long distances and across dimensions.
- Feature-full GUIs for both Bedrock and Java players, using forms and chest GUIs respectively.
- Waystones can have up to 32 character long names and can be set to "global" to allow for anybody on the server to use them.
- The mod works on both servers and singleplayer worlds, and all storage data is held in the world itself.

## Recipes
*Recipe for the Waystone*

![Recipe for the Waystone](assets/waystone_recipe.png)

*Recipe for the Portable Waystone*

![Recipe for the Portable Waystone](assets/portable_waystone_recipe.png)

# Configuration
The file is saved in `config/sswaystones.json`, and can be edited either manually or by commands.
- `/sswaystones config set [key] [value]` (sets a configuration option)
- `/sswaystones config get [key]` (gets the value of a configuration option)
- `/sswaystones config help` (lists all configuration options)
- `/sswaystones config reload` (loads configuration from disk)
- `/sswaystones config save` (saves configuration to disk)

## Roadmap
- [x] Add optional XP price for teleporting.
- [x] More configuration
- [ ] More items (~~Portable Waystone~~, Scrolls, etc.)

## Contributing
If there is a bug you would like to fix or a feature you'd like to propose, you may make a Pull Request and it will be reviewed.

### Translating
If you would like to translate this mod into another language, you can do so by creating its respective language file in `src/main/resources/data/sswaystones/lang` and making a PR, all of the keys are in the default `en_us.json` and you can find a tutorial on how translations work on the [Fabric Wiki](https://fabricmc.net/wiki/tutorial:lang). (just make sure you make it in the `data` folder and not `assets`!)
