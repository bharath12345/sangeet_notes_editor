# Taals & Raags

The editor ships with 11 taals and 26 raags built in. You can also create custom compositions in any of these and add your own metadata.

## Built-in taals

| Taal | Matras | Vibhag pattern | Notes |
|------|--------|----------------|-------|
| **Teentaal** | 16 | 4+4+4+4 | Sam, Taali 2, Khali, Taali 3 — the most common taal |
| **Ektaal** | 12 | 2+2+2+2+2+2 | Sam, Khali, Taali 2, Khali, Taali 3, Taali 4 — common in vilambit khayal |
| **Jhaptaal** | 10 | 2+3+2+3 | Sam, Taali 2, Khali, Taali 3 — frequent in instrumental music |
| **Rupak** | 7 | 3+2+2 | Sam coincides with Khali — unusual. Common in semiclassical |
| **Dadra** | 6 | 3+3 | Sam, Khali — light-classical |
| **Keherwa** | 8 | 4+4 | Sam, Khali — light-classical, film music |
| **Chautaal** | 12 | 2+2+2+2+2+2 | Pakhawaj taal, used in dhrupad |
| **Dhamar** | 14 | 5+2+3+4 | Pakhawaj taal, used in dhamar compositions |
| **Tilwada** | 16 | 4+4+4+4 | Vilambit, similar structure to Teentaal but different bols |
| **Jhoomra** | 14 | 3+4+3+4 | Vilambit, characteristic Khayal taal |
| **Deepchandi** | 14 | 3+4+3+4 | Semi-classical |

Each taal includes the **vibhag structure** (clap groups) with markers (Sam = X, Taali = numbered clap, Khali = 0) and the traditional **bol** sequence shown to the user.

### Rupak — the unusual taal

In Rupak, the **sam coincides with khali** (no clap on the first beat). The editor handles this correctly: the X marker appears on beat 1 along with the 0, and bol sequences begin from a "Tin" (rather than "Dha").

### Custom taals

The taal field in the `.swar` file format is data, not code — you can add custom taals by editing the JSON directly. A future release will add a Custom Taal dialog. For now, pick the closest built-in and edit metadata via Properties.

## Built-in raags

All 26 raags include: arohan (ascending), avrohan (descending), vadi (main note), samvadi (secondary), pakad (signature phrase), thaat (parent scale), and prahar (time of day).

| Raag | Thaat | Prahar |
|------|-------|--------|
| **Yaman** | Kalyan | Evening (1st prahar of night) |
| **Bhairav** | Bhairav | Early morning |
| **Durga** | Bilawal | Late evening |
| **Bhupali** | Kalyan | Early evening |
| **Malkauns** | Bhairavi | Late night |
| **Bageshree** | Kafi | Late night |
| **Desh** | Khamaj | Late evening |
| **Kafi** | Kafi | Anytime (especially monsoon) |
| **Bihag** | Bilawal | Night |
| **Kedar** | Kalyan | Evening |
| **Hansadhwani** | Bilawal | Anytime |
| **Jaunpuri** | Asavari | Morning |
| **Todi** | Todi | Morning |
| **Marwa** | Marwa | Sunset |
| **Puriya** | Marwa | Evening |
| **Shree** | Poorvi | Late afternoon |
| **Miyan ki Malhar** | Kafi | Monsoon, late evening |
| **Megh** | Kafi | Monsoon |
| **Pilu** | Kafi | Light raag, anytime |
| **Khamaj** | Khamaj | Evening |
| **Bilawal** | Bilawal | Morning |
| **Bhairavi** | Bhairavi | Morning (concluding raag) |
| **Asavari** | Asavari | Late morning |
| **Ahir Bhairav** | Bhairav | Early morning |
| **Hindol** | Kalyan | Morning |
| **Madmad Sarang** | Kafi | Noon |

Raag metadata appears in the composition header (above the grid) and in the HTML export.

### Custom raags

Same as taals — the raag field is data. Edit the `.swar` JSON to define a custom raag with its own arohan/avrohan/etc. A future release will add a Custom Raag dialog.

## Laya (tempo)

| Laya | Approximate BPM | Typical use |
|------|-----------------|-------------|
| **Ati-vilambit** | 20–30 | Slow khayal opening |
| **Vilambit** | 30–60 | Slow gat / bandish |
| **Madhya** | 60–120 | Medium gat / bandish |
| **Drut** | 120–250 | Fast taan, jhala |
| **Ati-drut** | 250+ | Very fast jhala |

Laya is metadata — it doesn't change the rendering. Vilambit compositions tend to have many notes per beat (4–8 subdivisions); drut tends to have one note per beat.

**Palta** compositions have **no laya** — they're practiced at varying speeds.

## Adding your own catalog

The catalog of built-in raags and taals lives in `sangeet-core/src/main/scala/com/varpas/sangeet/core/raag/Raags.scala` and `taal/Taals.scala`. Contributions adding new raags are welcome — open a PR on the [GitHub repository](https://github.com/bharath12345/sangeet_notes_editor).

## What to read next

- [Starting Beat](10-starting-beat.md) — mukhda and pickup beats before sam
- [Creating Compositions](02-creating-compositions.md) — the New dialog
