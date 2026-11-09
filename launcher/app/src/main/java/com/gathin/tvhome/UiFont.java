package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The typeface behind one of the font_* strings in res/values/fonts.xml, for views built in code. The string
 * points at a bundled font, or, in locales Outfit can't draw (values-vi), names a system family (#833).
 */
final class UiFont {
    private UiFont() {}

    /** Typefaces loaded from a cache file on API < 26, by resource id: the copy happens once per font. */
    private static final Map<Integer, Typeface> sFileCache = new HashMap<>();

    static Typeface get(Context context, int fontString) {
        Resources res = context.getResources();
        TypedValue value = new TypedValue();
        res.getValue(fontString, value, true);
        String name = String.valueOf(value.string);
        if (name.startsWith("res/")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return res.getFont(value.resourceId);
            return fromFile(context);
        }
        return Typeface.create(name, Typeface.NORMAL);
    }

    /**
     * API 25 has no Resources.getFont. All four res/ fonts are &lt;font-family&gt; wrappers around the one
     * variable font @font/outfit, and the weights are ignored below API 28, so on API 25 the bundled font
     * is that file, loaded the way the support library does it: copied to a cache file once, then created
     * from the file. Any failure leaves the default typeface instead of the bundled font.
     */
    private static Typeface fromFile(Context context) {
        synchronized (sFileCache) {
            Typeface cached = sFileCache.get(R.font.outfit);
            if (cached != null) return cached;
            Typeface loaded = loadFile(context);
            if (loaded == null) loaded = Typeface.DEFAULT;
            sFileCache.put(R.font.outfit, loaded);
            return loaded;
        }
    }

    // A font file is stored raw in the APK, so openRawResource reads its bytes (the same trick the
    // support library's ResourcesCompat uses); lint only accepts raw ids there.
    @SuppressLint("ResourceType")
    private static Typeface loadFile(Context context) {
        File file = new File(context.getCacheDir(), "font-outfit.ttf");
        try (InputStream in = context.getResources().openRawResource(R.font.outfit);
             OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            return Typeface.createFromFile(file);
        } catch (Exception e) {
            return null;
        }
    }
}
