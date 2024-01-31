# BungeePteroPower

BungeePteroPowerは、サーバーの人数に応じてサーバーを自動的に起動/終了することができるプラグインです。  
プレイヤーがBungeecordプロキシサーバーに参加または退出したときに、Pterodactylパネル上のサーバーを起動および停止することができます。  
そのため、サーバーのリソースを節約し、サーバーをより効率的に管理するのに役立ちます。  

https://github.com/Kamesuta/BungeePteroPower/assets/16362824/4f36d65c-ca9f-4dd2-9413-ecf15cd094dc

## 主な機能

- サーバーに一定時間プレイヤーがいない状態になった場合、PterodactylのAPIを使用して自動的にサーバーを停止します。
- サーバーにプレイヤーが参加した場合に、PterodactylのAPIを使用して自動的にサーバーを起動します。
- 停止までの時間は、サーバーごとに設定可能です。
- 権限設定により、手動起動可能なプレイヤー、及び、入ると自動的に起動が行われるプレイヤーを設定できます。

## ダウンロード

- [GitHub Releases](https://github.com/Kamesuta/BungeePteroPower/releases) からダウンロードできます。

## 必要要件

- Java 11 以上
  - PterodactylのAPIを使用するため、java.net.http.HttpClientを使用しています。

## インストール

1. PterodactylパネルでAPIキーを取得します。
  - PterodactylのクライアントAPIキーは、アカウントページの「API Credentials」タブから確認できます。
2. プラグインをBungeeCordサーバーに追加し、起動します。
3. 生成された `config.yml`ファイルの [設定が必要な項目](#設定が必要な項目) を設定します。
4. `/ptero reload`コマンドでコンフィグを再読み込みします。

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
  - `token`: PterodactylのクライアントAPIキーを設定します。
    - `ptlc_`から始まる文字列です。
    - PterodactylのクライアントAPIキーは、アカウントページの「API Credentials」タブから確認できます。

### 任意の設定項目

- `language`: 使用する言語を設定します。デフォルトは英語(`en`)です。
  - 対応している言語は、英語(`en`) と 日本語(`ja`) です。
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
