package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class PasswordService {

    private static final int SALT_BYTES = 16;      // 16 bytes -> 32 hex chars
    private static final int KEY_BITS = 256;       // 256-bit -> 64 hex chars
    private static final int ITERATIONS = 120_000; // tune later

    private final SecureRandom random = new SecureRandom();

    // ============================
    // Registration
    // ============================

    public SaltHash newSaltHash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_BITS);
        wipe(password);
        return new SaltHash(toHex(hash), toHex(salt));
    }

    // ============================
    // Login verification
    // ============================

    public boolean verify(char[] password, String saltHex, String expectedHashHex) {
        byte[] salt = fromHex(saltHex);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_BITS);
        wipe(password);
        return constantTimeEquals(toHex(hash), expectedHashHex);
    }

    // Praktisk overload for LoginForm (String → char[])
    public boolean verify(String password, String saltHex, String expectedHashHex) {
        return verify(password.toCharArray(), saltHex, expectedHashHex);
    }

    // ============================
    // Internal helpers
    // ============================

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke lage passord-hash", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

    public record SaltHash(String hashHex, String saltHex) {}
}
