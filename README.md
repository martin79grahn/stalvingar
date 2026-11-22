# Stålvingar

A horizontal propeller-plane shoot 'em up: seven stages (dawn, storm, sunset, night, arctic, volcano, iron tempest)
with five kinds of boss (flying fortress, battleship, aircraft carrier, flying-boat gunship, missile submarine),
then the loop repeats harder. One HTML5 canvas file (`game/index.html`)
with all graphics and sound generated in code.

## Planes
Chosen after the title screen (remembered):
- **FALK**: fast interceptor. Light rapid guns, narrow spread, a RAIL shot that pierces, small quick bombs.
- **BJÖRN**: slow heavy fighter. Heavy shells (about 30 % more damage), wide spread, a big piercing CANNON, heavy bombs with a wide blast.

## Power bar
Red planes, and a whole red squadron shot down, drop orange pods. Each pod moves the highlight one box
along the bar at the bottom; POWER takes the lit upgrade:

| Box | Levels | Effect |
|---|---|---|
| SPEED | 5 | faster plane |
| TWIN | 3 | spread shot, then faster fire |
| BOMB | 2 | bombs that fall onto ships and planes below |
| REAR | 2 | tail gunner (back, then back + diagonals) |
| CANNON / RAIL | 3 | piercing shots, faster per level |
| WINGS | 2 | wingmen that follow your path and fire with you |
| SHIELD | 3 hits | absorbs hits; taking it again refills it |

Rare blue pods destroy everything on screen. Gunfire can shoot down the submarine's missiles. Losing a ship loses all upgrades. Gold medals give points.

## Controls
Keyboard: arrows/WASD, Z fire, X power, P pause, M sound, F11 fullscreen (Linux).
Gamepad: stick/d-pad, A fire, B power, Start pause.
Touch: drag anywhere to fly (fires automatically), tap POWER or the bar.

## Builds
- `./build-linux.sh` → `dist/Stalvingar-linux-vX.tar.gz` (the page + a WebKitGTK launcher, `stalvingar.sh` starts it, `install` adds a menu entry; both check the Release Store for updates)
- `VERSION=x CODE=n ./build-apk.sh` → `out/Stalvingar-vX.apk` (fullscreen landscape WebView), signed with
  `~/.android/stalvingar-release.keystore` (alias stalvingar, password in `~/.android/stalvingar-release.pass`)
- or just open `game/index.html` in a browser.
