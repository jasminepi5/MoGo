package com.fit3161.fit3162.mogo.utils

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumbers {

    private val util = PhoneNumberUtil.getInstance()

    /**
     * Validates a phone number typed for the selected country and returns it in E.164 format
     * (e.g. "+61412345678"), or null if it isn't a valid number for that country.
     *
     * @param countryCode Calling code from the registration dropdown, e.g. "+61".
     * @param nationalNumber Digits as the user typed them, with or without the local trunk prefix (e.g. "0412345678" or "412345678").
     */
    fun toE164(countryCode: String, nationalNumber: String): String? {
        val callingCode = countryCode.removePrefix("+").toIntOrNull() ?: return null
        // The region decides how the national number is read (e.g. a leading 0 is dropped in AU, kept in IT)
        val region = util.getRegionCodeForCountryCode(callingCode)
        if (region == PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY || region == "ZZ") return null

        return try {
            val parsed = util.parse(nationalNumber, region)
            if (parsed.countryCode == callingCode && util.isValidNumber(parsed)) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
        } catch (e: NumberParseException) {
            null
        }
    }
}
