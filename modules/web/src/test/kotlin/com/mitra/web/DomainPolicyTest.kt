package com.mitra.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomainPolicyTest {
    private val policy = DomainPolicy()
    private val gov = setOf("*.gov.in")
    private val irctc = setOf("irctc.co.in")

    @Test
    fun `allows https sub-domain of a wildcard`() {
        assertEquals(DomainCheck.Allowed, policy.check("https://scholarships.gov.in/status", gov))
    }

    @Test
    fun `refuses plain http`() {
        assertTrue(policy.check("http://scholarships.gov.in", gov) is DomainCheck.Refused)
    }

    @Test
    fun `refuses a domain that is not on the list`() {
        assertTrue(policy.check("https://example.com", gov) is DomainCheck.Refused)
    }

    @Test
    fun `refuses an allowed name hidden inside another domain`() {
        assertTrue(policy.check("https://irctc.co.in.example.com", irctc) is DomainCheck.Refused)
    }

    @Test
    fun `refuses a one-letter misspelling`() {
        assertTrue(policy.check("https://irctcc.co.in", irctc) is DomainCheck.Refused)
    }

    @Test
    fun `refuses punycode hosts`() {
        assertTrue(policy.check("https://xn--rctc-xqa.co.in", irctc) is DomainCheck.Refused)
    }

    @Test
    fun `refuses when no domains are allowed`() {
        assertTrue(policy.check("https://www.irctc.co.in", emptySet()) is DomainCheck.Refused)
    }

    @Test
    fun `allows exact host and its www sub-domain`() {
        assertEquals(DomainCheck.Allowed, policy.check("https://www.irctc.co.in/nget", irctc))
    }
}
