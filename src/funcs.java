import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**Une classe contenant des methodes statiques bien utiles
 **/
public class funcs {

    /**DECRYPTER la clef aes grace à sa clef privée rsa quand on rejoint un groupe ou le serveur
     * @param tableau un string de la forme [..., ..., ...]
     **/
	static SecretKey RSA_DECRYPT(String tableau, PrivateKey clef_privee) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, clef_privee);
		return new SecretKeySpec(cipher.doFinal(stringToByte_Mais_pff(tableau)), "AES");
	}

    /**ENCRYPTER la clef aes grace à la clef publique rsa du client quand il rejoint un groupe ou le serveur
     * @param tableau un string de la forme [..., ..., ...]
     **/
	static String RSA_ENCRYPT(SecretKey clef_aes, String tableau) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(tableau));
        // permte d'éviter les problemes de chiffrement en base 64
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PublicKey clef_publique = factory.generatePublic(spec);
		cipher.init(Cipher.ENCRYPT_MODE, clef_publique);
		return Arrays.toString(cipher.doFinal(clef_aes.getEncoded()));

	}
	
	/**
	 * prend un string qui représente un tableau de byte et ça renvoie le tableau de byte
	 * @param tableau un string de la forme [..., ..., ...]
	 */
	static byte[] stringToByte_Mais_pff(String tableau) {
		tableau = tableau.substring(1, tableau.length() - 1);
		String[] tab = tableau.split(", ");
		byte[] res = new byte[tab.length];
		for (int i = 0; i < tab.length; i++) {
			res[i] = (byte) Integer.parseInt(tab[i]);

		}
		return res;
	}
}
