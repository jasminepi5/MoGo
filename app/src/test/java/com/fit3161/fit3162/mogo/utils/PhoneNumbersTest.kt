package com.fit3161.fit3162.mogo.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun australianMobileWithLeadingZero() {
        assertEquals("+61412345678", PhoneNumbers.toE164("+61", "0412345678"))
    }

    @Test
    fun australianMobileWithoutLeadingZero() {
        assertEquals("+61412345678", PhoneNumbers.toE164("+61", "412345678"))
    }

    @Test
    fun italianLandlineKeepsItsLeadingZero() {
        assertEquals("+390612345678", PhoneNumbers.toE164("+39", "0612345678"))
    }

    @Test
    fun usNumber() {
        assertEquals("+12015550123", PhoneNumbers.toE164("+1", "2015550123"))
    }

    @Test
    fun tooShortIsRejected() {
        assertNull(PhoneNumbers.toE164("+61", "12345"))
    }

    @Test
    fun emptyIsRejected() {
        assertNull(PhoneNumbers.toE164("+61", ""))
    }

    @Test
    fun unknownCountryCodeIsRejected() {
        assertNull(PhoneNumbers.toE164("+999", "412345678"))
    }

    @Test
    fun missingCountryCodeIsRejected() {
        assertNull(PhoneNumbers.toE164("", "0412345678"))
    }
}
