package com.itexpert.content.lib.utils.crypto;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtils {

    public static KeyPair genererCle() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());

        return keyPairGenerator.generateKeyPair();
    }

    public static String crypt(String data, String publicKeyStr) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKeyFromString(publicKeyStr));
        return encodeHexString(cipher.doFinal(data.getBytes()));
    }

    public static String decrypt(String data, String privateKeyStr) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKeyFromString(privateKeyStr));
            return encodeHexString(cipher.doFinal(decodeHexString(data)));
        } catch (Exception ex) {
            return null;
        }
    }

    public static PublicKey getPublicKeyFromString(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr.getBytes("utf-8"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);
        return publicKey;
    }

    public static PrivateKey getPrivateKeyFromString(String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr.getBytes("utf-8"));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = fact.generatePrivate(keySpec);
        return privateKey;
    }

    private static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    private static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: " + hexChar);
        }
        return digit;
    }

    public static void main(String[] args) throws Exception {
        // Génération d'une paire de clés
       /* KeyPair keyPair = genererCle();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        PrivateKey privateKey = keyPair.getPrivate();
        String privateKeyStr =  Base64.getEncoder().encodeToString(privateKey.getEncoded());

        System.err.println();
        System.err.println();
        System.err.println("publicKeyStr:  "+publicKeyStr);
        System.err.println();
        System.err.println("privateKeyStr:  "+privateKeyStr);
        System.err.println();
        System.err.println();

        // Texte à crypter
        //String texteClair = "Ceci est un texte à crypter.";
                String texteClair = "{\n  \"product\": \"expety-content\",\n  \"varsion\": \"1.0\",\n  \"customet\": \"azirarm@gmail.com\",\n  \"licence-type\": \"FREE\",\n  \"max-users\": \"1\",\n  \"max-nodes\": \"1\",\n  \"start-date\":\"2112331313\",\n  \"end-date\":\"3231232145\"\n}";

        // Cryptage
        String texteCrypte = crypt(encodeHexString(texteClair.getBytes()), publicKeyStr);
        System.err.println("Texte crypté : " +  texteCrypte);

*/
        String textClaire = "{\"product\":\"expert-content\",\"varsion\":\"1.0\",\"customer\":\"azirarm@gmail.com\",\"type\":\"FREE\",\"startDate\":1731353529,\"endDate\":1762889512}";
        String publicKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwhK48BOadEyWGHkj/wA/7XfDngZqhq4iVfadJjlTSufeo0sHTXSro/sbgDsQplHDlaCW67s7kvoD9cQpNVr4ecEvHnaiQUZdIn/Ome62ciSPcsx0l+9d353/x+bbg5EtuCroHwxXy3jvjaedzoeCt0l9vVxA5/KBZdFalEpqZGH8JP7ITla10aT+I9Kl5LlvDE9XQtZydB4Cp6dD2ssZg6o+qV88/FPkOwea1HPtljR0A4ouG2EotyCcd/WGAFYye05RlH8PD00B4A93zfS8gWUaeM0fDAvR3JFtyrdAuQy2ooteO4ZMF2T55B30ut95Lb/8HoESOiWNHYzrvh+yDQIDAQAB";
        String privateKeyStr = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDCErjwE5p0TJYYeSP/AD/td8OeBmqGriJV9p0mOVNK596jSwdNdKuj+xuAOxCmUcOVoJbruzuS+gP1xCk1Wvh5wS8edqJBRl0if86Z7rZyJI9yzHSX713fnf/H5tuDkS24KugfDFfLeO+Np53Oh4K3SX29XEDn8oFl0VqUSmpkYfwk/shOVrXRpP4j0qXkuW8MT1dC1nJ0HgKnp0PayxmDqj6pXzz8U+Q7B5rUc+2WNHQDii4bYSi3IJx39YYAVjJ7TlGUfw8PTQHgD3fN9LyBZRp4zR8MC9HckW3Kt0C5DLaii147hkwXZPnkHfS633ktv/wegRI6JY0djOu+H7INAgMBAAECggEAUPZ3kaxT8LWsnlwXUL/9a2ddw9SgZzxJFbyy1anvnh26Szw3OTB1lIzi7a12ZjRRGGBs5b3v/gJwWXyyxlADveOCcT6DtSCSJRzVh3FHSroG4Bj8gQ/6hRoIhZBBPpN308+Ok7lV/QwgP+PP2UO+HBG8M53DdCA+rEfY9mlPFj3yGT3PRJZd84HO/tUKPi95Woc204b4eZQR2WQTqD2iAwUavL6AxCOP3+OKf1G3OOx8E2OMxVtK93kRDDXcx6mTFCPUsPilu5nVGw0BOUUawCNkf8u2U7+v++gKZuae96E5z3ylu/4mhMEnz8LXR9sitwk+dFKX10S/byvGXYieHwKBgQDtL47kVGDaHFrx2/NljqYBY57hXDukcZwtkF24geC5dohHMSFi1reok/X6oc5ZNwFNUfm2m731gUi74dISktOguDQt3qI2GqtQc9ne7zmDQlkVxq9sNe8HgDyj8AuCMzfvvh5VW0tkvfSsHsOiDqQeLxGgbN1mgPdhUwGRu0YBnwKBgQDRd7EyCj8rKZbvfKrR/IlO+Z3UKO10lsC0H3BWIKJUsl4TyTkwZrX/DGjiZMkqL1iiAY3+TLRm+mKoHi9QfO7TpUo2d5aWWfYyH6rPbsPzOqxDI3U9kwAK66DglGctY0OtdZUYCeJeNF4LZ2dbPQrTBJlhK74G+JHaFkE8O44k0wKBgE6I5MNtvPP7V5Glxp3l8cmb9sugAo+cnp3no2CogIRoXw9TkT5s4jsLYvMuCGk58VBsyEZSoz9bI34yc6B6xV/+HijiAsvvYLIu6gELLPvtSBoRw5wM76yIJYwSu3zNAMR9KwYQSJJ+4zjqptRODMGBzFBWO6jpIwAyv95FKBT5AoGAK/q1eYwtNyLETgQe2XfsXGiMer8dHB2K2EbBj1PKwX2xwvW8zCzEiYhRfTcsBW9rbJhm972jIvvvOt0bkgqpGkbaDhWNyIOa7Ws4sdTPz4HCKVaIhyUWHTTb0ktWXnE5BHJrm2B35s+X700GpmFqpGOS0JQekdgex2tkxwC6F4ECgYA95T9JzMFik6uZNcUgVAYBUoc23v3JLU6TbzWJI81QUdEbBg2D5ojbBWYtZTmOtjvr2w0sdU3efDhDYOnRknVMpdoBgN01GSZjvsRwC8zA1UZ64s69lkEjZM4qHNJwf6mEdVZM359hCdUPVYkGCZV6rtoe4lovZ0mgjk6mxxbaNg==";
        //String texteCrypte="69d1d544cf3db3a3ac91ef346a775f19c56c39bceec98a1c1762d150c5042b1dfc91757aaf9f67230c7a1c94ff5f4f83c026721cc2d3861956898af91d886ae8c1a6a01fad5286a1db02f87dd5192ef6804e2946b236219c469458e9b9c70bbcd5e1c00f8f5fcbe5239f82926e72e50e804035e298ef820dcd5030972a7ce660767a064472eefcbfa8397f01b02d948cbbd194bf34d077ae682e7421beef56ac932fc722080ca33a58b4d0c71d6e016cc0098b5273d74921c9425d55fcd454364f5c44037dd9a8fc18bb54a78012e1ddee82a9d095da32da421ce1f7608703ad03c3a77b83c6739d47145811938a260a7b445c90cabc7f31269b2c257187c6ec";
        String texteCrypte = crypt(textClaire, publicKeyStr);
        System.err.println("Texte crypté : " + texteCrypte);
        // Décryptage
        String texteDecrypte = decrypt(texteCrypte, privateKeyStr);
        System.err.println("Texte décrypté : " + new String(decodeHexString(texteDecrypte)));
    }
}
