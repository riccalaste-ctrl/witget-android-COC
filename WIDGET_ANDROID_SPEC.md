# Clashboard iOS Widget → Replica Android — Specifica completa

> Sorgente: `C:\Users\Utente1\Desktop\clash_widgets` (app iOS "Clashboard", companion di Clash of Clans).
> Scopo: replicare i widget su Android (Jetpack Glance / AppWidget).
> File chiave: `ClashDashWidget/ClashDashWidget.swift` (2569 righe), `ClashDashWidget/ClashDashWidgetBundle.swift` (23 righe),
> `clash_widgets/Models.swift` (1384), `clash_widgets/PlayerAccount.swift` (162), `clash_widgets/PersistentStore.swift` (154).
> Metodo: REA MCP connesso e verificato in questa sessione + analisi statica del repository completo
> (come prescritto dalla skill per i repo sorgente completi).
>
> ## 0. Evidenza REA di sessione (verificata 2026-10-14)
>
> - `rea_binary_session`: server connesso, nessuna target aperta (`open:false`).
>   Provider **Ghidra 12.1.4_PUBLIC disponibile** (`C:\Users\Utente1\Desktop\REA\tools\ghidra_12.1.4_PUBLIC`,
>   `analyzeHeadless.bat` + `decompile.exe` presenti); **Hopper non disponibile** (`unsupported_host` su win32);
>   **IDA non configurato** (`REA_IDA_MCP_CONFIG` assente).
> - `rea_open_binary` su `ClashDashWidget/Info.plist`: **riuscito** — `format:plist`,
>   `sha256:046589fe1a6008d4a51c89af43182e1d7a8a6ef8fe7794319e85e04446365a55`.
> - `rea_inspect_plist` sul target plist: **non eseguibile** — `capability_unavailable`,
>   il provider composito (`native-macos+rea-artifact-graph+rea-dotnet-static`) richiede macOS
>   ("Native macOS utilities require macOS"), host Windows.
> - `rea_inspect_artifact` / `inspect_asset_catalog` / `inspect_macho`: non applicabili, nessun `.app`/`.ipa`
>   compilato nel repo (verificato: zero file `*.ipa|*.xcarchive|*.app`).
> - `rea_get_evidence_bundle(detail:summary)`: **0 record ritenuti** — nessuna Evidence persistita;
>   tutti i riscontri sotto sono osservazioni inline con percorso/riga esatti.
> - Conseguenza: niente decompilazione Ghidra da fare (nessun binario); l'analisi resta statica su sorgenti,
>   `project.pbxproj`, asset e JSON — che è la via prescritta dalla skill per i repo completi.

---

## 1. Struttura target Xcode del widget

