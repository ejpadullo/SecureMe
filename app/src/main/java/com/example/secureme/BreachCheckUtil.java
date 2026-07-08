// BreachCheckUtil.java
// Utility class to check if a password has been breached using HaveIBeenPwned's API

package com.example.secureme;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;

public class BreachCheckUtil {

    public interface BreachCheckCallback {
        void onResult(boolean isBreached);
    }

    public static void checkPasswordBreach(String password, BreachCheckCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] hashBytes = digest.digest(password.getBytes());

                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    String fullHash = sb.toString().toUpperCase(Locale.ROOT);
                    String prefix = fullHash.substring(0, 5);
                    String suffix = fullHash.substring(5);

                    URL url = new URL("https://api.pwnedpasswords.com/range/" + prefix);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(suffix)) {
                            return true;
                        }
                    }
                    reader.close();
                } catch (Exception e) {
                    Log.e("BreachCheck", "Error checking password breach", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean breached) {
                callback.onResult(breached);
            }
        }.execute();
    }

    // ✅ NEW: Generate a secure random password
    public static String generateSecurePassword(int length) {
        final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lower = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String symbols = "!@#$%^&*()-_=+[]{}|;:'\",.<>?/";
        final String all = upper + lower + digits + symbols;

        Random random = new Random();
        StringBuilder password = new StringBuilder();

        // Ensure password has at least one from each category
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(symbols.charAt(random.nextInt(symbols.length())));

        for (int i = 4; i < length; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        return password.toString();
    }
}
