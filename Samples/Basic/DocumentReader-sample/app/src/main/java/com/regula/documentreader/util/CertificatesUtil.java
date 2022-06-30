package com.regula.documentreader.util;

import android.content.res.AssetManager;

import com.regula.documentreader.api.enums.PKDResourceType;
import com.regula.documentreader.api.params.rfid.PKDCertificate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertificatesUtil {

   public static List<PKDCertificate> getRfidCertificates(AssetManager am, String certificatesDir) {
      List<PKDCertificate> pkdCertificatesList = new ArrayList<>();
      try {
         String list[] = am.list(certificatesDir);
         if (list != null && list.length > 0) {
            for (String file : list) {
               String[] findExtension = file.split("\\.");
               int pkdResourceType = 0;
               if (findExtension.length > 0) {
                  pkdResourceType = PKDResourceType.getType(findExtension[findExtension.length - 1].toLowerCase());
               }

               InputStream licInput = am.open(certificatesDir+"/"+file);
               int available = licInput.available();
               byte[] binaryData = new byte[available];
               licInput.read(binaryData);

               PKDCertificate certificate = new PKDCertificate(binaryData, pkdResourceType);
               pkdCertificatesList.add(certificate);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return pkdCertificatesList;
   }

   public static List<PKDCertificate> getRfidTACertificates(AssetManager am) {
      List<PKDCertificate> pkdCertificatesList = new ArrayList<>();
      String certificatesDir = "Regula/certificates_ta";
      try {
         Map<String, List<String>> filesCertMap = new HashMap<>();
         String list[] = am.list(certificatesDir);
         if (list != null && list.length > 0) {
            for (String file : list) {
               String[] findExtension = file.split("\\.");
               if (!filesCertMap.containsKey(findExtension[0])) {
                  List<String> certList = new ArrayList<>();
                  certList.add(file);
                  filesCertMap.put(findExtension[0], certList);
               } else {
                  filesCertMap.get(findExtension[0]).add(file);
               }
            }
         }

         for (Map.Entry me : filesCertMap.entrySet()) {
            List<String> files = (List<String>) me.getValue();
            PKDCertificate certificate = new PKDCertificate();
            for(String file : files) {
               String[] findExtension = file.split("\\.");
               certificate.resourceType = PKDResourceType.CERTIFICATE_TA;
               InputStream licInput = am.open(certificatesDir+"/"+file);
               int available = licInput.available();
               byte[] binaryData = new byte[available];
               licInput.read(binaryData);
               if (findExtension[1].equals("cvCert")) {
                  certificate.binaryData = binaryData;
               } else {
                  certificate.privateKey = binaryData;
               }
            }
            pkdCertificatesList.add(certificate);
         }

      } catch (IOException e) {
         e.printStackTrace();
      }
      return pkdCertificatesList;
   }
}
