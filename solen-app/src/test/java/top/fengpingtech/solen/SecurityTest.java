package top.fengpingtech.solen;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SecurityTest {
    public static void main(String[] args)  throws Exception {
        String content = "study hard and make progress everyday";
        System.out.println("content :"+content);

        KeyPair keyPair = getKeyPair();
        System.out.println("keyPair =============== " + keyPair);
        PublicKey publicKey =  keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("publicKey ================== " + publicKey);
        System.out.println("privateKey ================== " + privateKey);
        String md5Sign  = getMd5Sign(content,privateKey);
        System.out.println("sign with md5 and rsa :"+ md5Sign);
        boolean md5Verifty = verifyWhenMd5Sign(content,md5Sign,publicKey);
        System.out.println("verify sign with md5 and rsa :"+ md5Verifty);

        String sha1Sign  = getSha1Sign(content,privateKey);
        System.out.println("sign with sha1 and rsa :"+ sha1Sign);
        boolean sha1Verifty = verifyWhenSha1Sign(content,sha1Sign,publicKey);
        System.out.println("verify sign with sha1 and rsa :"+ sha1Verifty);

    }

    //生成密钥对
    static KeyPair getKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512); //可以理解为：加密后的密文长度，实际原文要小些 越大 加密解密越慢
        return keyGen.generateKeyPair();
    }

    //用md5生成内容摘要，再用RSA的私钥加密，进而生成数字签名
    static String getMd5Sign(String content , PrivateKey privateKey) throws Exception {
        byte[] contentBytes = content.getBytes("utf-8");
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(privateKey);
        signature.update(contentBytes);
        byte[] signs = signature.sign();
        return Base64.getEncoder().encodeToString(signs);
    }

    //对用md5和RSA私钥生成的数字签名进行验证
    static boolean verifyWhenMd5Sign(String content, String sign, PublicKey publicKey) throws Exception {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(publicKey);
        signature.update(contentBytes);
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    //用sha1生成内容摘要，再用RSA的私钥加密，进而生成数字签名
    static String getSha1Sign(String content , PrivateKey privateKey) throws Exception {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(contentBytes);
        byte[] signs = signature.sign();
        return Base64.getEncoder().encodeToString(signs);
    }

    //对用md5和RSA私钥生成的数字签名进行验证
    static boolean verifyWhenSha1Sign(String content, String sign, PublicKey publicKey) throws Exception {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(publicKey);
        signature.update(contentBytes);
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    /**
     * RSA签名
     * @param content 待签名数据
     * @param privateKey 商户私钥
     * @param input_charset 编码格式
     * @return 签名值
     */
    public static String sign(String content, String privateKey, String input_charset)
    {
        try
        {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec( Base64.getDecoder().decode(privateKey) );
            KeyFactory keyf = KeyFactory.getInstance("RSA");
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);

            Signature signature = Signature
                    .getInstance("SHA1WithRSA");

            signature.initSign(priKey);
            signature.update( content.getBytes(input_charset) );

            byte[] signed = signature.sign();

            return  Base64.getEncoder().encodeToString(signed);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
