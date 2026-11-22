package se.martin79grahn.stalvingar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Asks the Release Store whether a newer release exists. If so it offers "Uppdatera", downloads
 * the APK and hands it to Android's package installer.
 * Settings come from assets/update.json (written by build.sh): {"store", "repo", "key"}, where
 * "store" may list several addresses separated by spaces (home Wi-Fi, then Tailscale), tried in order.
 * If no store can be reached nothing happens.
 */
final class Updater {
    static final String ACTION_INSTALL = "se.martin79grahn.stalvingar.INSTALL_STATUS";
    private static final String TAG = "Updater";
    private final Activity a;
    private String store, repo, key;

    Updater(Activity a) {
        this.a = a;
    }

    /** Automatic check at startup: silent unless there is an update. */
    void check() {
        check(false, null);
    }

    /**
     * With manual = true it also says when you're up to date or the
     * store can't be reached. done runs on the UI thread when the check is over.
     */
    void check(boolean manual, Runnable done) {
        new Thread(() -> {
            String info = null, detail = null;
            try {
                JSONObject cfg = new JSONObject(new String(readAll(a.getAssets().open("update.json")), "UTF-8"));
                repo = cfg.getString("repo");
                key = cfg.optString("key", "");
                JSONObject d = null;
                Exception last = null;
                for (String s : cfg.getString("store").trim().split("[\\s,]+")) {
                    try {
                        store = s.replaceAll("/+$", "");
                        d = new JSONObject(new String(get(store + "/api/update/" + repo
                                + "?platform=android&key=" + enc(key)), "UTF-8"));
                        break;
                    } catch (Exception e) {
                        last = e;
                    }
                }
                if (d == null) throw last;
                String latest = d.getString("version");
                String current = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
                if (d.isNull("download_url") || !isNewer(latest, current)) {
                    Log.i(TAG, "up to date: " + current + ", latest " + latest);
                    info = "Du har senaste versionen";
                    detail = "Version " + current;
                    return;
                }
                String url = d.getString("download_url");
                if (url.startsWith("/")) url = store + url + (url.contains("?") ? "&" : "?") + "key=" + enc(key);
                final String u = url;
                final String notes = clean(d.isNull("notes") ? "" : d.getString("notes"));
                a.runOnUiThread(() -> ask(current, latest, notes, u));
            } catch (java.io.FileNotFoundException e) {
                info = "Uppdateringskollen är avstängd";
                detail = "Appen byggdes utan adress till appbutiken.";
            } catch (Exception e) {
                Log.i(TAG, "update check failed: " + e);
                info = "Kunde inte söka efter uppdateringar";
                detail = "Appbutiken nås hemma på samma wifi som datorn, eller var som helst med Tailscale påslaget.";
            } finally {
                final String i = info, dt = detail;
                a.runOnUiThread(() -> {
                    if (done != null) done.run();
                    if (manual && i != null && !a.isFinishing())
                        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle(i).setMessage(dt).setPositiveButton("OK", null).show();
                });
            }
        }).start();
    }

    private void ask(String current, String latest, String notes, String url) {
        if (a.isFinishing()) return;
        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Ny version finns!")
                .setMessage(current + "  →  " + latest + (notes.isEmpty() ? "" : "\n\n" + notes))
                .setPositiveButton("Uppdatera", (dlg, w) -> install(url))
                .setNegativeButton("Senare", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void install(String url) {
        ProgressDialog p = new ProgressDialog(a, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        p.setTitle("Laddar ner…");
        p.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        p.setProgressNumberFormat(null);
        p.setCancelable(false);
        p.show();
        new Thread(() -> {
            PackageInstaller.Session s = null;
            try {
                HttpURLConnection c = open(url);
                long len = c.getContentLengthLong();
                PackageInstaller pi = a.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                s = pi.openSession(pi.createSession(params));
                try (InputStream in = c.getInputStream(); OutputStream out = s.openWrite("update.apk", 0, len)) {
                    byte[] buf = new byte[1 << 16];
                    long done = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (len > 0) {
                            final int pct = (int) (done * 100 / len);
                            a.runOnUiThread(() -> p.setProgress(pct));
                        }
                    }
                    s.fsync(out);
                }
                Intent i = new Intent(a, MainActivity.class).setAction(ACTION_INSTALL);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT
                        | (android.os.Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                s.commit(PendingIntent.getActivity(a, 0, i, flags).getIntentSender());
                s.close();
                a.runOnUiThread(p::dismiss);
            } catch (Exception e) {
                Log.w(TAG, "in-app install failed, opening browser", e);
                if (s != null) s.abandon();
                a.runOnUiThread(() -> {
                    p.dismiss();
                    a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                });
            }
        }).start();
    }

    /** Called from MainActivity.onNewIntent with the installer's status. */
    void onInstallStatus(Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @SuppressWarnings("deprecation")
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) a.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (status != PackageInstaller.STATUS_SUCCESS && status != PackageInstaller.STATUS_FAILURE_ABORTED) {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(a, "Uppdateringen misslyckades" + (msg != null ? ": " + msg : ""), Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ helpers

    static boolean isNewer(String a, String b) {
        String[] pa = a.replaceFirst("^[vV]", "").split("-")[0].split("\\.");
        String[] pb = b.replaceFirst("^[vV]", "").split("-")[0].split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int x = i < pa.length ? toInt(pa[i]) : 0, y = i < pb.length ? toInt(pb[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String clean(String md) {
        StringBuilder out = new StringBuilder();
        int lines = 0;
        for (String l : md.split("\n")) {
            if (l.trim().startsWith("#")) {  // a heading: skip the first, stop at the next
                if (lines > 0) break;
                continue;
            }
            l = l.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replaceAll("^\\s*[-*+]\\s+", "• ")
                    .replaceAll("[#*`_>]+", "").trim();
            if (l.isEmpty()) continue;
            out.append(l).append('\n');
            if (++lines >= 6) break;
        }
        String s = out.toString().trim();
        return s.length() > 320 ? s.substring(0, 320) + "…" : s;
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        return c;
    }

    private static byte[] get(String url) throws Exception {
        try (InputStream in = open(url).getInputStream()) {
            return readAll(in);
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }
}
