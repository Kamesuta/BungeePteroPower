# BungeePteroPower
![LogoArt](https://github.com/Kamesuta/BungeePteroPower/assets/16362824/e8914f79-806b-436c-a0e6-e4eaf8ad5eca)  
[![Spigotmc Available](https://img.shields.io/badge/Spigotmc-ダウンロード-green)](https://www.spigotmc.org/resources/%E2%9A%A1-bungeepteropower-%E2%9A%A1-start-stop-servers-when-player-join-leave.114883/)
[![JitPack](https://jitpack.io/v/Kamesuta/BungeePteroPower.svg)](https://jitpack.io/#Kamesuta/BungeePteroPower)

BungeePteroPowerは、サーバーの人数に応じてサーバーを自動的に起動/終了することができるプラグインです。  
プレイヤーがBungeecordプロキシサーバーに参加または退出したときに、[Pterodactylパネル](https://pterodactyl.io/)上のサーバーを起動および停止することができます。  
そのため、サーバーのリソースを節約し、サーバーをより効率的に管理するのに役立ちます。  

https://github.com/Kamesuta/BungeePteroPower/assets/16362824/019fdfc5-f0fc-4532-89f3-3342b5812593

## 主な機能

- サーバーに一定時間プレイヤーがいない状態になった場合、PterodactylのAPIを使用して自動的にサーバーを停止します。
- サーバーにプレイヤーが参加した場合に、PterodactylのAPIを使用して自動的にサーバーを起動します。
- 停止までの時間は、サーバーごとに設定可能です。
- 権限設定により、手動起動可能なプレイヤー、及び、入ると自動的に起動が行われるプレイヤーを設定できます。

## ダウンロード

- [Spigot](https://www.spigotmc.org/resources/%E2%9A%A1-bungeepteropower-%E2%9A%A1-start-stop-servers-when-player-join-leave.114883/) または [GitHub Releases](https://github.com/Kamesuta/BungeePteroPower/releases) からダウンロードできます。

![Overview](https://github.com/Kamesuta/BungeePteroPower/assets/16362824/3cece79e-b41a-4119-a6cd-4800dd4f705d)

## 必要要件

- Java 11 以上
    - PterodactylとのREST API通信に、Java11の`java.net.http.HttpClient`を使用しています。

## インストール

1. PterodactylパネルでAPIキーを取得します。
    - PterodactylのクライアントAPIキーは、アカウントページの「API Credentials」タブから確認できます。
2. プラグインをBungeeCordサーバーに追加し、起動します。
3. 生成された `plugins/BungeePteroPower/config.yml` ファイルで [設定が必要な項目](#設定が必要な項目) を設定します。
    ```yml
    # Pterodactyl 設定
    pterodactyl:
      # Pterodactyl パネルの URL
      # Cloudflare Tunnel を使用する場合は、バイパス設定で IP を許可する必要があります。
      url: "https://panel.example.com"
      # Pterodactyl パネルのクライアント API キー。"ptlc_" で始まります。
      # クライアント API キーは、「アカウント」ページの「API 資格情報」タブで見つけることができます。
      apikey: "ptlc_000000000000000000000000000000000000000000"
    
    # サーバーごとの設定
    servers:
      pvp:
        # Pterodactyl サーバー ID
        # Pterodactyl サーバーページの URL で Pterodactyl サーバー ID を見つけることができます。
        # たとえば、URL が https://panel.example.com/server/1234abcd の場合、サーバー ID は 1234abcd です。
        id: 1234abcd
        # 最後のプレイヤーが退出した後、サーバーを停止するまでの秒数。
        # 自動的にサーバーを停止したくない場合は、-1 に設定します。
        # 0 に設定すると、最後のプレイヤーが退出した直後にサーバーが停止します。
        timeout: 30
    ```
4. `/ptero reload` コマンドで設定をリロードします。
5. [権限設定](#権限設定) を構成します。  
    (このプラグインを使用するためには権限を設定する必要があります。そうしないと、プレイヤーは何もできません！)  
    次のいずれかの方法を使用できます。  
    - [LuckPerms](https://luckperms.net/) のような権限プラグインを使用します。
        1. LuckPermsの場合、以下のコマンドで権限を設定します。
            ```
            # プレイヤーはすべてのサーバーを起動できます
            /lp user <プレイヤー名> permission set ptero.autostart.*
            # プレイヤーは特定のサーバーを起動できます
            /lp user <プレイヤー名> permission set ptero.autostart.<サーバー名>
            # すべてのプレイヤーはすべてのサーバーを起動できます
            /lp group default permission set ptero.autostart.*
            ```
            ※ `<プレイヤー名>` はプレイヤーの名前、 `<サーバー名>` は BungeeCord の `config.yml` で指定されたサーバー名を指します。
    - ビルトインの権限設定を使用します。
        1. `config.yml` を開きます。
        2. 以下の設定を `config.yml` ファイルに追加します。
            ```yml
            permissions:
                default:
                # プレイヤーはすべてのサーバーを起動できます
                - ptero.autostart.*
                # プレイヤーは特定のサーバーを起動できます
                - ptero.autostart.<server_name>
            ```  
            ※ `<server_name>` は BungeeCord の `config.yml` で指定されたサーバー名を指します。
        3. BungeeCord サーバーを再起動します。

## 使用方法

### 自動起動

- プレイヤーがBungeeCord上の各サーバーに参加しようとしたときに、サーバーが自動的に起動します。
    - `ptero.autostart.<サーバー名>`権限を持つプレイヤーのみが使用できます。

### 手動での起動/停止

- `/ptero start <サーバー名>`コマンドで、サーバーを手動で起動できます。
    - `ptero.start.<サーバー名>`権限を持つプレイヤーのみが使用できます。
- `/ptero stop <サーバー名>`コマンドで、サーバーを手動で停止できます。
    - `ptero.stop.<サーバー名>`権限を持つプレイヤーのみが使用できます。

※ `<サーバー名>` はBungeeCordの `config.yml` に記述されているサーバー名です。

### config.yml/言語ファイルリロード

- `/ptero reload` でconfig.ymlと言語ファイルのリロードができます。

## 設定

`config.yml`ファイルには以下の設定項目がありますが、すべての項目を設定する必要はありません。

### 設定が必要な項目

- `pterodactyl`: Pterodactylの設定を行います。URLとAPIキーを設定します。
    - `url`: ご自身のPterodactylパネルのURLを設定します。(例: https://panel.example.com/)
        - Cloudflare Tunnelなどを使用している場合は、通信できるようにIPによるバイパス設定を行ってください。
    - `apikey`: PterodactylのクライアントAPIキーを設定します。
        - `ptlc_`から始まる文字列です。
        - PterodactylのクライアントAPIキーは、アカウントページの「API Credentials」タブから確認できます。

### 任意の設定項目

- `language`: 使用する言語を設定します。デフォルトは英語(`en`)です。
    - 対応している言語は以下の通りです
        - 英語(`en`)
        - 日本語(`ja`)
        - フランス語(`fr`)
- `startTimeout`: このプラグインでサーバーを起動した後、一定時間プレイヤーがいない場合にサーバーを停止します。単位は秒です。
    - 起動後は、startTimeout+サーバーのtimeout時間が経過すると、サーバーが停止します。
    - 1に設定すると、サーバー起動後、プレイヤーが入って抜けるまではサーバーを起動したままになります。
- `servers`: サーバーごとの設定を行います。サーバーIDと自動停止までの時間を設定します。
    - `id`: Pterodactyl上のサーバーIDを設定します。
        - PterodactylのサーバーIDは、サーバーページのURLから確認できます。
        - 例えば、URLが https://panel.example.com/server/1234abcd の場合、サーバーIDは 1234abcd です。
    - `timeout`: サーバーからプレイヤーがいなくなった際、一定時間プレイヤーがいない場合にサーバーを停止します。単位は秒です。

### パーミッション設定

BungeePteroPowerプラグインでは、パーミッションを使用して、プレイヤーが使用できるコマンドをサーバーごとに細かく制限できます。

- `ptero.autostart.<サーバー名>`: この権限を持つプレイヤーがBungeeCord上の各サーバーに参加したときに、サーバーが自動的に起動します。
- `ptero.start.<サーバー名>`: `/ptero start <サーバー名>`コマンドで、サーバーを手動で起動できます。
    - `ptero.autostart.<サーバー名>`の権限がなく、この権限を持つ場合、サーバーに入ると「[サーバーを起動]」という手動で起動できるボタンが表示されます。
- `ptero.stop.<サーバー名>`: `/ptero stop <サーバー名>`コマンドで、サーバーを手動で停止できます。
- `ptero.reload`: `/ptero reload`コマンドで、コンフィグを再読み込みできます。

※ `<サーバー名>` はBungeeCordの `config.yml` に記述されているサーバー名です。
※ `<サーバー名>` のところに `*` を指定すると、すべてのサーバーに対して権限が適用されます。

### 言語ファイルについて

- 起動すると、`config.yml`の `language` で設定した言語のファイルが生成されます。
- 編集してから `/ptero reload` コマンドで再読み込みすることで、プラグインの言語を変更できます。
- Pull Requestで言語ファイルを追加していただけると嬉しいです。

## プラグインの開発者向け情報

### アドオンを作成する

- BungeePteroPowerは、他のプラグインと連携するためのAPIを提供しています。
    - Pterodactyl以外に対応したい場合は、APIを実装することで対応可能です。
- 依存関係を追加することで、BungeePteroPowerのAPIを使用できます。
    1. アドオン内のpom.xml内にJitPackリポジトリを追加します
        ```xml
        <repositories>
            <repository>
                <id>jitpack.io</id>
                <url>https://jitpack.io</url>
            </repository>
        </repositories>
        ```
    2. BungeePteroPowerを依存関係に追加します
        ```xml
        <dependency>
            <groupId>com.github.Kamesuta</groupId>
            <artifactId>BungeePteroPower</artifactId>
            <version>バージョン</version>
        </dependency>
        ```
    3. bungee.ymlに依存関係を追加します
        ```yml
        depends:
          - BungeePteroPower
        ```
    4. APIを使用します
        ```java
        import com.kamesuta.bungeepteropower.api.BungeePteroPowerAPI;

        public class YourPlugin extends JavaPlugin {
            @Override
            public void onEnable() {
                // BungeePteroPowerAPIのインスタンスを取得します
                BungeePteroPowerAPI api = BungeePteroPowerAPI.getInstance();
                // カスタムPowerControllerを登録します
                api.registerPowerController("your_service", new YourPowerController());
            }
        }
        ```
- あなたのPowerControllerをBungeePteroPowerに追加してほしい場合は、プルリクエストを送ってください。

### ビルド

BungeePteroPowerではプルリクエストを歓迎しています。  
以下の手順でビルドできます。  

```bash
git clone https://github.com/Kamesuta/BungeePteroPower.git
cd BungeePteroPower
mvn install
```
- このプラグインは、Java 11 以上でビルドする必要があります。
- ビルド後、`target` ディレクトリに `BungeePteroPower-<バージョン>.jar` ファイルが生成されます。
