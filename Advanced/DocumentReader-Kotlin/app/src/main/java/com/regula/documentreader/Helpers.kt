package com.regula.documentreader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.regula.common.utils.CameraUtil
import com.regula.documentreader.api.DocumentReader.Instance
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eRPRM_ResultType.RPRM_RESULT_TYPE_MRZ_OCR_EXTENDED
import com.regula.documentreader.api.enums.eVisualFieldType
import java.io.FileNotFoundException
import java.io.InputStream

class Helpers {
    companion object {
        const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22

        private val links = mapOf(
            "Documents" to "https://docs.regulaforensics.com/home/faq/machine-readable-travel-documents",
            "Core" to "https://docs.regulaforensics.com/android/core",
            "Scenarios" to "https://docs.regulaforensics.com/android/scenarios",
            "Security" to "https://docs.regulaforensics.com/home/faq/security-mechanisms-for-electronic-documents",
            "Results" to "https://docs.regulaforensics.com/android/results/getting-results"
        )

        val measureSystemIntToString = mapOf(
            0 to "Metric",
            1 to "Imperial"
        )

        val measureSystemStringToInt = mapOf(
            "Metric" to 0,
            "Imperial" to 1
        )

        val captureModeIntToString = mapOf(
            0 to "Auto",
            1 to "Capture video",
            2 to "capture frame"
        )

        val captureModeStringToInt = mapOf(
            "Auto" to 0,
            "Capture video" to 1,
            "Capture frame" to 2
        )

        fun <T> listToString(list: List<T>?, context: Context?): String {
            if (list?.isEmpty() == true)
                return context!!.resources.getString(R.string.string_default)
            return list.toString().removeSurrounding("[", "]")
        }

        fun Array<String>.toIntArray(): IntArray {
            val result = IntArray(size)
            forEachIndexed { index, s -> result[index] = s.toInt() }
            return result
        }

        fun MutableList<Int>.toMutableListString(): MutableList<String> {
            val result = mutableListOf<String>()
            forEach { result.add(it.toString()) }
            return result
        }

        fun String.toIntArray(): IntArray = try {
            if (trim().isEmpty())
                arrayOf<Int>().toIntArray()
            else
                trim().removeSuffix(",").split(",").map { it.trim().toInt() }.toIntArray()
        } catch (e: Exception) {
            throw e
        }

        fun opaqueStatusBar(layout: ViewGroup) {
            val resourceId =
                layout.context.resources.getIdentifier("status_bar_height", "dimen", "android")
            layout.setPadding(0, layout.context.resources.getDimensionPixelSize(resourceId), 0, 0)
        }

        fun Context.themeColor(@AttrRes attrRes: Int): Int {
            val typedValue = TypedValue()
            theme.resolveAttribute(attrRes, typedValue, true)
            return typedValue.data
        }

        fun openLink(context: Context, name: String) {
            val uri: Uri = Uri.parse(links[name])
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }

        fun Context.dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        fun Context.pxToDp(px: Int): Int {
            return (px / resources.displayMetrics.density).toInt()
        }

        fun drawable(id: Int, context: Context): Drawable =
            ResourcesCompat.getDrawable(context.resources, id, context.theme)!!

        fun colorString(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

        fun View.beforeRender(run: () -> Unit) {
            val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    run()
                    return true
                }
            }
            viewTreeObserver.addOnPreDrawListener(preDrawListener)
        }

        fun grayedOutAlpha(enabled: Boolean): Float = if (enabled) 1f else 0.3f

        fun replaceFragment(fragment: Fragment, activity: FragmentActivity, id: Int) =
            activity.supportFragmentManager.beginTransaction().replace(id, fragment).commit()

        fun adaptImageSize(bitmap: Bitmap, newWidth: Int): Bitmap {
            val aspectRatio = bitmap.height.toFloat() / bitmap.width
            val newHeight = (aspectRatio * newWidth).toInt()

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
        }

        fun resetFunctionality() {
            val customization = Instance().customization()
            val processParam = Instance().processParams()
            Instance().resetConfiguration()
            Instance().customization().fromJson(customization.toJsonObject())
            Instance().processParams().fromJson(processParam.toJSONObject())
        }

        fun resetCustomization() {
            val functionality = Instance().functionality()
            val processParam = Instance().processParams()
            Instance().resetConfiguration()
            Instance().functionality().fromJson(functionality.toJsonObject())
            Instance().processParams().fromJson(processParam.toJSONObject())
        }

