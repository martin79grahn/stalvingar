#!/usr/bin/env python3
"""Stålvingar for Linux: runs the game (index.html next to this file) in a WebKitGTK window.

F11 toggles fullscreen; --fullscreen starts in fullscreen; --windowed forces a window.
High score and sound setting are kept in ~/.local/share/stalvingar.
At startup it asks the Release Store for a newer version (update.json next to this file).
Needs python3-gobject with GTK 3 and WebKit2GTK 4.1 (preinstalled on Fedora/Nobara/Ubuntu desktops).
"""
import json
import os
import sys
import threading
import urllib.parse
import urllib.request

# WebKitGTK's DMA-BUF renderer dies with "Error 71 (protocol error)" on some Wayland setups
os.environ.setdefault("WEBKIT_DISABLE_DMABUF_RENDERER", "1")

import gi  # noqa: E402

gi.require_version("Gtk", "3.0")
gi.require_version("Gdk", "3.0")
gi.require_version("WebKit2", "4.1")
from gi.repository import Gdk, GLib, Gtk, WebKit2  # noqa: E402

HERE = os.path.dirname(os.path.realpath(__file__))
VERSION = "dev"  # build-linux.sh writes the release version here
PAGE = next(p for p in (os.path.join(HERE, "index.html"), os.path.join(HERE, "..", "game", "index.html")) if os.path.exists(p))


def newer(a, b):
    def parts(v):
        out = []
        for x in v.lstrip("vV").split("-")[0].split("."):
            digits = "".join(c for c in x if c.isdigit())
            out.append(int(digits or 0))
        return out
    pa, pb = parts(a), parts(b)
    n = max(len(pa), len(pb))
    return pa + [0] * (n - len(pa)) > pb + [0] * (n - len(pb))


def check_update(win):
    try:
        with open(os.path.join(HERE, "update.json")) as f:
            cfg = json.load(f)
    except (OSError, ValueError):
        return
    if VERSION == "dev":
        return
    for store in cfg["store"].replace(",", " ").split():
        store = store.rstrip("/")
        url = "%s/api/update/%s?platform=linux&key=%s" % (store, cfg["repo"], urllib.parse.quote(cfg.get("key", "")))
        try:
            with urllib.request.urlopen(url, timeout=5) as r:
                d = json.load(r)
        except Exception:  # noqa: BLE001
            continue
        if newer(d.get("version", "0"), VERSION):
            GLib.idle_add(offer, win, d, store + d.get("store_page", "/"))
        return


def offer(win, d, page):
    dlg = Gtk.MessageDialog(transient_for=win, modal=True, message_type=Gtk.MessageType.INFO,
                            buttons=Gtk.ButtonsType.NONE, text="Ny version finns!")
    dlg.format_secondary_text("%s  →  %s\n\n%s" % (VERSION, d["version"], (d.get("notes") or "")[:400]))
    dlg.add_buttons("Senare", Gtk.ResponseType.CANCEL, "Öppna appbutiken", Gtk.ResponseType.OK)
    if dlg.run() == Gtk.ResponseType.OK:
        Gtk.show_uri_on_window(win, page, Gtk.get_current_event_time())
    dlg.destroy()
    return False


def main():
    data = os.path.join(GLib.get_user_data_dir(), "stalvingar")
    cache = os.path.join(GLib.get_user_cache_dir(), "stalvingar")
    mgr = WebKit2.WebsiteDataManager(base_data_directory=data, base_cache_directory=cache)
    view = WebKit2.WebView.new_with_context(WebKit2.WebContext.new_with_website_data_manager(mgr))
    s = view.get_settings()
    s.set_enable_webaudio(True)
    s.set_media_playback_requires_user_gesture(False)
    s.set_allow_file_access_from_file_urls(True)
    s.set_enable_developer_extras("--dev" in sys.argv)
    view.set_background_color(Gdk.RGBA(0, 0, 0, 1))

    win = Gtk.Window(title="Stålvingar")
    win.set_default_size(1152, 648)
    icon = os.path.join(HERE, "stalvingar.png")
    if os.path.exists(icon):
        win.set_icon_from_file(icon)
    win.add(view)
    full = ["--fullscreen" in sys.argv]

    def on_key(_w, ev):
        if ev.keyval == Gdk.KEY_F11:
            full[0] = not full[0]
            win.fullscreen() if full[0] else win.unfullscreen()
            return True
        return False

    win.connect("key-press-event", on_key)
    win.connect("destroy", Gtk.main_quit)
    view.load_uri(GLib.filename_to_uri(os.path.realpath(PAGE)))
    win.show_all()
    if full[0]:
        win.fullscreen()
    view.grab_focus()
    threading.Thread(target=check_update, args=(win,), daemon=True).start()
    Gtk.main()


if __name__ == "__main__":
    main()
