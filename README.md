# Stålvingar

A horizontal propeller-plane shoot 'em up: four stages (dawn, storm, sunset, night), two kinds of boss
(flying fortress and battleship), then the loop repeats harder. One HTML5 canvas file (`game/index.html`)
with all graphics and sound generated in code.

## Power bar
Red planes, and a whole red squadron shot down, drop orange pods. Each pod moves the highlight one box
along the bar at the bottom; POWER takes the lit upgrade:

| Box | Levels | Effect |
|---|---|---|
| SPEED | 5 | faster plane |
| TWIN | 3 | spread shot, then faster fire |
| BOMB | 2 | bombs that fall onto ships and planes below |
| REAR | 2 | tail gunner (back, then back + diagonals) |
| CANNON | 3 | piercing cannon shells, faster per level |
| WINGS | 2 | wingmen that follow your path and fire with you |
| SHIELD | 3 hits | absorbs hits; taking it again refills it |

Rare blue pods destroy everything on screen. Losing a ship loses all upgrades. Gold medals give points.

## Controls
Keyboard: arrows/WASD, Z fire, X power, P pause, M sound, F11 fullscreen (Linux).
Gamepad: stick/d-pad, A fire, B power, Start pause.
Touch: drag anywhere to fly (fires automatically), tap POWER or the bar.

## Builds
- `./build-linux.sh` → `dist/Stalvingar-linux-vX.tar.gz` (the page + a WebKitGTK launcher, `install.sh` adds a menu entry)
- `VERSION=x CODE=n ./build-apk.sh` → `out/Stalvingar-vX.apk` (fullscreen landscape WebView), signed with
  `~/.android/stalvingar-release.keystore` (alias stalvingar, password in `~/.android/stalvingar-release.pass`)
- or just open `game/index.html` in a browser.