- Target: `ClashDashWidgetExtension` (`clash_widgets.xcodeproj`, righe 317-330, 470-481, 651, 707).
- Bundle ID widget: `Zachary-Buschmann.clash-widgets.ClashDashWidget` (righe 690, 746).
- `ClashDashWidget/Info.plist`: solo `NSExtensionPointIdentifier = com.apple.widgetkit-extension`.
- Entitlements (root `ClashDashWidgetExtension.entitlements` + copia identica in `clash_widgets/`):
  unico gruppo `group.Zachary-Buschmann.clash-widgets` (stesso dell'app: `clash_widgets/clash_widgets.entitlements`).
- `ASSETCATALOG_COMPILER_WIDGET_BACKGROUND_COLOR_NAME = WidgetBackground` (righe 651, 707).
- Sources compilati nel widget, fase `8879EADA /* Sources */` righe 470-481 (verificato: esattamente questi 5 file, nient'altro):
  `ClashDashWidget.swift`, `ClashDashWidgetBundle.swift`, `Models.swift`, `PlayerAccount.swift`, `PersistentStore.swift`.
  Niente `DataService`, niente networking: il widget legge solo lo stato persistito.
- Deep link su tap: sempre `clashdash://refresh` (righe 524, 1689, 2151, 2444).

## 2. Widget registrati nel bundle (`ClashDashWidgetBundle.swift`)

| # | Struct | kind | Display name | Description | Family | Stato |
|---|--------|------|--------------|-------------|--------|-------|
| 1 | `ClashDashWidget` | `ClashDashWidget` | Clash Builders | Track Building Upgrades with the widget. | `.systemMedium` | attivo |
| 2 | `LabPetWidget` | `LabPetWidget` | Lab & Pets | Track your laboratory and pet house upgrades… | `.systemSmall` | attivo |
| 3 | `BuilderBaseWidget` | `BuilderBaseWidget` | Builder Base | Track your Builder Base builders and Star Laboratory upgrades… | `.systemSmall` | attivo |
| 4 | `HelperCooldownWidget` | `HelperCooldownWidget` | Helper Cooldowns | Track helper cooldown timers… | `.systemSmall` | attivo |
| 5 | `ClosestUpgradeWidget` | `ClosestUpgradeWidget` | Closest Upgrade | Shows the closest upgrade to finishing for a selected type and profile. | `.accessoryRectangular` (lock screen) | attivo |
| 6 | `WarStatusWidget` | `WarStatusWidget` | Clan War Status | Shows the current clan war status and progress. | `.accessoryRectangular` | NASCOSTO (commentato, codice presente righe 886-1054) |
| 7 | `WarStatusSmallWidget` | `WarStatusSmallWidget` | Clan War (Compact) | Shows clan war in a compact 2x2 lock screen format. | `.systemSmall` | NASCOSTO (righe 1057-1182) |
| 8 | `ClanWarWidget` | `ClanWarWidget` | Clan War | Shows current clan war status, opponent, and war timer. | `.systemMedium` | NASCOSTO (righe 1722-2114) |

Mapping famiglie iOS → Android: `.systemSmall` ≈ 2×2 celle (~110×110dp), `.systemMedium` ≈ 4×2 celle (~220×110dp),
`.accessoryRectangular` ≈ riquadro lock-screen/glance rettangolare (una riga, solo testo/icone monocromatiche su iOS).

Tutti i widget usano `AppIntentConfiguration` + `containerBackground(.fill.tertiary)` su iOS 17+.

## 3. Layout e metriche esatte per widget

### 3.1 Clash Builders (systemMedium, righe 460-639)
- Griglia `LazyVGrid`: 4 builder → 2 colonne; 5-6 builder → 3 colonne. Spacing colonne 8, righe 4, padding orizzontale 6, padding esterno 12.
- Cella (max 6, ordinate per `endTime` crescente, solo `category == .builderVillage`; riga 447-456):
  nome 10pt semibold 1 riga + eventuale badge supercharge 8×8 (`extras/supercharge`);
  icona 28×28 a sinistra; a destra livello 8pt secondary + tempo 9pt orange;
  barra `ProgressView` verde h4.
- Cella libera: SF `checkmark.circle.fill` 20pt verde 70% + "Builder N" 9pt + "Available" 8pt.
- Riga stato: "All Builders Busy" verde bold caption2 oppure "N builder(s) free" orange + ` • profileName`.
- Refresh timeline (righe 279-318): boost attivi → ≤60s: 30s; ≤300s: 1min; altrimenti 5min. Senza boost → ≤300s: 1min; altrimenti 15min.

### 3.2 Lab & Pets (systemSmall, righe 2116-2268)
- 2 slot (`lab` + `pets`, ordinati per endTime). Icona 22×22, nome 11pt semibold, livello 8pt, tempo 10pt orange su riga separata, barra verde h3.
- Stato: "All Busy" verde / "N free" orange 9pt bold + profileName 8pt. Vuoto: `hammer.fill` + "Available".
- Refresh: 1min con boost, 15min senza (righe 1314-1318).

### 3.3 Builder Base (systemSmall, righe 2409-2562)
- Slot: BH<6 → 1 slot; BH≥6 → 3 slot (2 builderBase + 1 starLab, righe 2305-2320). Icona 18×18, nome 11pt, livello 8pt, tempo 9pt **bold** orange, barra verde h2.
- Refresh: 1min con boost, 15min senza.

### 3.4 Helper Cooldowns (systemSmall, righe 1595-1719)
- Icona grande `buildings_home/helper_hut` 64×64 centrata + titolo "Helper's Hut" 11pt semibold.
- Barra custom arrotondata h6 (sfondo grigio 20%, riempimento verde), totale 23h = 82800s.
- Se residuo ≤3600s: `TimelineView` ogni secondo con testo live; altrimenti testo statico. Formato `formatHelperCooldown`: `Xh Xm` / `Xm Xs` / `Xs`; 0 → "Ready to Work" verde.
- Helper mappati da `helpers` dell'export (righe 1540-1551): `93000000` Builder's Apprentice → `profile/apprentice_builder`,
  `93000001` Lab Assistant → `profile/lab_assistant`, `93000002` Alchemist → `profile/alchemist`.
- Refresh: minimo residuo <0 → 15min; ≤30s → 30s; altrimenti 5min (righe 1462-1476).

### 3.5 Closest Upgrade (lock screen rectangular, righe 784-875)
- Icona 36×36 + label tipo caption2 secondary + nome 12pt semibold 1 riga + tempo 11pt orange + barra lineare.
- 7 tipi selezionabili (`UpgradeTypeSelection`, righe 180-199): builders, lab, pets, builderBase, starLab,
  homeOverall (builder+lab+pets), builderBaseOverall (builderBase+starLab).
- Refresh: 1min con boost, 15min senza (riga 690).

### 3.6 War (nascosti ma implementati)
- Date formato `yyyyMMdd'T'HHmmss.SSS'Z'`, locale `en_US_POSIX`, UTC (righe 1028-1035).
- Fasi: Preparation / Battle / Ended + progress 0..1; timer `Prep - Xd Xh` / `Battle - Xh Xm`.
- `ClanWarWidget` usa `AsyncImage` dai `badgeUrls.small` con fallback cerchio colorato + iniziali; stelle `★ N`; `% destroyed` = max(clan, opponent).

## 4. Icone dedicate al widget — inventario completo

`ClashDashWidget/Assets.xcassets/` (unico catalogo proprio del widget):

| File | Byte | Dimensioni px (verificate) | Note |
|------|------|----------------------------|------|
| `Contents.json` | 69 | — | solo `{info:{author:xcode,version:1}}` |
| `AccentColor.colorset/Contents.json` | 134 | — | colore vuoto (default sistema) |
| `WidgetBackground.colorset/Contents.json` | 134 | — | colore vuoto, nome referenziato dal build (`WidgetBackground`) |
| `AppIcon.appiconset/Contents.json` | 642 | — | icona extension (non visibile in home) |
| `buildings_home/elixir_collector.imageset/` | PNG `100(8).png` 15047 + Contents 325 | **100×131** | scala 1x puntata, 2x/3x vuote — DUPLICATO byte-identico dell'app (SHA-256 `6147ECA7…`) |
| `buildings_home/hero_hall.imageset/` | `hero_hall.png` 160917 + Contents 328 | **350×332** | idem — DUPLICATO dell'app (`66E451FA…`). PNG più pesante del widget |
| `buildings_home/x_bow.imageset/` | `100(2).png` 14511 + Contents 325 | **100×98** | idem — DUPLICATO dell'app (`45948706…`) |
| `crafted_defenses/Contents.json` | 128 | — | namespace folder |
| `crafted_defenses/bomb_hive.imageset/` | `120(1).png` 19732 + Contents 325 | **120×107** | idem — **UNICO del widget** (assente nel catalogo app) |
| `crafted_defenses/hero_bell.imageset/` | `120.png` 22074 + Contents 322 | **120×122** | idem — **UNICO del widget** |
| `crafted_defenses/light_beam.imageset/` | `120(2).png` 22331 + Contents 325 | **120×127** | idem — **UNICO del widget** |
| `extras/hero_potion.imageset/` | `50(9).png` 8020 + Contents 324 | **50×66** | idem — DUPLICATO byte-identico dell'app (`95B6E8B9…`) |

Pattern `Contents.json` (identico in tutti, cambia solo `filename`):
`images: [{filename, idiom: universal, scale: 1x}, {idiom: universal, scale: 2x}, {idiom: universal, scale: 3x}]`.
Solo la 1x ha file; 2x/3x lasciano fare upscaling a iOS (`interpolation(.none)` nel codice per restare pixelati).

## 5. Punto cruciale: il widget NON usa solo le sue icone

`project.pbxproj`, fase Resources dell'extension `8879EADC` (righe 393-401): include **ENTRAMBI**
l'`Assets.xcassets` dell'app (`8879EB51` → fileRef `A1B2C309`, riga 18) **e** quello del widget (`B2C3E505` → `B2C3D405`, riga 36).
Appartenenza verificata: la fase `8879EADC` è nelle `buildPhases` del `PBXNativeTarget "ClashDashWidgetExtension"`
(riga 318-323) con `productType = "com.apple.product-type.app-extension"` (riga 337).
Quindi a runtime `UIImage(named:)` nel widget risolve anche tutte le icone dell'app. Le 7 immagini nel catalogo widget
sono solo override/aggiunte; **per Android vanno copiati gli asset dall'app** (`clash_widgets/Assets.xcassets/`):
`buildings_home/` (~60 set), `builder_base/` (~45), `lab/` (~60), `pets/` (12), `heroes/` (6),
`equipment/` (~48), `extras/` (~28), `crafted_defenses/` (6: bomb_hive, hero_bell, light_beam, hot_candle, hero_hunter, cake_a_pult),
`resources/` (gold/elixir/dark_elixir, builder, clock, lab, pet_house, wall_1…wall_19), `town_hall/` (th1…th18),
`leagues/` (13), `profile/` (alchemist, apprentice_builder, goblin_builder, home_builder, lab_assistant, gold_pass, free_pass, gem, cwl),
`backgrounds/`, `images/`, `changelog/`.

Dedup verificato via SHA-256 (2026-10-14): **4 dei 7 set widget sono byte-identici all'app** (su Android basta 1 copia):
`hero_potion` (`95B6E8B9…`), `elixir_collector` (`6147ECA7…`), `hero_hall` (`66E451FA…`), `x_bow` (`45948706…`).
**Solo 3 sono aggiunte reali del widget**: `bomb_hive`, `hero_bell`, `light_beam`.
Split stagionali confermato: app `crafted_defenses/` = {cake_a_pult, hero_hunter, hot_candle},
widget `crafted_defenses/` = {bomb_hive, hero_bell, light_beam} → copertura 6/6 di `mapping.json`
(`103000001` Light Beam, `103000002` Bomb Hive, `103000007` Hero Bell, `103000011` Hot Candle,
`103000012` Hero Hunter, `103000013` Cake A Pult). Su Android unire le due cartelle in un unico set.

Icone referenziate dal codice widget ma presenti SOLO nel catalogo app (prova del dual-catalog),
con dimensioni reali verificate:
`extras/supercharge` (badge 8×8, righe 539, 2172, 2465 — file app `Screenshot_2026-01-21_at_10.48.30_AM-removebg-preview.png`,
**358×455**, 102651 byte: va trimmato/ridotto, a schermo occupa 8pt);
`buildings_home/helper_hut` (64×64, riga 1607 — `100(18).png` **100×103**);
`profile/apprentice_builder|lab_assistant|alchemist` (righe 1542-1546 — `Apprentice_Builder_HV_01.png`,
`Lab_Assistant_02.png`, `Alchemist_GroundShadows.png`, tutti **640×640** ~180-206KB: ridimensionare per il widget).

## 6. Algoritmo di risoluzione icone (da replicare 1:1, righe 599-638, ripetuto 3×)

```
folder per categoria: builderVillage→buildings_home, lab→lab, starLab→builder_base,
                      pets→pets, builderBase→builder_base
sanitize(nome): split su [non alfanumerici] → join "_" → trim "_" → lowercase
  es: "X-Bow"→x_bow, "B.O.B's Hut"→b_o_b_s_hut, "P.E.K.K.A"→p_e_k_k_a,
      "L.A.S.S.I"→l_a_s_s_i, "O.T.T.O's Outpost"→o_t_t_o_s_outpost, "Wall Breaker"→wall_breaker
se isSeasonalDefense o 103_000_000 ≤ dataId < 104_000_000: prova prima crafted_defenses/<slug>
poi <folder>/<slug>, poi <slug> nudo
prendi la prima variante esistente (su iOS: UIImage(named:) != nil); fallback: <folder>/<slug>
```

Nomi canonici: game-ID → `clash_widgets/json/mapping.json` (229 righe; range: `1xxxxxx` edifici/BB, `12xxxxxx` trappole,
`26xxxxxx` spell, `28xxxxxx` eroi, `4xxxxxx` truppe, `73xxxxxx` pet, `9xxxxxxxx` equipment, `102xxxxxx` moduli stagionali,
`103xxxxxx` archetipi stagionali, `107xxxxxx` guardiani, `93xxxxxx` helper) → `asset_map.json` (131 entry nome→slug).

## 7. Pipeline immagine (righe 16-34, 36-83)

1. `trimmedWidgetImage(named:)`: carica `UIImage`, ritaglia i bordi totalmente trasparenti (soglia alpha > 5,
   bounding-box dei pixel opachi, `cropping(to:)`). Su Android: scansione bitmap equivalente al primo caricamento + cache.
2. `.interpolation(.none)` + `scaledToFit()`: niente smoothing (pixel-art). Su Android: `FILTER_BITMAP_FLAG` off / `inScaled=false`.
3. `clashWidgetTintAwareRendering()`: iOS 18+ `widgetAccentedRenderingMode(.accentedDesaturated)` (evita blob di tinta unita
   in modalità tinted/clear). Su Android 12+ (Material You): applicare desaturazione parziale quando il launcher applica
   dynamic-tint, altrimenti colori pieni.

## 8. Dati condivisi app↔widget (file e chiavi esatte)

- File JSON: `<AppGroup>/ClashboardData/app_state.json` → `{profiles:[PlayerAccount], selectedProfileID, appearancePreference,
  notificationSettings?}` (`PersistentStore.swift`).
- `PlayerAccount`: id, displayName, tag, rawJSON (export di gioco), lastImportDate, activeUpgrades `[BuildingUpgrade]`,
  cachedProfile (`PlayerProfile`: townHallLevel, builderHallLevel…), builderCount (default 5, +1 se goblin attivo),
  builderApprenticeLevel, labAssistantLevel, alchemistLevel, clockTowerLevel, goldPassBoost 0-100,
  hiddenEquipmentNames, activeBoosts `[ActiveBoost]`, helperCooldownOverrides.
- `BuildingUpgrade`: id UUID, dataId Int?, name, targetLevel, superchargeLevel?, superchargeTargetLevel?, usesGoblin,
  endTime/startTime Date, totalDuration, category (`builderVillage|lab|starLab|pets|builderBase`), isSeasonalDefense?,
  goldPassFactorAtImport?, remoteEventTimeMultiplier?, sourceExportTimer?, appliedRemoteEventIDs.
  `levelDisplayText`: `SC c → t` se supercharge else `Lv t-1 → t`.
- Chiavi `UserDefaults(suiteName:)` usate dal widget: `widget_profile_selection` (UUID o legacy `profile1…profile10`),
  `saved_upgrades` (fallback JSON), `saved_raw_json` (fallback helper), `widget_simple_text`,
  `widget_import_requested` (flag per Control Center), `war_json_<tag>`, `current_war_json`, `is_in_cwl_<tag>`, `is_in_cwl`.
- Su Android: un `DataStore`/file JSON con stesso schema + chiavi mirror; deep link `clashdash://` → intent-filter/path `refresh`.

## 9. Intent di configurazione (righe 87-226)

- `WidgetProfileIntent`: parametro `selectedProfile: ProfileEntity?` (query dinamica dai profili salvati + "Last Opened Profile"/automatic);
  `perform()` salva `widget_profile_selection` e ricarica le timeline. Su Android: `AppWidgetManager.OPTION_*` / Glance `Action` + selector attività.
- `ClosestUpgradeIntent`: parametri `upgradeType` (7 casi, riga 189-198) + `selectedProfile`.
- `ImportClipboardIntent` (`openAppWhenRun=true`): setta `widget_import_requested=true`. Su Android: bottone che apre l'app con extra import.
- Controllo Control Center iOS 18+ (`ImportClipboardControl`, righe 1185-1217): icone SF `doc.on.clipboard` + `plus.circle.fill`.

## 10. Tempi e progress (righe 1219-1282)

- `formatBoostedTimeRemaining`: ≤0 "Complete"; giorni `Xd Xh`; ore `Xh Xm`; minuti `Xm XXs`; else `Xs`.
- Progress = `(effectiveTotal − boostedRemaining) / effectiveTotal`, clamp 0..1; `effectiveTotal` include gold-pass ed eventi remoti.
- Boost (`BoostType`, `Models.swift` 8-111): builderPotion 10x/1h, researchPotion 24x/1h, petPotion 24x/1h, builderBite 2x/1h,
  studySoup 4x/1h, labAssistant/builderApprentice level-based/1h, clockTower(Potion) 10x (torre: 14+2/livello min).
  Categorie affette per tipo (righe 92-105); i boost torre non si sommano (estendono).

## 11. Checklist operativa per Android (per gli agenti implementatori)

1. **Asset**: DEDUPLICARE prima di copiare — 4 set widget sono identici all'app (par. 5), copia singola.
   Copiare dal widget solo `crafted_defenses/{bomb_hive,hero_bell,light_beam}` + dall'app: `buildings_home/`,
   `builder_base/`, `lab/`, `pets/`, `crafted_defenses/{cake_a_pult,hero_hunter,hot_candle}`,
   `extras/supercharge.*` (sorgente 358×455 → trim + downscale a badge 8dp),
   `extras/hero_potion.*`, `buildings_home/helper_hut.*` (100×103),
   `profile/{apprentice_builder,lab_assistant,alchemist}.*` (sorgenti 640×640 → downscale),
   `heroes/`, `resources/{gold,elixir,dark_elixir,builder,clock}.*`.
   Struttura `res/drawable-nodpi/coc_<cartella>_<slug>.png` (nodpi: gli originali sono 1x senza 2x/3x).
   Dimensioni widget verificate: icone upgrade 28/22/18/36dp da sorgenti ~100-120px (downscale diretto ok);
   `hero_hall` 350×332 va ridotto; ritratti helper 640×640 vanno ridotti.
   Applicare al primo load il trim alpha>5 + cache (par. 7).
2. **Risoluzione nomi**: implementare `sanitize()` + ordine `crafted_defenses → <folder> → nudo` + tabella categoria→cartella (par. 6);
   caricare `mapping.json` + `asset_map.json` come tabelle (par. 6, range ID).
3. **Widget**: 5 Glance (`ClashBuilders` 4×2, `LabPets` 2×2, `BuilderBase` 2×2, `HelperCooldowns` 2×2, `ClosestUpgrade` stile lista);
   i 3 war solo se richiesto (codice pronto al par. 3.6).
4. **Dati**: mirror di `app_state.json` + chiavi par. 8 via DataStore condiviso app/widget (stesso `userId`/processo);
   parsing `rawJSON` (`CoCExport`, `Models.swift` 612-680) per helper/cooldown.
5. **Refresh**: `updatePeriodMillis=0` + `WorkManager` con le policy del par. 3 (30s/1m/5m/15m); lock-screen widget senza refresh rapido.
6. **UI**: font/pesi/dimensioni/colori del par. 3 (verde `#…` busy/complete, orange tempi, secondary livelli); barre h4/h3/h2;
   tap → deep-link intent `clashdash://refresh`.
7. **Tint**: con Material You dynamic color applicare matrice desaturata alle icone (par. 7.3), mai tinta piena.
8. **Tempi**: formattazione par. 10 + fuso UTC per le date war (`yyyyMMdd'T'HHmmss.SSS'Z'`); countdown live solo ≤60min (come iOS).

## 12. Limiti e incognite residue

- Nomi asset derivati a runtime: la lista PNG copre i casi osservati, non prova completezza (grep statico; doc ufficiale par. "Do not classify assets as unused using filename search alone").
- War widget nascosti ma funzionanti; `WarStatusWidget`/`WarStatusSmallWidget` condividono il provider.
- `Products.storekit`, Google Ads/UMP, notifiche e tab app non servono al widget e non sono coperti qui.
- REA: nessuna Evidence persistita in sessione (`get_evidence_bundle` = 0 record); ogni riscontro sopra è
  un'osservazione inline con percorso/riga/hash. Ghidra disponibile ma inutilizzato (nessun binario nel repo).
