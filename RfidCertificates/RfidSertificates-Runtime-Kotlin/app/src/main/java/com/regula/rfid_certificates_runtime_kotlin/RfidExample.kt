package com.regula.rfid_certificates_runtime_kotlin

import android.content.Context
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.rfid.IRfidPKDCertificateCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderRequest
import com.regula.documentreader.api.completions.rfid.IRfidTASignatureCompletion
import com.regula.documentreader.api.completions.rfid.certificates.IRfidPACertificates
import com.regula.documentreader.api.completions.rfid.certificates.IRfidTACertificates
import com.regula.documentreader.api.completions.rfid.certificates.IRfidTASignature
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.rfid.authorization.PAResourcesIssuer
import com.regula.documentreader.api.params.rfid.authorization.TAChallenge
import com.regula.documentreader.api.results.DocumentReaderResults

class RfidExample {

    fun exampleImplementationPACertificates(context: Context) {
        DocumentReader.Instance().startRFIDReader(context, object : IRfidReaderCompletion() {
            override fun onCompleted(
                rfidAction: Int,
                results: DocumentReaderResults?,
                ex: DocumentReaderException?
            ) {
                TODO("Not yet implemented")
            }

        }, IRfidReaderRequest(object : IRfidPACertificates {
            override fun onRequestPACertificates(
                serialNumber: ByteArray?,
                issuer: PAResourcesIssuer?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }
        }))
    }

    fun exampleImplementationTACertificates(context: Context) {
        DocumentReader.Instance().startRFIDReader(context, object : IRfidReaderCompletion() {
            override fun onCompleted(
                rfidAction: Int,
                results: DocumentReaderResults?,
                ex: DocumentReaderException?
            ) {
                TODO("Not yet implemented")
            }

        }, IRfidReaderRequest(object : IRfidTACertificates {
            override fun onRequestTACertificates(
                keyCAR: String?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }

        }))
    }

    fun exampleImplementationTASignature(context: Context) {
        DocumentReader.Instance().startRFIDReader(context, object : IRfidReaderCompletion() {
            override fun onCompleted(
                rfidAction: Int,
                results: DocumentReaderResults?,
                ex: DocumentReaderException?
            ) {
                TODO("Not yet implemented")
            }

        }, IRfidReaderRequest(object : IRfidTASignature {
            override fun onRequestTASignature(
                challenge: TAChallenge?,
                completion: IRfidTASignatureCompletion
            ) {
                TODO("Not yet implemented")
            }
        }))
    }

    fun exampleImplementationPAandTA(context: Context) {
        DocumentReader.Instance().startRFIDReader(context, object : IRfidReaderCompletion() {
            override fun onCompleted(
                rfidAction: Int,
                results: DocumentReaderResults?,
                ex: DocumentReaderException?
            ) {
                TODO("Not yet implemented")
            }

        }, IRfidReaderRequest(object : IRfidPACertificates {
            override fun onRequestPACertificates(
                serialNumber: ByteArray?,
                issuer: PAResourcesIssuer?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }

        }, object : IRfidTACertificates {
            override fun onRequestTACertificates(
                keyCAR: String?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }

        }))
    }

    fun exampleImplementationPAandTAandTASignature(context: Context) {
        DocumentReader.Instance().startRFIDReader(context, object : IRfidReaderCompletion() {
            override fun onCompleted(
                rfidAction: Int,
                results: DocumentReaderResults?,
                ex: DocumentReaderException?
            ) {
                TODO("Not yet implemented")
            }

        }, IRfidReaderRequest(object : IRfidPACertificates {
            override fun onRequestPACertificates(
                serialNumber: ByteArray?,
                issuer: PAResourcesIssuer?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }

        }, object : IRfidTACertificates {
            override fun onRequestTACertificates(
                keyCAR: String?,
                completion: IRfidPKDCertificateCompletion
            ) {
                TODO("Not yet implemented")
            }

        }, object : IRfidTASignature {
            override fun onRequestTASignature(
                challenge: TAChallenge?,
                completion: IRfidTASignatureCompletion
            ) {
                TODO("Not yet implemented")
            }

        }))
    }
}