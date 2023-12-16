package me.vasler.utils;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FileUtil {

    public static List<TempFiles> files = new ArrayList<>();

    public static List<File> getFiles(String path) {
        final List<File> files = new ArrayList<>();
        File[] file1 = new File(path).listFiles();
        if (file1 != null) {
            for (File file : file1) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    File[] file2 = new File(file.getPath()).listFiles();
                    if (file2 != null) {
                        for (File file3 : file2) {
                            if (file3.isFile()) {
                                files.add(file3);
                            }
                        }
                    }
                }
            }
        }
        return files;
    }

    public static void fixCryptographyKeyLength() {
        String errorString = "Failed manually overriding key-length permissions.";
        int newMaxKeyLength;
        try {
            if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
                Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                Constructor con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissionCollection = con.newInstance();
                Field f = c.getDeclaredField("all_allowed");
                f.setAccessible(true);
                f.setBoolean(allPermissionCollection, true);

                c = Class.forName("javax.crypto.CryptoPermissions");
                con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissions = con.newInstance();
                f = c.getDeclaredField("perms");
                f.setAccessible(true);
                ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

                c = Class.forName("javax.crypto.JceSecurityManager");
                f = c.getDeclaredField("defaultPolicy");
                f.setAccessible(true);
                Field mf = Field.class.getDeclaredField("modifiers");
                mf.setAccessible(true);
                mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                f.set(null, allPermissions);

                newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            }
        } catch (Exception e) {
            throw new RuntimeException(errorString, e);
        }
        if (newMaxKeyLength < 256)
            throw new RuntimeException(errorString); // hack failed
    }

    public static String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }


    public static void close(TempFiles file)
    {
        try
        {
            new File(file.newpath).delete();
        }
        catch (Exception e)
        {

        }
    }

    public static TempFiles readFile(String path)
    {
        TempFiles tempfile = new TempFiles();
        tempfile.oldpath = path;

        String tempdir = System.getProperty("java.io.tmpdir");
        tempfile.newpath = tempdir + generateName(15);

        files.add(tempfile);

        try
        {
            FileChannel in = new FileInputStream(path).getChannel();
            FileChannel out = new FileOutputStream(tempfile.newpath).getChannel();

            out.transferFrom(in, 0, in.size());

            in.close();
            out.close();

            tempfile.bytes = Files.readAllBytes(new File(tempfile.newpath).toPath());

            return tempfile;
        }
        catch (Exception e)
        {
            //e.printStackTrace();
        }

        return null;
    }

    public static String generateName(int len)
    {
        String table = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < len)
            sb.append(table.charAt(new Random().nextInt(table.length())));
        return sb.toString();
    }
}