        fun getBitmap(
            selectedImage: Uri?,
            targetWidth: Int,
            targetHeight: Int,
            context: Context
        ): Bitmap? {
            val resolver = context.contentResolver
            var inputStream: InputStream? = null
            try {
                inputStream = resolver.openInputStream(selectedImage!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)

            try {
                inputStream = resolver.openInputStream(selectedImage!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            options.inSampleSize =
                CameraUtil.calculateInSampleSize(options, targetWidth, targetHeight)
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeStream(inputStream, null, options)
        }

        val VisualFieldType = mapOf(
            "FT_DOCUMENT_CLASS_CODE" to 0,
            "FT_ISSUING_STATE_CODE" to 1,
            "FT_DOCUMENT_NUMBER" to 2,
            "FT_DATE_OF_EXPIRY" to 3,
            "FT_DATE_OF_ISSUE" to 4,
            "FT_DATE_OF_BIRTH" to 5,
            "FT_PLACE_OF_BIRTH" to 6,
            "FT_PERSONAL_NUMBER" to 7,
            "FT_SURNAME" to 8,
            "FT_GIVEN_NAMES" to 9,
            "FT_MOTHERS_NAME" to 10,
            "FT_NATIONALITY" to 11,
            "FT_SEX" to 12,
            "FT_HEIGHT" to 13,
            "FT_WEIGHT" to 14,
            "FT_EYES_COLOR" to 15,
            "FT_HAIR_COLOR" to 16,
            "FT_ADDRESS" to 17,
            "FT_DONOR" to 18,
            "FT_SOCIAL_SECURITY_NUMBER" to 19,
            "FT_DL_CLASS" to 20,
            "FT_DL_ENDORSED" to 21,
            "FT_DL_RESTRICTION_CODE" to 22,
            "FT_DL_UNDER_21_DATE" to 23,
            "FT_AUTHORITY" to 24,
            "FT_SURNAME_AND_GIVEN_NAMES" to 25,
            "FT_NATIONALITY_CODE" to 26,
            "FT_PASSPORT_NUMBER" to 27,
            "FT_INVITATION_NUMBER" to 28,
            "FT_VISA_ID" to 29,
            "FT_VISA_CLASS" to 30,
            "FT_VISA_SUB_CLASS" to 31,
            "FT_MRZ_STRING_1" to 32,
            "FT_MRZ_STRING_2" to 33,
            "FT_MRZ_STRING_3" to 34,
            "FT_MRZ_TYPE" to 35,
            "FT_OPTIONAL_DATA" to 36,
            "FT_DOCUMENT_CLASS_NAME" to 37,
            "FT_ISSUING_STATE_NAME" to 38,
            "FT_PLACE_OF_ISSUE" to 39,
            "FT_DOCUMENT_NUMBER_CHECKSUM" to 40,
            "FT_DATE_OF_BIRTH_CHECKSUM" to 41,
            "FT_DATE_OF_EXPIRY_CHECKSUM" to 42,
            "FT_PERSONAL_NUMBER_CHECKSUM" to 43,
            "FT_FINAL_CHECKSUM" to 44,
            "FT_PASSPORT_NUMBER_CHECKSUM" to 45,
            "FT_INVITATION_NUMBER_CHECKSUM" to 46,
            "FT_VISA_ID_CHECKSUM" to 47,
            "FT_SURNAME_AND_GIVEN_NAMES_CHECKSUM" to 48,
            "FT_VISA_VALID_UNTIL_CHECKSUM" to 49,
            "FT_OTHER" to 50,
            "FT_MRZ_STRINGS" to 51,
            "FT_NAME_SUFFIX" to 52,
            "FT_NAME_PREFIX" to 53,
            "FT_DATE_OF_ISSUE_CHECKSUM" to 54,
            "FT_DATE_OF_ISSUE_CHECK_DIGIT" to 55,
            "FT_DOCUMENT_SERIES" to 56,
            "FT_REG_CERT_REG_NUMBER" to 57,
            "FT_REG_CERT_CAR_MODEL" to 58,
            "FT_REG_CERT_CAR_COLOR" to 59,
            "FT_REG_CERT_BODY_NUMBER" to 60,
            "FT_REG_CERT_CAR_TYPE" to 61,
            "FT_REG_CERT_MAX_WEIGHT" to 62,
            "FT_REG_CERT_WEIGHT" to 63,
            "FT_ADDRESS_AREA" to 64,
            "FT_ADDRESS_STATE" to 65,
            "FT_ADDRESS_BUILDING" to 66,
            "FT_ADDRESS_HOUSE" to 67,
            "FT_ADDRESS_FLAT" to 68,
            "FT_PLACE_OF_REGISTRATION" to 69,
            "FT_DATE_OF_REGISTRATION" to 70,
            "FT_RESIDENT_FROM" to 71,
            "FT_RESIDENT_UNTIL" to 72,
            "FT_AUTHORITY_CODE" to 73,
            "FT_PLACE_OF_BIRTH_AREA" to 74,
            "FT_PLACE_OF_BIRTH_STATE_CODE" to 75,
            "FT_ADDRESS_STREET" to 76,
            "FT_ADDRESS_CITY" to 77,
            "FT_ADDRESS_JURISDICTION_CODE" to 78,
            "FT_ADDRESS_POSTAL_CODE" to 79,
            "FT_DOCUMENT_NUMBER_CHECK_DIGIT" to 80,
            "FT_DATE_OF_BIRTH_CHECK_DIGIT" to 81,
            "FT_DATE_OF_EXPIRY_CHECK_DIGIT" to 82,
            "FT_PERSONAL_NUMBER_CHECK_DIGIT" to 83,
            "FT_FINAL_CHECK_DIGIT" to 84,
            "FT_PASSPORT_NUMBER_CHECK_DIGIT" to 85,
            "FT_INVITATION_NUMBER_CHECK_DIGIT" to 86,
            "FT_VISA_ID_CHECK_DIGIT" to 87,
            "FT_SURNAME_AND_GIVEN_NAMES_CHECK_DIGIT" to 88,
            "FT_VISA_VALID_UNTIL_CHECK_DIGIT" to 89,
            "FT_PERMIT_DL_CLASS" to 90,
            "FT_PERMIT_DATE_OF_EXPIRY" to 91,
            "FT_PERMIT_IDENTIFIER" to 92,
            "FT_PERMIT_DATE_OF_ISSUE" to 93,
            "FT_PERMIT_RESTRICTION_CODE" to 94,
            "FT_PERMIT_ENDORSED" to 95,
            "FT_ISSUE_TIMESTAMP" to 96,
            "FT_NUMBER_OF_DUPLICATES" to 97,
            "FT_MEDICAL_INDICATOR_CODES" to 98,
            "FT_NON_RESIDENT_INDICATOR" to 99,
            "FT_VISA_TYPE" to 100,
            "FT_VISA_VALID_FROM" to 101,
            "FT_VISA_VALID_UNTIL" to 102,
            "FT_DURATION_OF_STAY" to 103,
            "FT_NUMBER_OF_ENTRIES" to 104,
            "FT_DAY" to 105,
            "FT_MONTH" to 106,
            "FT_YEAR" to 107,
            "FT_UNIQUE_CUSTOMER_IDENTIFIER" to 108,
            "FT_COMMERCIAL_VEHICLE_CODES" to 109,
            "FT_AKA_DATE_OF_BIRTH" to 110,
            "FT_AKA_SOCIAL_SECURITY_NUMBER" to 111,
            "FT_AKA_SURNAME" to 112,
            "FT_AKA_GIVEN_NAMES" to 113,
            "FT_AKA_NAME_SUFFIX" to 114,
            "FT_AKA_NAME_PREFIX" to 115,
            "FT_MAILING_ADDRESS_STREET" to 116,
            "FT_MAILING_ADDRESS_CITY" to 117,
            "FT_MAILING_ADDRESS_JURISDICTION_CODE" to 118,
            "FT_MAILING_ADDRESS_POSTAL_CODE" to 119,
            "FT_AUDIT_INFORMATION" to 120,
            "FT_INVENTORY_NUMBER" to 121,
            "FT_RACE_ETHNICITY" to 122,
            "FT_JURISDICTION_VEHICLE_CLASS" to 123,
            "FT_JURISDICTION_ENDORSEMENT_CODE" to 124,
            "FT_JURISDICTION_RESTRICTION_CODE" to 125,
            "FT_FAMILY_NAME" to 126,
            "FT_GIVEN_NAMES_RUS" to 127,
            "FT_VISA_ID_RUS" to 128,
            "FT_FATHERS_NAME" to 129,
            "FT_FATHERS_NAME_RUS" to 130,
            "FT_SURNAME_AND_GIVEN_NAMES_RUS" to 131,
            "FT_PLACE_OF_BIRTH_RUS" to 132,
            "FT_AUTHORITY_RUS" to 133,
            "FT_ISSUING_STATE_CODE_NUMERIC" to 134,
            "FT_NATIONALITY_CODE_NUMERIC" to 135,
            "FT_ENGINE_POWER" to 136,
            "FT_ENGINE_VOLUME" to 137,
            "FT_CHASSIS_NUMBER" to 138,
            "FT_ENGINE_NUMBER" to 139,
            "FT_ENGINE_MODEL" to 140,
            "FT_VEHICLE_CATEGORY" to 141,
            "FT_IDENTITY_CARD_NUMBER" to 142,
            "FT_CONTROL_NO" to 143,
            "FT_PARRENTS_GIVEN_NAMES" to 144,
            "FT_SECOND_SURNAME" to 145,
            "FT_MIDDLE_NAME" to 146,
            "FT_REG_CERT_VIN" to 147,
            "FT_REG_CERT_VIN_CHECK_DIGIT" to 148,
            "FT_REG_CERT_VIN_CHECKSUM" to 149,
            "FT_LINE_1_CHECK_DIGIT" to 150,
            "FT_LINE_2_CHECK_DIGIT" to 151,
            "FT_LINE_3_CHECK_DIGIT" to 152,
            "FT_LINE_1_CHECKSUM" to 153,
            "FT_LINE_2_CHECKSUM" to 154,
            "FT_LINE_3_CHECKSUM" to 155,
            "FT_REG_CERT_REG_NUMBER_CHECK_DIGIT" to 156,
            "FT_REG_CERT_REG_NUMBER_CHECKSUM" to 157,
            "FT_REG_CERT_VEHICLE_ITS_CODE" to 158,
            "FT_CARD_ACCESS_NUMBER" to 159,
            "FT_MARITAL_STATUS" to 160,
            "FT_COMPANY_NAME" to 161,
            "FT_SPECIAL_NOTES" to 162,
            "FT_SURNAME_OF_SPOSE" to 163,
            "FT_TRACKING_NUMBER" to 164,
            "FT_BOOKLET_NUMBER" to 165,
            "FT_CHILDREN" to 166,
            "FT_COPY" to 167,
            "FT_SERIAL_NUMBER" to 168,
            "FT_DOSSIER_NUMBER" to 169,
            "FT_AKA_SURNAME_AND_GIVEN_NAMES" to 170,
            "FT_TERRITORIAL_VALIDITY" to 171,
            "FT_MRZ_STRINGS_WITH_CORRECT_CHECK_SUMS" to 172,
            "FT_DL_CDL_RESTRICTION_CODE" to 173,
            "FT_DL_UNDER_18_DATE" to 174,
            "FT_DL_RECORD_CREATED" to 175,
            "FT_DL_DUPLICATE_DATE" to 176,
            "FT_DL_ISS_TYPE" to 177,
            "FT_MILITARY_BOOK_NUMBER" to 178,
            "FT_DESTINATION" to 179,
            "FT_BLOOD_GROUP" to 180,
            "FT_SEQUENCE_NUMBER" to 181,
            "FT_REG_CERT_BODY_TYPE" to 182,
            "FT_REG_CERT_CAR_MARK" to 183,
            "FT_TRANSACTION_NUMBER" to 184,
            "FT_AGE" to 185,
            "FT_FOLIO_NUMBER" to 186,
            "FT_VOTER_KEY" to 187,
            "FT_ADDRESS_MUNICIPALITY" to 188,
            "FT_ADDRESS_LOCATION" to 189,
            "FT_SECTION" to 190,
            "FT_OCR_NUMBER" to 191,
            "FT_FEDERAL_ELECTIONS" to 192,
            "FT_REFERENCE_NUMBER" to 193,
            "FT_OPTIONAL_DATA_CHECKSUM" to 194,
            "FT_OPTIONAL_DATA_CHECK_DIGIT" to 195,
            "FT_VISA_NUMBER" to 196,
            "FT_VISA_NUMBER_CHECKSUM" to 197,
            "FT_VISA_NUMBER_CHECK_DIGIT" to 198,
            "FT_VOTER" to 199,
            "FT_PREVIOUS_TYPE" to 200,
            "FT_FIELD_FROM_MRZ" to 220,
            "FT_CURRENT_DATE" to 221,
            "FT_STATUS_DATE_OF_EXPIRY" to 251,
            "FT_BANKNOTE_NUMBER" to 252,
            "FT_CSC_CODE" to 253,
            "FT_ARTISTIC_NAME" to 254,
            "FT_ACADEMIC_TITLE" to 255,
            "FT_ADDRESS_COUNTRY" to 256,
            "FT_ADDRESS_ZIPCODE" to 257,
            "FT_E_ID_RESIDENCE_PERMIT_1" to 258,
            "FT_E_ID_RESIDENCE_PERMIT_2" to 259,
            "FT_E_ID_PLACE_OF_BIRTH_STREET" to 260,
            "FT_E_ID_PLACE_OF_BIRTH_CITY" to 261,
            "FT_E_ID_PLACE_OF_BIRTH_STATE" to 262,
            "FT_E_ID_PLACE_OF_BIRTH_COUNTRY" to 263,
            "FT_E_ID_PLACE_OF_BIRTH_ZIPCODE" to 264,
            "FT_CDL_CLASS" to 265,
            "FT_DL_UNDER_19_DATE" to 266,
            "FT_WEIGHT_POUNDS" to 267,
            "FT_LIMITED_DURATION_DOCUMENT_INDICATOR" to 268,
            "FT_ENDORSEMENT_EXPIRATION_DATE" to 269,
            "FT_REVISION_DATE" to 270,
            "FT_COMPLIANCE_TYPE" to 271,
            "FT_FAMILY_NAME_TRUNCATION" to 272,
            "FT_FIRST_NAME_TRUNCATION" to 273,
            "FT_MIDDLE_NAME_TRUNCATION" to 274,
            "FT_EXAM_DATE" to 275,
            "FT_ORGANIZATION" to 276,
            "FT_DEPARTMENT" to 277,
            "FT_PAY_GRADE" to 278,
            "FT_RANK" to 279,
            "FT_BENEFITS_NUMBER" to 280,
            "FT_SPONSOR_SERVICE" to 281,
            "FT_SPONSOR_STATUS" to 282,
            "FT_SPONSOR" to 283,
            "FT_RELATIONSHIP" to 284,
            "FT_USCIS" to 285,
            "FT_CATEGORY" to 286,
            "FT_CONDITIONS" to 287,
            "FT_IDENTIFIER" to 288,
            "FT_CONFIGURATION" to 289,
            "FT_DISCRETIONARY_DATA" to 290,
            "FT_LINE_1_OPTIONAL_DATA" to 291,
            "FT_LINE_2_OPTIONAL_DATA" to 292,
            "FT_LINE_3_OPTIONAL_DATA" to 293,
            "FT_EQV_CODE" to 294,
            "FT_ALT_CODE" to 295,
            "FT_BINARY_CODE" to 296,
            "FT_PSEUDO_CODE" to 297,
            "FT_FEE" to 298,
            "FT_STAMP_NUMBER" to 299,
            "FT_SBH_SECURITYOPTIONS" to 300,
            "FT_SBH_INTEGRITYOPTIONS" to 301,
            "FT_DATE_OF_CREATION" to 302,
            "FT_VALIDITY_PERIOD" to 303,
            "FT_PATRON_HEADER_VERSION" to 304,
            "FT_BDB_TYPE" to 305,
            "FT_BIOMETRIC_TYPE" to 306,
            "FT_BIOMETRIC_SUBTYPE" to 307,
            "FT_BIOMETRIC_PRODUCTID" to 308,
            "FT_BIOMETRIC_FORMAT_OWNER" to 309,
            "FT_BIOMETRIC_FORMAT_TYPE" to 310,
            "FT_PHONE" to 311,
            "FT_PROFESSION" to 312,
            "FT_TITLE" to 313,
            "FT_PERSONAL_SUMMARY" to 314,
            "FT_OTHER_VALID_ID" to 315,
            "FT_CUSTODY_INFO" to 316,
            "FT_OTHER_NAME" to 317,
            "FT_OBSERVATIONS" to 318,
            "FT_TAX" to 319,
            "FT_DATE_OF_PERSONALIZATION" to 320,
            "FT_PERSONALIZATION_SN" to 321,
            "FT_OTHERPERSON_NAME" to 322,
            "FT_PERSONTONOTIFY_DATE_OF_RECORD" to 323,
            "FT_PERSONTONOTIFY_NAME" to 324,
            "FT_PERSONTONOTIFY_PHONE" to 325,
            "FT_PERSONTONOTIFY_ADDRESS" to 326,
            "FT_DS_CERTIFICATE_ISSUER" to 327,
            "FT_DS_CERTIFICATE_SUBJECT" to 328,
            "FT_DS_CERTIFICATE_VALIDFROM" to 329,
            "FT_DS_CERTIFICATE_VALIDTO" to 330,
            "FT_VRC_DATAOBJECT_ENTRY" to 331,
            "FT_TYPE_APPROVAL_NUMBER" to 332,
            "FT_ADMINISTRATIVE_NUMBER" to 333,
            "FT_DOCUMENT_DISCRIMINATOR" to 334,
            "FT_DATA_DISCRIMINATOR" to 335,
            "FT_ISO_ISSUER_ID_NUMBER" to 336,
            "FT_GNIB_NUMBER" to 340,
            "FT_DEPT_NUMBER" to 341,
            "FT_TELEX_CODE" to 342,
            "FT_ALLERGIES" to 343,
            "FT_SP_CODE" to 344,
            "FT_COURT_CODE" to 345,
            "FT_CTY" to 346,
            "FT_SPONSOR_SSN" to 347,
            "FT_DO_D_NUMBER" to 348,
            "FT_MC_NOVICE_DATE" to 349,
            "FT_DUF_NUMBER" to 350,
            "FT_AGY" to 351,
            "FT_PNR_CODE" to 352,
            "FT_FROM_AIRPORT_CODE" to 353,
            "FT_TO_AIRPORT_CODE" to 354,
            "FT_FLIGHT_NUMBER" to 355,
            "FT_DATE_OF_FLIGHT" to 356,
            "FT_SEAT_NUMBER" to 357,
            "FT_DATE_OF_ISSUE_BOARDING_PASS" to 358,
            "FT_CCW_UNTIL" to 359,
            "FT_REFERENCE_NUMBER_CHECKSUM" to 360,
            "FT_REFERENCE_NUMBER_CHECK_DIGIT" to 361,
            "FT_ROOM_NUMBER" to 362,
            "FT_RELIGION" to 363,
            "FT_REMAINDER_TERM" to 364,
            "FT_ELECTRONIC_TICKET_INDICATOR" to 365,
            "FT_COMPARTMENT_CODE" to 366,
            "FT_CHECK_IN_SEQUENCE_NUMBER" to 367,
            "FT_AIRLINE_DESIGNATOR_OF_BOARDING_PASS_ISSUER" to 368,
            "FT_AIRLINE_NUMERIC_CODE" to 369,
            "FT_TICKET_NUMBER" to 370,
            "FT_FREQUENT_FLYER_AIRLINE_DESIGNATOR" to 371,
            "FT_FREQUENT_FLYER_NUMBER" to 372,
            "FT_FREE_BAGGAGE_ALLOWANCE" to 373,
            "FT_PDF_417_CODEC" to 374,
            "FT_IDENTITY_CARD_NUMBER_CHECKSUM" to 375,
            "FT_IDENTITY_CARD_NUMBER_CHECK_DIGIT" to 376,
            "FT_VETERAN" to 377,
            "FT_DL_CLASS_CODE_A_1_FROM" to 378,
            "FT_DL_CLASS_CODE_A_1_TO" to 379,
            "FT_DL_CLASS_CODE_A_1_NOTES" to 380,
            "FT_DL_CLASS_CODE_A_FROM" to 381,
            "FT_DL_CLASS_CODE_A_TO" to 382,
            "FT_DL_CLASS_CODE_A_NOTES" to 383,
            "FT_DL_CLASS_CODE_B_FROM" to 384,
            "FT_DL_CLASS_CODE_B_TO" to 385,
            "FT_DL_CLASS_CODE_B_NOTES" to 386,
            "FT_DL_CLASS_CODE_C_1_FROM" to 387,
            "FT_DL_CLASS_CODE_C_1_TO" to 388,
            "FT_DL_CLASS_CODE_C_1_NOTES" to 389,
            "FT_DL_CLASS_CODE_C_FROM" to 390,
            "FT_DL_CLASS_CODE_C_TO" to 391,
            "FT_DL_CLASS_CODE_C_NOTES" to 392,
            "FT_DL_CLASS_CODE_D_1_FROM" to 393,
            "FT_DL_CLASS_CODE_D_1_TO" to 394,
            "FT_DL_CLASS_CODE_D_1_NOTES" to 395,
            "FT_DL_CLASS_CODE_D_FROM" to 396,
            "FT_DL_CLASS_CODE_D_TO" to 397,
            "FT_DL_CLASS_CODE_D_NOTES" to 398,
            "FT_DL_CLASS_CODE_BE_FROM" to 399,
            "FT_DL_CLASS_CODE_BE_TO" to 400,
            "FT_DL_CLASS_CODE_BE_NOTES" to 401,
            "FT_DL_CLASS_CODE_C_1_E_FROM" to 402,
            "FT_DL_CLASS_CODE_C_1_E_TO" to 403,
            "FT_DL_CLASS_CODE_C_1_E_NOTES" to 404,
            "FT_DL_CLASS_CODE_CE_FROM" to 405,
            "FT_DL_CLASS_CODE_CE_TO" to 406,
            "FT_DL_CLASS_CODE_CE_NOTES" to 407,
            "FT_DL_CLASS_CODE_D_1_E_FROM" to 408,
            "FT_DL_CLASS_CODE_D_1_E_TO" to 409,
            "FT_DL_CLASS_CODE_D_1_E_NOTES" to 410,
            "FT_DL_CLASS_CODE_DE_FROM" to 411,
            "FT_DL_CLASS_CODE_DE_TO" to 412,
            "FT_DL_CLASS_CODE_DE_NOTES" to 413,
            "FT_DL_CLASS_CODE_M_FROM" to 414,
            "FT_DL_CLASS_CODE_M_TO" to 415,
            "FT_DL_CLASS_CODE_M_NOTES" to 416,
            "FT_DL_CLASS_CODE_L_FROM" to 417,
            "FT_DL_CLASS_CODE_L_TO" to 418,
            "FT_DL_CLASS_CODE_L_NOTES" to 419,
            "FT_DL_CLASS_CODE_T_FROM" to 420,
            "FT_DL_CLASS_CODE_T_TO" to 421,
            "FT_DL_CLASS_CODE_T_NOTES" to 422,
            "FT_DL_CLASS_CODE_AM_FROM" to 423,
            "FT_DL_CLASS_CODE_AM_TO" to 424,
            "FT_DL_CLASS_CODE_AM_NOTES" to 425,
            "FT_DL_CLASS_CODE_A_2_FROM" to 426,
            "FT_DL_CLASS_CODE_A_2_TO" to 427,
            "FT_DL_CLASS_CODE_A_2_NOTES" to 428,
            "FT_DL_CLASS_CODE_B_1_FROM" to 429,
            "FT_DL_CLASS_CODE_B_1_TO" to 430,
            "FT_DL_CLASS_CODE_B_1_NOTES" to 431,
            "FT_SURNAME_AT_BIRTH" to 432,
            "FT_CIVIL_STATUS" to 433,
            "FT_NUMBER_OF_SEATS" to 434,
            "FT_NUMBER_OF_STANDING_PLACES" to 435,
            "FT_MAX_SPEED" to 436,
            "FT_FUEL_TYPE" to 437,
            "FT_EC_ENVIRONMENTAL_TYPE" to 438,
            "FT_POWER_WEIGHT_RATIO" to 439,
            "FT_MAX_MASS_OF_TRAILER_BRAKED" to 440,
            "FT_MAX_MASS_OF_TRAILER_UNBRAKED" to 441,
            "FT_TRANSMISSION_TYPE" to 442,
            "FT_TRAILER_HITCH" to 443,
            "FT_ACCOMPANIED_BY" to 444,
            "FT_POLICE_DISTRICT" to 445,
            "FT_FIRST_ISSUE_DATE" to 446,
            "FT_PAYLOAD_CAPACITY" to 447,
            "FT_NUMBER_OF_AXELS" to 448,
            "FT_PERMISSIBLE_AXLE_LOAD" to 449,
            "FT_PRECINCT" to 450,
            "FT_INVITED_BY" to 451,
            "FT_PURPOSE_OF_ENTRY" to 452,
            "FT_SKIN_COLOR" to 453,
            "FT_COMPLEXION" to 454,
            "FT_AIRPORT_FROM" to 455,
            "FT_AIRPORT_TO" to 456,
            "FT_AIRLINE_NAME" to 457,
            "FT_AIRLINE_NAME_FREQUENT_FLYER" to 458,
            "FT_LICENSE_NUMBER" to 459,
            "FT_IN_TANKS" to 460,
            "FT_EXEPT_IN_TANKS" to 461,
            "FT_FAST_TRACK" to 462,
            "FT_OWNER" to 463,
            "FT_MRZ_STRINGS_ICAO_RFID" to 464,
            "FT_NUMBER_OF_CARD_ISSUANCE" to 465,
            "FT_NUMBER_OF_CARD_ISSUANCE_CHECKSUM" to 466,
            "FT_NUMBER_OF_CARD_ISSUANCE_CHECK_DIGIT" to 467,
            "FT_CENTURY_DATE_OF_BIRTH" to 468,
            "FT_DL_CLASSCODE_A3_FROM" to 469,
            "FT_DL_CLASSCODE_A3_TO" to 470,
            "FT_DL_CLASSCODE_A3_NOTES" to 471,
            "FT_DL_CLASSCODE_C2_FROM" to 472,
            "FT_DL_CLASSCODE_C2_TO" to 473,
            "FT_DL_CLASSCODE_C2_NOTES" to 474,
            "FT_DL_CLASSCODE_B2_FROM" to 475,
            "FT_DL_CLASSCODE_B2_TO" to 476,
            "FT_DL_CLASSCODE_B2_NOTES" to 477,
            "FT_DL_CLASSCODE_D2_FROM" to 478,
            "FT_DL_CLASSCODE_D2_TO" to 479,
            "FT_DL_CLASSCODE_D2_NOTES" to 480,
            "FT_DL_CLASSCODE_B2E_FROM" to 481,
            "FT_DL_CLASSCODE_B2E_TO" to 482,
            "FT_DL_CLASSCODE_B2E_NOTES" to 483,
            "FT_DL_CLASSCODE_G_FROM" to 484,
            "FT_DL_CLASSCODE_G_TO" to 485,
            "FT_DL_CLASSCODE_G_NOTES" to 486,
            "FT_DL_CLASSCODE_J_FROM" to 487,
            "FT_DL_CLASSCODE_J_TO" to 488,
            "FT_DL_CLASSCODE_J_NOTES" to 489,
            "FT_DL_CLASSCODE_LC_FROM" to 490,
            "FT_DL_CLASSCODE_LC_TO" to 491,
            "FT_DLC_LASSCODE_LC_NOTES" to 492,
            "FT_BANKCARDNUMBER" to 493,
            "FT_BANKCARDVALIDTHRU" to 494,
            "FT_TAX_NUMBER" to 495,
            "FT_HEALTH_NUMBER" to 496,
            "FT_GRANDFATHERNAME" to 497,
            "FT_SELECTEE_INDICATOR" to 498,
            "FT_MOTHER_SURNAME" to 499,
            "FT_MOTHER_GIVENNAME" to 500,
            "FT_FATHER_SURNAME" to 501,
            "FT_FATHER_GIVENNAME" to 502,
            "FT_MOTHER_DATEOFBIRTH" to 503,
            "FT_FATHER_DATEOFBIRTH" to 504,
            "FT_MOTHER_PERSONALNUMBER" to 505,
            "FT_FATHER_PERSONALNUMBER" to 506,
            "FT_MOTHER_PLACEOFBIRTH" to 507,
            "FT_FATHER_PLACEOFBIRTH" to 508,
            "FT_MOTHER_COUNTRYOFBIRTH" to 509,
            "FT_FATHER_COUNTRYOFBIRTH" to 510,
            "FT_DATE_FIRST_RENEWAL" to 511,
            "FT_DATE_SECOND_RENEWAL" to 512,
            "FT_PLACE_OF_EXAMINATION" to 513,
            "FT_APPLICATION_NUMBER" to 514,
            "FT_VOUCHER_NUMBER" to 515,
            "FT_AUTHORIZATION_NUMBER" to 516,
            "FT_FACULTY" to 517,
            "FT_FORM_OF_EDUCATION" to 518,
            "FT_DNI_NUMBER" to 519,
            "FT_RETIREMENT_NUMBER" to 520,
            "FT_PROFESSIONAL_ID_NUMBER" to 521,
            "FT_AGE_AT_ISSUE" to 522,
            "FT_YEARS_SINCE_ISSUE" to 523,
            "FT_DLCLASSCODE_BTP_FROM" to 524,
            "FT_DLCLASSCODE_BTP_NOTES" to 525,
            "FT_DLCLASSCODE_BTP_TO" to 526,
            "FT_DLCLASSCODE_C3_FROM" to 527,
            "FT_DLCLASSCODE_C3_NOTES" to 528,
            "FT_DLCLASSCODE_C3_TO" to 529,
            "FT_DLCLASSCODE_E_FROM" to 530,
            "FT_DLCLASSCODE_E_NOTES" to 531,
            "FT_DLCLASSCODE_E_TO" to 532,
            "FT_DLCLASSCODE_F_FROM" to 533,
            "FT_DLCLASSCODE_F_NOTES" to 534,
            "FT_DLCLASSCODE_F_TO" to 535,
            "FT_DLCLASSCODE_FA_FROM" to 536,
            "FT_DLCLASSCODE_FA_NOTES" to 537,
            "FT_DLCLASSCODE_FA_TO" to 538,
            "FT_DLCLASSCODE_FA1_FROM" to 539,
            "FT_DLCLASSCODE_FA1_NOTES" to 540,
            "FT_DLCLASSCODE_FA1_TO" to 541,
            "FT_DLCLASSCODE_FB_FROM" to 542,
            "FT_DLCLASSCODE_FB_NOTES" to 543,
            "FT_DLCLASSCODE_FB_TO" to 544,
            "FT_DLCLASSCODE_G1_FROM" to 545,
            "FT_DLCLASSCODE_G1_NOTES" to 546,
            "FT_DLCLASSCODE_G1_TO" to 547,
            "FT_DLCLASSCODE_H_FROM" to 548,
            "FT_DLCLASSCODE_H_NOTES" to 549,
            "FT_DLCLASSCODE_H_TO" to 550,
            "FT_DLCLASSCODE_I_FROM" to 551,
            "FT_DLCLASSCODE_I_NOTES" to 552,
            "FT_DLCLASSCODE_I_TO" to 553,
            "FT_DLCLASSCODE_K_FROM" to 554,
            "FT_DLCLASSCODE_K_NOTES" to 555,
            "FT_DLCLASSCODE_K_TO" to 556,
            "FT_DLCLASSCODE_LK_FROM" to 557,
            "FT_DLCLASSCODE_LK_NOTES" to 558,
            "FT_DLCLASSCODE_LK_TO" to 559,
            "FT_DLCLASSCODE_N_FROM" to 560,
            "FT_DLCLASSCODE_N_NOTES" to 561,
            "FT_DLCLASSCODE_N_TO" to 562,
            "FT_DLCLASSCODE_S_FROM" to 563,
            "FT_DLCLASSCODE_S_NOTES" to 564,
            "FT_DLCLASSCODE_S_TO" to 565,
            "FT_DLCLASSCODE_TB_FROM" to 566,
            "FT_DLCLASSCODE_TB_NOTES" to 567,
            "FT_DLCLASSCODE_TB_TO" to 568,
            "FT_DLCLASSCODE_TM_FROM" to 569,
            "FT_DLCLASSCODE_TM_NOTES" to 570,
            "FT_DLCLASSCODE_TM_TO" to 571,
            "FT_DLCLASSCODE_TR_FROM" to 572,
            "FT_DLCLASSCODE_TR_NOTES" to 573,
            "FT_DLCLASSCODE_TR_TO" to 574,
            "FT_DLCLASSCODE_TV_FROM" to 575,
            "FT_DLCLASSCODE_TV_NOTES" to 576,
            "FT_DLCLASSCODE_TV_TO" to 577,
            "FT_DLCLASSCODE_V_FROM" to 578,
            "FT_DLCLASSCODE_V_NOTES" to 579,
            "FT_DLCLASSCODE_V_TO" to 580,
            "FT_DLCLASSCODE_W_FROM" to 581,
            "FT_DLCLASSCODE_W_NOTES" to 582,
            "FT_DLCLASSCODE_W_TO" to 583,
            "FT_URL" to 584,
            "FT_CALIBER" to 585,
            "FT_MODEL" to 586,
            "FT_MAKE" to 587,
            "FT_NUMBER_OF_CYLINDERS" to 588,
            "FT_SURNAME_OF_HUSBAND_AFTER_REGISTRATION" to 589,
            "FT_SURNAME_OF_WIFE_AFTER_REGISTRATION" to 590,
            "FT_DATE_OF_BIRTH_OF_WIFE" to 591,
            "FT_DATE_OF_BIRTH_OF_HUSBAND" to 592,
            "FT_CITIZENSHIP_OF_FIRST_PERSON" to 593,
            "FT_CITIZENSHIP_OF_SECOND_PERSON" to 594,
            "FT_CVV" to 595,
            "FT_DATE_OF_INSURANCE_EXPIRY" to 596,
            "FT_MORTGAGE_BY" to 597,
            "FT_OLD_DOCUMENT_NUMBER" to 598,
            "FT_OLD_DATE_OF_ISSUE" to 599,
            "FT_OLD_PLACE_OF_ISSUE" to 600,
            "FT_DLCLASSCODE_LR_FROM" to 601,
            "FT_DLCLASSCODE_LR_TO" to 602,
            "FT_DLCLASSCODE_LR_NOTES" to 603,
            "FT_DLCLASSCODE_MR_FROM" to 604,
            "FT_DLCLASSCODE_MR_TO" to 605,
            "FT_DLCLASSCODE_MR_NOTES" to 606,
            "FT_DLCLASSCODE_HR_FROM" to 607,
            "FT_DLCLASSCODE_HR_TO" to 608,
            "FT_DLCLASSCODE_HR_NOTES" to 609,
            "FT_DLCLASSCODE_HC_FROM" to 610,
            "FT_DLCLASSCODE_HC_TO" to 611,
            "FT_DLCLASSCODE_HC_NOTES" to 612,
            "FT_DLCLASSCODE_MC_FROM" to 613,
            "FT_DLCLASSCODE_MC_TO" to 614,
            "FT_DLCLASSCODE_MC_NOTES" to 615,
            "FT_DLCLASSCODE_RE_FROM" to 616,
            "FT_DLCLASSCODE_RE_TO" to 617,
            "FT_DLCLASSCODE_RE_NOTES" to 618,
            "FT_DLCLASSCODE_R_FROM" to 619,
            "FT_DLCLASSCODE_R_TO" to 620,
            "FT_DLCLASSCODE_R_NOTES" to 621,
            "FT_DLCLASSCODE_CA_FROM" to 622,
            "FT_DLCLASSCODE_CA_TO" to 623,
            "FT_DLCLASSCODE_CA_NOTES" to 624,
        )

        val LCID = mapOf(
            "LATIN" to 0,
            "AFRIKAANS" to 1078,
            "ALBANIAN" to 1052,
            "ARABIC_ALGERIA" to 5121,
            "ARABIC_BAHRAIN" to 15361,
            "ARABIC_EGYPT" to 3073,
            "ARABIC_IRAQ" to 2049,
            "ARABIC_JORDAN" to 11265,
            "ARABIC_KUWAIT" to 13313,
            "ARABIC_LEBANON" to 12289,
            "ARABIC_LIBYA" to 4097,
            "ARABIC_MOROCCO" to 6145,
            "ARABIC_OMAN" to 8193,
            "ARABIC_QATAR" to 16385,
            "ARABIC_SAUDI_ARABIA" to 1025,
            "ARABIC_SYRIA" to 10241,
            "ARABIC_TUNISIA" to 7169,
            "ARABIC_UAE" to 14337,
            "ARABIC_YEMEN" to 9217,
            "ARABIC_ARMENIAN" to 1067,
            "AZERI_CYRILIC" to 2092,
            "AZERI_LATIN" to 1068,
            "BASQUE" to 1069,
            "BELARUSIAN" to 1059,
            "BULGARIAN" to 1026,
            "CATALAN" to 1027,
            "CHINESE_HONGKONG_SAR" to 3076,
            "CHINESE_MACAO_SAR" to 5124,
            "CHINESE" to 2052,
            "CHINESE_SINGAPORE" to 4100,
            "CHINESE_TAIWAN" to 1028,
            "CROATIAN" to 1050,
            "CZECH" to 1029,
            "DANISH" to 1030,
            "DIVEHI" to 1125,
            "DUTCH_BELGIUM" to 2067,
            "DUTCH_NETHERLANDS" to 1043,
            "ENGLISH_AUSTRALIA" to 3081,
            "ENGLISH_BELIZE" to 10249,
            "ENGLISH_CANADA" to 4105,
            "ENGLISH_CARRIBEAN" to 9225,
            "ENGLISH_IRELAND" to 6153,
            "ENGLISH_JAMAICA" to 8201,
            "ENGLISH_NEW_ZEALAND" to 5129,
            "ENGLISH_PHILIPPINES" to 13321,
            "ENGLISH_SOUTH_AFRICA" to 7177,
            "ENGLISH_TRINIDAD" to 11273,
            "ENGLISH_UK" to 2057,
            "ENGLISH_US" to 1033,
            "ENGLISH_ZIMBABWE" to 12297,
            "ESTONIAN" to 1061,
            "FAEROESE" to 1080,
            "FARSI" to 1065,
            "FINNISH" to 1035,
            "FRENCH_BELGIUM" to 2060,
            "FRENCH_CANADA" to 3084,
            "FRENCH_FRANCE" to 1036,
            "FRENCH_LUXEMBOURG" to 5132,
            "FRENCH_MONACO" to 6156,
            "FRENCH_SWITZERLAND" to 4108,
            "FYRO_MACEDONIAN" to 1071,
            "GALICIAN" to 1110,
            "GEORGIAN" to 1079,
            "GERMAN_AUSTRIA" to 3079,
            "GERMAN_GERMANY" to 1031,
            "GERMAN_LIECHTENSTEIN" to 5127,
            "GERMAN_LUXEMBOURG" to 4103,
            "GERMAN_SWITZERLAND" to 2055,
            "GREEK" to 1032,
            "GUJARATI" to 1095,
            "HEBREW" to 1037,
            "HINDI_INDIA" to 1081,
            "HUNGARIAN" to 1038,
            "ICELANDIC" to 1039,
            "INDONESIAN" to 1057,
            "ITALIAN_ITALY" to 1040,
            "ITALIAN_SWITZERLAND" to 2064,
            "JAPANESE" to 1041,
            "KANNADA" to 1099,
            "KAZAKH" to 1087,
            "KONKANI" to 1111,
            "KOREAN" to 1042,
            "KYRGYZ_CYRILICK" to 1088,
            "LATVIAN" to 1062,
            "LITHUANIAN" to 1063,
            "MALAY_MALAYSIA" to 1086,
            "MALAY_BRUNEI_DARUSSALAM" to 2110,
            "MARATHI" to 1102,
            "MONGOLIAN_CYRILIC" to 1104,
            "NORWEGIAN_BOKMAL" to 1044,
            "NORWEGIAN_NYORSK" to 2068,
            "POLISH" to 1045,
            "PORTUGUESE_BRAZIL" to 1046,
            "PORTUGUESE_PORTUGAL" to 2070,
            "PUNJABI" to 1094,
            "RHAETO_ROMANIC" to 1047,
            "ROMANIAN" to 1048,
            "RUSSIAN" to 1049,
            "SANSKRIT" to 1103,
            "SERBIAN_CYRILIC" to 3098,
            "SERBIAN_LATIN" to 2074,
            "SLOVAK" to 1051,
            "SLOVENIAN" to 1060,
            "SPANISH_ARGENTINA" to 11274,
            "SPANISH_BOLIVIA" to 16394,
            "SPANISH_CHILE" to 13322,
            "SPANICH_COLOMBIA" to 9226,
            "SPANISH_COSTA_RICA" to 5130,
            "SPANISH_DOMINICAN_REPUBLIC" to 7178,
            "SPANISH_ECUADOR" to 12298,
            "SPANISH_EL_SALVADOR" to 17418,
            "SPANISH_GUATEMALA" to 4106,
            "SPANISH_HONDURAS" to 18442,
            "SPANISH_MEXICO" to 2058,
            "SPANISH_NICARAGUA" to 19466,
            "SPANISH_PANAMA" to 6154,
            "SPANISH_PARAGUAY" to 15370,
            "SPANISH_PERU" to 10250,
            "SPANISH_PUERTO_RICO" to 20490,
            "SPANISH_TRADITIONAL_SORT" to 1034,
            "SPANISH_INTERNATIONAL_SORT" to 3082,
            "SPANISH_URUGUAY" to 14346,
            "SPANISH_VENEZUELA" to 8202,
            "SWAHILI" to 1089,
            "SWEDISH" to 1053,
            "SWEDISH_FINLAND" to 2077,
            "SYRIAC" to 1114,
            "TAMIL" to 1097,
            "TATAR" to 1092,
            "TELUGU" to 1098,
            "THAI_THAILAND" to 1054,
            "TURKISH" to 1055,
            "TAJIK_CYRILLIC" to 1064,
            "TURKMEN" to 1090,
            "UKRAINIAN" to 1058,
            "URDU" to 1056,
            "UZBEK_CYRILIC" to 2115,
            "UZBEK_LATIN" to 1091,
            "VIETNAMESE" to 1066,
            "CTC_SIMPLIFIED" to 50001,
            "CTC_TRADITIONAL" to 50002,
        )

        val ResultType = mapOf(
            "NONE" to -1,
            "RPRM_RESULT_TYPE_EMPTY" to 0,
            "RPRM_RESULT_TYPE_RAW_IMAGE" to 1,
            "RPRM_RESULT_TYPE_FILE_IMAGE" to 2,
            "RPRM_RESULT_TYPE_MRZ_OCR_EXTENDED" to 3,
            "RPRM_RESULT_TYPE_BARCODES" to 5,
            "RPRM_RESULT_TYPE_GRAPHICS" to 6,
            "RPRM_RESULT_TYPE_MRZ_TEST_QUALITY" to 7,
            "RPRM_RESULT_TYPE_DOCUMENT_TYPES_CANDIDATES" to 8,
            "RPRM_RESULT_TYPE_CHOSEN_DOCUMENT_TYPE_CANDIDATE" to 9,
            "RPRM_RESULT_TYPE_DOCUMENTS_INFO_LIST" to 10,
            "RPRM_RESULT_TYPE_OCR_LEXICAL_ANALYZE" to 15,
            "RPRM_RESULT_TYPE_RAW_UNCROPPED_IMAGE" to 16,
            "RPRM_RESULT_TYPE_VISUAL_OCR_EXTENDED" to 17,
            "RPRM_RESULT_TYPE_BAR_CODES_TEXT_DATA" to 18,
            "RPRM_RESULT_TYPE_BAR_CODES_IMAGE_DATA" to 19,
            "RPRM_RESULT_TYPE_EOS_IMAGE" to 23,
            "RPRM_RESULT_TYPE_BAYER_IMAGE" to 24,
            "RPRM_RESULT_TYPE_MAGNETIC_STRIPE" to 25,
            "RPRM_RESULT_TYPE_MAGNETIC_STRIPE_TEXT_DATA" to 26,
            "RPRM_RESULT_TYPE_FIELD_FILE_IMAGE" to 27,
            "RPRM_RESULT_TYPE_DATABASE_CHECK" to 28,
            "RPRM_RESULT_TYPE_FINGERPRINT_TEMPLATE_ISO" to 29,
            "RPRM_RESULT_TYPE_INPUT_IMAGE_QUALITY" to 30,
            "RPRM_RESULT_TYPE_HOLO_PARAMS" to 47,
            "RPRM_RESULT_TYPE_DOCUMENT_POSITION" to 85,
            "RPRM_RESULT_TYPE_CUSTOM" to 100,
            "RFID_RESULT_TYPE_RFID_RAW_DATA" to 101,
            "RFID_RESULT_TYPE_RFID_TEXT_DATA" to 102,
            "RFID_RESULT_TYPE_RFID_IMAGE_DATA" to 103,
            "RFID_RESULT_TYPE_RFID_BINARY_DATA" to 104,
            "RFID_RESULT_TYPE_RFID_ORIGINAL_GRAPHICS" to 105,
            "RPRM_RESULT_TYPE_BARCODE_POSITION" to 62,
            "RPRM_RESULT_TYPE_MRZ_POSITION" to 61,
        )

        fun keyByValue(map: Map<String, Int>, value: Int): String {
            val set = map.filterValues { it == value }.keys
            if (set.isEmpty())
                return "n/a"
            return set.first()
        }

        fun keyByValue(mapIndex: Int, value: Int): String = keyByValue(
            when (mapIndex) {
                ParameterField.fieldType -> VisualFieldType
                ParameterField.lcid -> LCID
                ParameterField.sourceType -> ResultType
                ParameterField.original -> mapOf("false" to 0, "true" to 1)
                else -> emptyMap()
            }, value
        )

        fun getResultTypeTranslation(value: Int) = when (value) {
            eRPRM_ResultType.NONE -> "None"
            eRPRM_ResultType.RPRM_RESULT_TYPE_EMPTY -> "Empty"
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE -> "Raw Image"
            eRPRM_ResultType.RPRM_RESULT_TYPE_FILE_IMAGE -> "File Image"
            RPRM_RESULT_TYPE_MRZ_OCR_EXTENDED -> "Mrz OCR Extended"
            eRPRM_ResultType.RPRM_RESULT_TYPE_BARCODES -> "Barcodes"
            eRPRM_ResultType.RPRM_RESULT_TYPE_GRAPHICS -> "Graphics"
            eRPRM_ResultType.RPRM_RESULT_TYPE_MRZ_TEST_QUALITY -> "Mrz Test Quality"
            eRPRM_ResultType.RPRM_RESULT_TYPE_DOCUMENT_TYPES_CANDIDATES -> "Document Types Candidates"
            eRPRM_ResultType.RPRM_RESULT_TYPE_CHOSEN_DOCUMENT_TYPE_CANDIDATE -> "Choosen Document Type Candidate"
            eRPRM_ResultType.RPRM_RESULT_TYPE_DOCUMENTS_INFO_LIST -> "Documents Info List"
            eRPRM_ResultType.RPRM_RESULT_TYPE_OCR_LEXICAL_ANALYZE -> "OCR Lexical Analyze"
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_UNCROPPED_IMAGE -> "Raw Uncropped Image"
            eRPRM_ResultType.RPRM_RESULT_TYPE_VISUAL_OCR_EXTENDED -> "Visual OCR Extended"
            eRPRM_ResultType.RPRM_RESULT_TYPE_BAR_CODES_TEXT_DATA -> "Barcodes Text Data"
            eRPRM_ResultType.RPRM_RESULT_TYPE_BAR_CODES_IMAGE_DATA -> "Barcodes Image Data"
            eRPRM_ResultType.RPRM_RESULT_TYPE_EOS_IMAGE -> "EOS Image"
            eRPRM_ResultType.RPRM_RESULT_TYPE_BAYER_IMAGE -> "Bayer Image"
            eRPRM_ResultType.RPRM_RESULT_TYPE_MAGNETIC_STRIPE -> "Magnetic Stripe"
            eRPRM_ResultType.RPRM_RESULT_TYPE_MAGNETIC_STRIPE_TEXT_DATA -> "Magnetic Stripe Text Data"
            eRPRM_ResultType.RPRM_RESULT_TYPE_FIELD_FILE_IMAGE -> "Filed File Image"
            eRPRM_ResultType.RPRM_RESULT_TYPE_DATABASE_CHECK -> "Database Check"
            eRPRM_ResultType.RPRM_RESULT_TYPE_FINGERPRINT_TEMPLATE_ISO -> "Fingerprint Template"
            eRPRM_ResultType.RPRM_RESULT_TYPE_INPUT_IMAGE_QUALITY -> "Input Image Quality"
            eRPRM_ResultType.RPRM_RESULT_TYPE_HOLO_PARAMS -> "Holo Params"
            eRPRM_ResultType.RPRM_RESULT_TYPE_DOCUMENT_POSITION -> "Document Position"
            eRPRM_ResultType.RPRM_RESULT_TYPE_CUSTOM -> "Custom"
            eRPRM_ResultType.RFID_RESULT_TYPE_RFID_RAW_DATA -> "RFID Raw Data"
            eRPRM_ResultType.RFID_RESULT_TYPE_RFID_TEXT_DATA -> "RFID Text Data"
            eRPRM_ResultType.RFID_RESULT_TYPE_RFID_IMAGE_DATA -> "RFID Image Data"
            eRPRM_ResultType.RFID_RESULT_TYPE_RFID_BINARY_DATA -> "RFID Binary Data"
            eRPRM_ResultType.RFID_RESULT_TYPE_RFID_ORIGINAL_GRAPHICS -> "RFID Original Graphics"
            eRPRM_ResultType.RPRM_RESULT_TYPE_BARCODE_POSITION -> "Barcode Position"
            eRPRM_ResultType.RPRM_RESULT_TYPE_MRZ_POSITION -> "Mrz Position"
            else -> "Undefined"
        }

        fun getTranslation(mapIndex: Int, value: String, context: Context): String {
            val map = when (mapIndex) {
                ParameterField.fieldType -> VisualFieldType
                ParameterField.lcid -> LCID
                ParameterField.sourceType -> ResultType
                ParameterField.original -> mapOf("false" to 0, "true" to 1)
                else -> emptyMap()
            }
            return when (mapIndex) {
                ParameterField.fieldType -> eVisualFieldType.getTranslation(context, map[value]!!)
                ParameterField.lcid -> com.regula.documentreader.api.enums.LCID.getTranslation(
                    context,
                    map[value]!!
                )
                ParameterField.sourceType -> getResultTypeTranslation(map[value]!!)
                ParameterField.original -> value
                else -> ""
            }
        }
    }
}
