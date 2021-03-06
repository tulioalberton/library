/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.tom.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 *
 * @author alysson
 */
public class SignatureTest {

    public static void main(String[] args) throws Exception {
        String signatureInstance = "SHA512withRSA";
        int keySize = 1024;
        int iterations = 1000;
        
    	byte[] data = new byte[20];
        byte[] signature;
        Signature signEng;
        long start, end;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        KeyPair kp = kpg.genKeyPair();
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        signEng = Signature.getInstance(signatureInstance);

        for(int i=0; i<iterations; i++) {
            signEng = Signature.getInstance(signatureInstance);
            signEng.initSign(privateKey);
        }
        start = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            signEng = Signature.getInstance(signatureInstance);
            signEng.initSign(privateKey);
        }
        end = System.currentTimeMillis();
        System.out.println("" + iterations + " init sign: "+(end-start)+"ms");

        for(int i=0; i<iterations; i++) {
            signEng.update(data);
            signature = signEng.sign();
        }
        start = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            signEng.update(data);
            signature = signEng.sign();
        }
        end = System.currentTimeMillis();
        System.out.println("" + iterations + " sign: "+(end-start)+"ms");

        signEng.update(data);
        signature = signEng.sign();

        for(int i=0; i<iterations; i++) {
            signEng = Signature.getInstance(signatureInstance);
            signEng.initVerify(publicKey);
        }
        start = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            signEng = Signature.getInstance(signatureInstance);
            signEng.initVerify(publicKey);
        }
        end = System.currentTimeMillis();
        System.out.println("" + iterations+ " init verify: "+(end-start)+"ms");

        for(int i=0; i<iterations; i++) {
            signEng.update(data);
            signEng.verify(signature);
        }
        boolean ok = false;
        start = System.currentTimeMillis();
        for(int i=0; i<iterations; i++) {
            signEng.update(data);
            ok = signEng.verify(signature);
        }
        end = System.currentTimeMillis();
        System.out.println("" + iterations + " verify: "+(end-start)+"ms " + ok);    
        
    }

}
