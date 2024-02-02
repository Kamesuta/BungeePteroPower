# BungeePteroPower
![LogoArt](https://github.com/Kamesuta/BungeePteroPower/assets/16362824/e8914f79-806b-436c-a0e6-e4eaf8ad5eca)  
[![Spigotmc Available](https://img.shields.io/badge/Spigotmc-Download-green)](https://www.spigotmc.org/resources/%E2%9A%A1-bungeepteropower-%E2%9A%A1-start-stop-servers-when-player-join-leave.114883/)

BungeePteroPower is a plugin that can automatically start/stop servers based on the number of players.  
It can start and stop servers on the [Pterodactyl panel](https://pterodactyl.io/) when players join or leave the Bungeecord proxy server.  
This helps to save server resources and manage servers more efficiently.  

https://github.com/Kamesuta/BungeePteroPower/assets/16362824/019fdfc5-f0fc-4532-89f3-3342b5812593

## Key Features

- Automatically stops servers using Pterodactyl's API when there are no players on the server for a certain period of time.
- Automatically starts servers using Pterodactyl's API when players join the server.
- The time until shutdown can be configured for each server.
- Permissions settings allow for specifying players who can manually start servers and players for whom automatic startup is enabled upon joining.

![Overview](https://github.com/Kamesuta/BungeePteroPower/assets/16362824/3cece79e-b41a-4119-a6cd-4800dd4f705d)

## Download

- You can download it from [Spigot](https://www.spigotmc.org/resources/%E2%9A%A1-bungeepteropower-%E2%9A%A1-start-stop-servers-when-player-join-leave.114883/) or [GitHub Releases](https://github.com/Kamesuta/BungeePteroPower/releases).

## Requirements

- Java 11 or higher
  - uses java.net.http.HttpClient for Pterodactyl API

## Installation

1. Obtain an API key in the Pterodactyl panel.
   - The client API key for Pterodactyl can be found in the "API Credentials" tab on the account page.
2. Add the plugin to the BungeeCord server and start it.
3. Configure the [required settings](#required-settings) in the generated `config.yml` file.
4. Reload the config with the `/ptero reload` command.

## Usage

### Automatic Startup

- Servers will automatically start when players attempt to join each server on BungeeCord.
  - This feature is available only to players with the `ptero.autostart.<server_name>` permission.

### Manual Start/Stop

- Use the `/ptero start <server_name>` command to manually start a server.
  - This command is available only to players with the `ptero.start.<server_name>` permission.
- Use the `/ptero stop <server_name>` command to manually stop a server.
  - This command is available only to players with the `ptero.stop.<server_name>` permission.

※ `<server_name>` refers to the server name specified in BungeeCord's `config.yml`.

### Reloading config.yml/Language files

- Use `/ptero reload` to reload the config.yml and language files.

## Configuration

The `config.yml` file includes the following settings, but not all items need to be configured.

### Required Settings

- `pterodactyl`: Configure settings for Pterodactyl, including URL and API key.
  - `url`: Set the URL of your Pterodactyl panel. (Example: https://panel.example.com/)
    - If you are using services like Cloudflare Tunnel, ensure proper bypass settings for IP-based communication.
  - `token`: Set the client API key for Pterodactyl.
    - It begins with `ptlc_`.
    - Client API keys for Pterodactyl can be found in the "API Credentials" tab on the account page.

### Optional Settings

- `language`: Set the language to be used. The default is English (`en`).
  - Supported languages are English (`en`) and Japanese (`ja`).
- `startTimeout`: After starting a server with this plugin, it will stop the server if there are no players for a certain period. The unit is seconds.
  - After starting, the server will stop after the `startTimeout` plus the server's timeout duration.
  - Setting it to 1 keeps the server running until players join and leave.
- `servers`: Configure settings for each server. Set the server ID and the time until automatic shutdown.
  - `id`: Set the server ID on Pterodactyl.
    - Server IDs on Pterodactyl can be found in the URL of the server page.
    - For example, if the URL is https://panel.example.com/server/1234abcd, the server ID is 1234abcd.
  - `timeout`: When there are no players on the server, it will stop after a certain period. The unit is seconds.

### Permission Settings

BungeePteroPower plugin allows fine-grained control over commands available to players for each server using permissions.

- `ptero.autostart.<server_name>`: Servers will automatically start when players join each server on BungeeCord for players with this permission.
- `ptero.start.<server_name>`: Allows the `/ptero start <server_name>` command to manually start a server.
  - If a player doesn't have `ptero.autostart.<server_name>` permission but has this permission, they will see a manual start button when they join the server.
- `ptero.stop.<server_name>`: Allows the `/ptero stop <server_name>` command to manually stop a server.
- `ptero.reload`: Allows the `/ptero reload` command to reload the config.

※ `<server_name>` refers to the server name specified in BungeeCord's `config.yml`.
※ Specify `*` for `<server_name>` to apply permissions to all servers.

### About Language Files

- Upon startup, a file for the language set in `config.yml` will be generated.
- You can edit and then reload the plugin's language by using the `/ptero reload` command.
- Contributions via Pull Requests for additional language files are welcome.
