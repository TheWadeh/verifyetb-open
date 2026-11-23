package com.verifyetb.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserPipelineTest {

    private val pipeline = SmsParserPipeline(
        cbeBirrParser = CbeBirrParser(),
        cbeParser = CbeParser(),
        telebirrParser = TelebirrParser(),
        ethiopianInstitutionParser = EthiopianInstitutionParser(),
    )

    @Test
    fun parsesCbeDebitWithBalanceAndReason() {
        val body = """
            Dear Customer, Your Acct 1*********1234 has been debited with ETB 1,250.00
            for Lunch. Remaining Balance is ETB 8,450.55. Ref: FT24123ABCDE Thank you.
        """.trimIndent()
        val parsed = pipeline.parse("CBE", body, 1_700_000_000_000L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(1250.0, parsed.amount, 0.001)
        assertEquals(8450.55, parsed.statedBalance!!, 0.001)
        assertEquals("CBE", parsed.institution)
        assertNotNull(parsed.reference)
        assertTrue(parsed.reason!!.contains("Lunch", ignoreCase = true))
    }

    @Test
    fun parsesCbeCredit() {
        val body = "Your account has been credited with ETB 5000.00. Available balance is ETB 12000.00."
        val parsed = pipeline.parse("CBEBirr", body, 2L)
        assertNotNull(parsed)
        assertEquals("CREDIT", parsed!!.type)
        assertEquals(5000.0, parsed.amount, 0.001)
        assertEquals(12000.0, parsed.statedBalance!!, 0.001)
        assertEquals("CBE Birr", parsed.institution)
    }

    @Test
    fun parsesCbeBirrTransferEvenWhenSenderIsCbe() {
        val body = """
            Dear Customer, You have transfered ETB 1,200.00 to ABEL KEBEDE (0911234567).
            Service fee is ETB 1.50. Your current balance is ETB 3,400.00.
            https://cbepay1.cbe.com.et/aureceipt?TID=CGD1QLKIAB&PH=251911234567
        """.trimIndent()
        val parsed = pipeline.parse("CBE", body, 5L)
        assertNotNull(parsed)
        assertEquals("CBE Birr", parsed!!.institution)
        assertEquals("DEBIT", parsed.type)
        assertEquals(1200.0, parsed.amount, 0.001)
        assertEquals(3400.0, parsed.statedBalance!!, 0.001)
        assertEquals("CGD1QLKIAB", parsed.reference)
        assertEquals(1.50, parsed.feeAmount, 0.001)
        assertTrue(parsed.counterparty!!.contains("ABEL"))
    }

    @Test
    fun bankTransferIntoCbeBirrStaysOnTheBank() {
        val body = """
            Dear Customer, Your Acct 1*********1234 has been debited with ETB 50,000.00
            for transfer to CBE Birr. Remaining Balance is ETB 12,000.00. Ref: FT24111BANK1
        """.trimIndent()
        val parsed = pipeline.parse("CBE", body, 6L)
        assertNotNull(parsed)
        assertEquals("CBE", parsed!!.institution)
        assertEquals(50000.0, parsed.amount, 0.001)
        assertEquals(12000.0, parsed.statedBalance!!, 0.001)
    }

    @Test
    fun phoneNumberIsNotTheCbeBirrAmount() {
        val body = """
            You have received 251911223344 ETB 300.00 in your CBE Birr account.
            Your new balance is ETB 900.00.
        """.trimIndent()
        val parsed = pipeline.parse("CBEBirr", body, 7L)
        assertNotNull(parsed)
        assertEquals("CBE Birr", parsed!!.institution)
        assertEquals("CREDIT", parsed.type)
        assertEquals(300.0, parsed.amount, 0.001)
        assertEquals(900.0, parsed.statedBalance!!, 0.001)
    }

    @Test
    fun parsesTelebirrTransfer() {
        val body = """
            You have transferred ETB 200.00 to 0911****** to Airtime.
            Your balance is ETB 1,100.25. Txn ID: TB99887766
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 3L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals(1100.25, parsed.statedBalance!!, 0.001)
        assertEquals("Telebirr", parsed.institution)
        assertEquals("Airtime", parsed.counterparty)
        assertEquals("Airtime", parsed.reason)
    }

    @Test
    fun parsesTelebirrReceived() {
        val body = "You have received ETB 350.00 from Abebe. Balance: ETB 900.00"
        val parsed = pipeline.parse("127", body, 4L)
        assertNotNull(parsed)
        assertEquals("CREDIT", parsed!!.type)
        assertEquals(350.0, parsed.amount, 0.001)
    }

    @Test
    fun matchesAwashAndMpesaFromCatalog() {
        val awash = pipeline.parse(
            "AwashBank",
            "Dear customer, your account has been debited with ETB 400.00. Remaining balance is ETB 2,000.00.",
            10L,
        )
        assertNotNull(awash)
        assertEquals("Awash Bank", awash!!.institution)

        val mpesa = pipeline.parse(
            "M-PESA",
            "You have sent ETB 50.00. Your balance is ETB 120.00.",
            11L,
        )
        assertNotNull(mpesa)
        assertEquals("M-Pesa", mpesa!!.institution)
    }

    @Test
    fun catalogContainsBanksAndWallets() {
        assertTrue(EthiopianInstitutions.banks.size >= 20)
        assertTrue(EthiopianInstitutions.wallets.size >= 10)
    }

    @Test
    fun parsesFeeOnDebitSms() {
        val body = """
            You have transferred ETB 500.00 to Abebe. Transaction fee ETB 2.50.
            Your balance is ETB 1,000.00. Txn ID: TB11223344
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 20L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(500.0, parsed.amount, 0.001)
        assertEquals(2.50, parsed.feeAmount, 0.001)
        assertEquals(1000.0, parsed.statedBalance!!, 0.001)
    }


    @Test
    fun parsesCbeServiceChargeFee() {
        val body = """
            Dear Customer, Your Acct 1*********1234 has been debited with ETB 1,000.00
            for Transfer with Service Charge of ETB 6.00. Remaining Balance is ETB 4,994.00.
            Ref: FT24199FEE01 Thank you.
        """.trimIndent()
        val parsed = pipeline.parse("CBE", body, 21L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(1000.0, parsed.amount, 0.001)
        assertEquals(6.0, parsed.feeAmount, 0.001)
        assertEquals(4994.0, parsed.statedBalance!!, 0.001)
    }

    @Test
    fun parsesTelebirrServiceFeeIs() {
        val body = """
            You have transferred ETB 200.00 to 0911****** .
            Service fee is ETB 1.00. Your balance is ETB 899.00. Txn ID: TB55443322
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 22L)
        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount, 0.001)
        assertEquals(1.0, parsed.feeAmount, 0.001)
    }

    @Test
    fun parsesCommissionFee() {
        val body = """
            Dear customer, your account has been debited with ETB 850.00.
            Commission ETB 5.00. Remaining balance is ETB 2,145.00.
        """.trimIndent()
        val parsed = pipeline.parse("AwashBank", body, 23L)
        assertNotNull(parsed)
        assertEquals(850.0, parsed!!.amount, 0.001)
        assertEquals(5.0, parsed.feeAmount, 0.001)
    }

    @Test
    fun parsesServiceChargePlusVat() {
        val body = """
            Dear Customer, Your Acct has been debited with ETB 2,500.00.
            Commission or Service Charge 10.00 ETB. 15% VAT on Commission 1.50 ETB.
            Remaining Balance is ETB 10,000.00. Ref: FT25030P6FRX
        """.trimIndent()
        val parsed = pipeline.parse("CBE", body, 30L)
        assertNotNull(parsed)
        assertEquals(2500.0, parsed!!.amount, 0.001)
        assertEquals(11.50, parsed.feeAmount, 0.001)
    }

    @Test
    fun parsesSeparateServiceFeeAndVatLines() {
        val body = """
            You have transferred ETB 1,000.00 to Meron Alemu.
            Service charge ETB 6.00 VAT ETB 0.90.
            Your balance is ETB 4,993.10. Txn ID: TB77889900
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 31L)
        assertNotNull(parsed)
        assertEquals(1000.0, parsed!!.amount, 0.001)
        assertEquals(6.90, parsed.feeAmount, 0.001)
        assertEquals("Meron Alemu", parsed.counterparty)
    }

    @Test
    fun parsesCounterpartySentToPhone() {
        val body = """
            You have transferred ETB 200.00 to 0911******.
            Service fee is ETB 1.00. Your balance is ETB 899.00. Txn ID: TB55443322
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 32L)
        assertNotNull(parsed)
        assertEquals("0911******", parsed!!.counterparty)
        assertEquals(1.0, parsed.feeAmount, 0.001)
    }

    @Test
    fun parsesCounterpartyReceivedFromName() {
        val body = "You have received ETB 350.00 from Abebe Kebede. Balance: ETB 900.00"
        val parsed = pipeline.parse("127", body, 33L)
        assertNotNull(parsed)
        assertEquals("Abebe Kebede", parsed!!.counterparty)
        assertEquals(0.0, parsed.feeAmount, 0.001)
    }

    @Test
    fun parsesBeneficiaryLabel() {
        val body = """
            Your account has been debited with ETB 400.00.
            Beneficiary: Sara Hailu. Service Charge of ETB 5.00 VAT: ETB 0.75.
            Remaining balance is ETB 2,000.00.
        """.trimIndent()
        val parsed = pipeline.parse("AwashBank", body, 34L)
        assertNotNull(parsed)
        assertEquals("Sara Hailu", parsed!!.counterparty)
        assertEquals(5.75, parsed.feeAmount, 0.001)
    }

    @Test
    fun airtimeStaysShortWithoutPhoneDump() {
        val body = """
            You have transferred ETB 50.00 to 0911****** to Airtime.
            Service fee is ETB 0.00. Your balance is ETB 200.00. Txn ID: TBAAIR001
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 40L)
        assertNotNull(parsed)
        assertEquals("Airtime", parsed!!.counterparty)
        assertEquals("Airtime", parsed.reason)
        assertEquals(50.0, parsed.amount, 0.001)
    }

    @Test
    fun waivedFeeDoesNotUseBalanceAsFee() {
        val body = """
            You have transferred ETB 100.00 to Abebe. Transaction fee waived.
            Remaining balance is ETB 900.00. Txn ID: TBNOFEE01
        """.trimIndent()
        val parsed = pipeline.parse("Telebirr", body, 50L)
        assertNotNull(parsed)
        assertEquals(0.0, parsed!!.feeAmount, 0.001)
    }

    @Test
    fun creditWithoutFeeStaysZero() {
        val body = "You have received ETB 350.00 from Abebe. Balance: ETB 900.00"
        val parsed = pipeline.parse("127", body, 24L)
        assertNotNull(parsed)
        assertEquals(0.0, parsed!!.feeAmount, 0.001)
    }

    @Test
    fun parsesTelebirrAirtimeAsAirtimeNotDownloadLink() {
        val body = """
            Dear Abebe You have paid ETB 50.00 for airtime. Your transaction number is TB9AIRTIME1.
            Your current balance is ETB 120.00. To download your payment information please click this link:
            https://transactioninfo.ethiotelecom.et/receipt/TB9AIRTIME1 Thank you for using telebirr Ethio telecom
        """.trimIndent()
        val parsed = pipeline.parse("127", body, 30L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(50.0, parsed.amount, 0.001)
        assertEquals("Airtime", parsed.counterparty)
        assertEquals(false, parsed.counterparty!!.contains("download", ignoreCase = true))
    }

    @Test
    fun parsesOromoCbeBirrWithdrawalFromCbeBirrSender() {
        val body = """
            Kabajamoo maamilaa, gaafa 06/11/26 14:57, lakkoofsa nagahee DK691U9D1R1tiin qarshii 200.00Br., 000005 - ATM irraa baastaniirtu. Hafteen herreega CBE Birr keessanii 348.80Br. dha. Galatoomaa!
        """.trimIndent()
        val parsed = pipeline.parse("CBEBirr", body, 60L)
        assertNotNull(parsed)
        assertEquals("CBE Birr", parsed!!.institution)
        assertEquals("DEBIT", parsed.type)
        assertEquals(200.0, parsed.amount, 0.001)
        assertEquals(348.80, parsed.statedBalance!!, 0.001)
        assertEquals("DK691U9D1R1", parsed.reference)
        assertEquals("ATM", parsed.counterparty)
    }

    @Test
    fun parsesOromoCbeBirrCredit() {
        val body = """
            Kabajamoo maamilaa, gaafa 12/11/26 08:51, lakkoofsa nagahee DKC91UYZ5WH tiin qarshiin 5,000.00Br. herreega CBE Birr tti galeera. Hafteen herreega keessanii 5,348.80Br. dha. Galatoomaa!
        """.trimIndent()
        val parsed = pipeline.parse("CBEBirr", body, 61L)
        assertNotNull(parsed)
        assertEquals("CBE Birr", parsed!!.institution)
        assertEquals("CREDIT", parsed.type)
        assertEquals(5000.0, parsed.amount, 0.001)
        assertEquals(5348.80, parsed.statedBalance!!, 0.001)
        assertEquals("DKC91UYZ5WH", parsed.reference)
    }

    @Test
    fun skipsCbeBirrVoucherReminder() {
        val body = """
            200.00Br.. The voucher will expire after 24 Hr at 07/11/26 14:53. Cash out the money from your nearby CBE ATM. Thank you!
        """.trimIndent()
        assertEquals(null, pipeline.parse("CBEBirr", body, 62L))
    }

    @Test
    fun parsesMpesaCashbackFromMpesaSender() {
        val body = "Congratulations, you have received a cashback of 0.10 Birr for Paying via M-PESA"
        val parsed = pipeline.parse("MPESA", body, 70L)
        assertNotNull(parsed)
        assertEquals("M-Pesa", parsed!!.institution)
        assertEquals("CREDIT", parsed.type)
        assertEquals(0.10, parsed.amount, 0.001)
        assertEquals(null, parsed.statedBalance)
        assertEquals("Cashback", parsed.counterparty)
    }

    @Test
    fun parsesMpesaBundleWithoutUsingThePhoneAsAmount() {
        val body = """
            Dear Gatluak, you have bought a Safaricom bundle of 5.00 Birr for 251721604290 on 3/8/26 at 6:42 AM. Transaction number UH37F5141B. Your current M-PESA balance is 5.00 Birr.
        """.trimIndent()
        val parsed = pipeline.parse("MPESA", body, 71L)
        assertNotNull(parsed)
        assertEquals("M-Pesa", parsed!!.institution)
        assertEquals("DEBIT", parsed.type)
        assertEquals(5.0, parsed.amount, 0.001)
        assertEquals(5.0, parsed.statedBalance!!, 0.001)
        assertEquals("UH37F5141B", parsed.reference)
        assertEquals("Safaricom bundle", parsed.counterparty)
    }

    @Test
    fun parsesMpesaBundleThatEmptiesTheWallet() {
        val body = """
            Dear Gatluak, you have bought a Safaricom bundle of 5.00 Birr for 251721604290 on 4/8/26 at 11:48 AM. Transaction number UH40FCDOOW. Your current M-PESA balance is 0.00 Birr.
        """.trimIndent()
        val parsed = pipeline.parse("MPESA", body, 72L)
        assertNotNull(parsed)
        assertEquals("DEBIT", parsed!!.type)
        assertEquals(5.0, parsed.amount, 0.001)
        assertEquals(0.0, parsed.statedBalance!!, 0.001)
        assertEquals("UH40FCDOOW", parsed.reference)
    }

    @Test
    fun skipsMpesaWrongPin() {
        assertEquals(null, pipeline.parse("MPESA", "You have entered the wrong PIN. Please try again.", 73L))
    }

    @Test
    fun ignoresNonFinancialNoise() {
        val parsed = pipeline.parse("Friend", "See you at 5pm near the cafe", 5L)
        assertEquals(null, parsed)
    }
}
