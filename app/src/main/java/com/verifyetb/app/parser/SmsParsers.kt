package com.verifyetb.app.parser

import com.verifyetb.app.data.local.entity.TransactionEntity
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

internal object MoneyParse {
    private val numberPattern =
        """(?<![0-9])([0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]{1,2})?|[0-9]{1,8}(?:\.[0-9]{1,2})?)(?![0-9])"""
    private val number = Regex(numberPattern)
    private val money = Regex(
        """(?i)(?:ETB|Br\.?|Birr)\s*""" + numberPattern +
            """|""" + numberPattern + """\s*(?:ETB|Br\.?|Birr)"""
    )

    fun toDouble(raw: String): Double =
        raw.replace(",", "").toDoubleOrNull() ?: 0.0

    fun extractMoney(text: String): Double? {
        for (match in money.findAll(text)) {
            val raw = number.find(match.value)?.value ?: continue
            if (isPhoneLike(raw)) continue
            return toDouble(raw)
        }
        val raw = number.find(text)?.value ?: return null
        if (isPhoneLike(raw)) return null
        return toDouble(raw)
    }

    /** 09… / 2519… handset numbers must not become a transfer amount. */
    private fun isPhoneLike(raw: String): Boolean {
        if (raw.contains('.') || raw.contains(',')) return false
        return raw.length >= 10
    }

    fun firstAmountAfter(keywords: List<String>, body: String): Double? {
        val lower = body.lowercase(Locale.US)
        for (key in keywords) {
            val idx = lower.indexOf(key.lowercase(Locale.US))
            if (idx >= 0) {
                extractMoney(body.substring(idx))?.let { return it }
            }
        }
        return null
    }

    fun transactionAmount(body: String): Double? {
        val ordered = firstAmountAfter(
            listOf(
                "debited with", "credited with", "debited by", "credited by",
                "withdrawn", "you have received", "has been received",
                "you have paid", "have paid",
                "you have transfered", "transfered etb", "transfered birr",
                "transferred etb", "transferred birr", "paid etb", "paid birr",
                "transactional amount",
                "cashback of", "bundle of", "qarshii",
                "amount of", "amount:", "amount ",
            ),
            body,
        )
        if (ordered != null) return ordered

        // Prefer first money token that is NOT the balance line.
        val lower = body.lowercase(Locale.US)
        val balanceIdx = balancePhrases
            .map { lower.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull() ?: Int.MAX_VALUE

        for (match in money.findAll(body)) {
            if (match.range.first < balanceIdx) {
                return extractMoney(match.value)
            }
        }
        return extractMoney(body)
    }

    private val balancePhrases = listOf(
        "remaining balance",
        "available balance",
        "current m-pesa balance",
        "current mpesa balance",
        "m-pesa balance",
        "mpesa balance",
        "current balance",
        "new balance",
        "updated balance",
        "total balance",
        "wallet balance",
        "cbe birr balance",
        "hafteen",
        "your balance",
        "balance is",
        "balance:",
        "bal.",
        "bal ",
    )

    fun balance(body: String): Double? = firstAmountAfter(balancePhrases, body)

    /**
     * Total fees on an SMS: service charge + VAT + commission + transaction fee.
     * Ethiopian banks often list service charge and VAT as separate lines — sum them.
     */
    fun fee(body: String, transactionAmount: Double? = null): Double {
        val amountGroup =
            """(?<![0-9])([0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]{1,2})?|[0-9]{1,8}(?:\.[0-9]{1,2})?)(?![0-9])"""
        // Each pattern targets one fee line so service + VAT both count.
        val patterns = listOf(
            // "15% VAT on Commission 1.50 ETB" / "VAT on commission: ETB 1.5"
            Regex(
                """(?i)(?:\d+\s*%\s*)?vat(?:\s+on\s+(?:commission|service(?:\s+charge)?|fee))?\b""" +
                    """(?:\s*(?:of|is|:|-))?\s*(?:ETB|Br\.?|Birr)?\s*""" + amountGroup +
                    """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "VAT ETB 1.50" / "VAT: 0.90 Birr"
            Regex(
                """(?i)\bvat\b\s*[:\-]?\s*(?:ETB|Br\.?|Birr)?\s*""" + amountGroup +
                    """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "Commission or Service Charge 10.00 ETB"
            Regex(
                """(?i)commission\s+or\s+service\s+charge\b(?:\s*(?:of|is|:|-))?\s*(?:ETB|Br\.?|Birr)?\s*""" +
                    amountGroup + """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "Service Charge of ETB 6.00" / "service fee is Br. 1.00"
            Regex(
                """(?i)\bservice\s+(?:charge|fee)\b(?:\s*(?:of|is|:|-))?\s*(?:ETB|Br\.?|Birr)?\s*""" +
                    amountGroup + """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "with a service charge of ETB 2.00"
            Regex(
                """(?i)(?:with\s+)?(?:a\s+)?service\s+charge\s+(?:of\s+)?(?:ETB|Br\.?|Birr)?\s*""" +
                    amountGroup,
            ),
            // "Transaction fee ETB 2.50"
            Regex(
                """(?i)\btransaction\s+fee\b(?:\s*(?:of|is|:|-))?\s*(?:ETB|Br\.?|Birr)?\s*""" +
                    amountGroup + """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "Commission ETB 5.00" / "Commission: 5 Birr"
            Regex(
                """(?i)\bcommission\b(?:\s*(?:of|is|:|-|amount))?\s*(?:ETB|Br\.?|Birr)?\s*""" +
                    amountGroup + """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "SC: ETB 10" / "SC ETB 10.00" (service charge shorthand)
            Regex(
                """(?i)\bsc\b\s*[:\-]?\s*(?:ETB|Br\.?|Birr)?\s*""" + amountGroup +
                    """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // "fee charged 2.00 Birr" / "charged ETB 1.50" near fee context handled below
            Regex(
                """(?i)(?:fee\s+)?charged\s+(?:ETB|Br\.?|Birr)?\s*""" + amountGroup +
                    """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
            // Trailing currency forms
            Regex(
                """(?i)\b(?:service\s+charge|service\s+fee|transaction\s+fee|commission|vat)\b""" +
                    """\s*[:\-]?\s*""" + amountGroup + """\s*(?:ETB|Br\.?|Birr)""",
            ),
            // Bare "Fee ETB 2.50" last so it doesn't steal service/VAT lines
            Regex(
                """(?i)\bfee\b\s*[:\-]?\s*(?:ETB|Br\.?|Birr)?\s*""" + amountGroup +
                    """(?:\s*(?:ETB|Br\.?|Birr))?""",
            ),
        )

        data class Hit(val start: Int, val end: Int, val value: Double)

        val hits = mutableListOf<Hit>()
        for (pattern in patterns) {
            for (match in pattern.findAll(body)) {
                val raw = match.groupValues.lastOrNull { it.isNotBlank() && it.any(Char::isDigit) }
                    ?: continue
                val value = toDouble(raw)
                if (value <= 0.0 || value >= 100_000.0) continue
                // Avoid treating the transfer principal as a fee.
                if (transactionAmount != null && value == transactionAmount) continue
                val start = match.range.first
                val end = match.range.last
                val matchedText = match.value.lowercase(Locale.US)
                val prefix = body.substring(0, start).lowercase(Locale.US)
                // "Service charge ETB 6.90 inclusive of VAT ETB 0.90" — don't add VAT twice.
                if (matchedText.contains("vat") &&
                    (prefix.contains("inclusive of") || prefix.contains("including vat") ||
                        prefix.takeLast(40).contains("inclusive"))
                ) {
                    continue
                }
                // Skip if this range is already covered (e.g. "VAT" matched twice).
                if (hits.any { existing -> start <= existing.end && end >= existing.start }) continue
                hits += Hit(start, end, value)
            }
        }

        if (hits.isNotEmpty()) {
            return hits.sumOf { it.value }
        }

        // No loose keyword fallback — it was picking balance amounts after "fee waived".
        return 0.0
    }

    fun findReference(body: String): String? {
        Regex("""(?i)lakkoofsa\s+nagahee\s+([A-Za-z0-9]{6,})""").find(body)?.let { match ->
            val token = match.groupValues[1].replace(Regex("(?i)tiin$"), "")
            if (token.length >= 6) return token
        }
        val patterns = listOf(
            Regex("""(?i)[?&]TID=([A-Z0-9]{6,})"""),
            Regex("""(?i)\btransaction\s+number\s+([A-Z0-9]{6,})"""),
            Regex("""(?i)\bOrder\s+ID\s*[:\-]?\s*([A-Z0-9]{6,})"""),
            Regex("""(?i)(?:ref(?:erence)?|txn|transaction|trace)\s*(?:no\.?|id|#|number)?\s*[:\-]?\s*([A-Z0-9\-]{6,})"""),
            Regex("""(?i)\bFT[A-Z0-9]{6,}\b"""),
            Regex("""(?i)\bCBE[A-Z0-9]{6,}\b"""),
            Regex("""(?i)\bID[:\-]?\s*([A-Z0-9\-]{6,})"""),
        )
        for (p in patterns) {
            p.find(body)?.let { m ->
                return m.groupValues.last { it.isNotBlank() }
            }
        }
        return null
    }

    fun findReason(body: String): String? {
        if (isAirtime(body)) return "Airtime"
        val patterns = listOf(
            Regex("""(?i)(?:reason|purpose|remark|narration)\s*[:\-]\s*(.+)"""),
            Regex("""(?i)payment for\s+(.+)"""),
            Regex("""(?i)for\s+([A-Za-z][A-Za-z0-9 .,'\-]{2,40})(?:\.|$)"""),
        )
        for (p in patterns) {
            p.find(body)?.groupValues?.getOrNull(1)?.trim()?.let {
                var cleaned = it.split('\n').first().trim().trimEnd('.')
                cleaned = cleaned.replace(
                    Regex(
                        """(?i)\s+(?:with\s+)?(?:service|transaction|commission|vat|fee|remaining|available|your\s+balance).*$""",
                    ),
                    "",
                ).trim()
                val banned = listOf(
                    "etb", "balance", "account", "dear", "thank", "transfer", "service",
                    "fee", "vat", "commission", "download",
                )
                if (cleaned.length in 2..80 &&
                    banned.none { b -> cleaned.lowercase(Locale.US).startsWith(b) }
                ) {
                    return cleaned
                }
            }
        }
        return null
    }

    /** True for airtime / top-up / recharge SMS. */
    fun isAirtime(body: String): Boolean {
        val lower = body.lowercase(Locale.US)
        return listOf(
            "airtime",
            "air time",
            "top-up",
            "top up",
            "topup",
            "recharge",
            "bundle purchase",
            "data bundle",
        ).any { lower.contains(it) }
    }

    /**
     * Who money was sent to / received from, when the SMS includes it.
     * Airtime purchases stay short — no phone/account dump as the title.
     */
    fun findCounterparty(body: String): String? {
        detectKnownLabel(body)?.let { return it }
        if (isAirtime(body)) return "Airtime"

        val banned = setOf(
            "etb", "birr", "br", "your", "the", "account", "acct", "balance", "remaining",
            "available", "dear", "customer", "thank", "thanks", "service", "charge", "fee",
            "vat", "commission", "transfer", "transferred", "airtime", "payment", "txn",
            "transaction", "ref", "reference", "from", "with", "for", "and", "you", "have",
            "been", "debited", "credited", "received", "sent", "paid", "wallet", "bank",
        )

        fun clean(raw: String): String? {
            var cleaned = raw
                .split('\n', '.', ',', ';', '|')
                .first()
                .trim()
                .trimEnd('.')
                .replace(Regex("""\s+"""), " ")
            // Drop trailing junk like "Your balance is…"
            cleaned = cleaned.replace(
                Regex(
                    """(?i)\s+(?:your|remaining|available|current)\s+balance.*$""",
                ),
                "",
            ).trim()
            // "0911****** to Airtime" → keep phone only
            cleaned = cleaned.replace(Regex("""(?i)\s+to\s+.*$"""), "").trim()
            cleaned = cleaned.replace(Regex("""(?i)^(to|from)\s+"""), "").trim()
            if (cleaned.length !in 2..50) return null
            val lower = cleaned.lowercase(Locale.US)
            if (banned.any { lower == it || lower.startsWith("$it ") }) return null
            if (lower.matches(Regex("""^(etb|birr|br\.?)\s*[0-9].*"""))) return null
            if (isJunkLabel(cleaned)) return null
            return cleaned
        }

        val patterns = listOf(
            // Labeled fields: Beneficiary: Abebe Kebede
            Regex(
                """(?i)(?:beneficiary|receiver|recipient|sender|payee|payer|counterparty)\s*[:\-]\s*""" +
                    """([A-Za-z][A-Za-z0-9 .'*+\-]{1,48})""",
            ),
            // "transferred ETB 200.00 to Abebe" / "sent ETB 50 to 0911******"
            Regex(
                """(?i)(?:transferred|transfered|sent|paid)\s+(?:ETB|Br\.?|Birr)\s*[0-9,]+\.?[0-9]*\s+to\s+""" +
                    """([A-Za-z0-9*+\-][A-Za-z0-9*+\- .']{1,40})""",
            ),
            // "you have transferred to NAME"
            Regex(
                """(?i)(?:transferred|transfered|sent|paid)\s+to\s+""" +
                    """([A-Za-z0-9*+\-][A-Za-z0-9*+\- .']{1,40})""",
            ),
            // "received ETB 350.00 from Abebe"
            Regex(
                """(?i)(?:received|credited)\s+(?:ETB|Br\.?|Birr)\s*[0-9,]+\.?[0-9]*\s+from\s+""" +
                    """([A-Za-z0-9*+\-][A-Za-z0-9*+\- .']{1,40})""",
            ),
            // "received from Abebe" / "credited from NAME"
            Regex(
                """(?i)(?:received|credited)\s+from\s+""" +
                    """([A-Za-z0-9*+\-][A-Za-z0-9*+\- .']{1,40})""",
            ),
            // "from Abebe Kebede" / "to 0911223344"
            Regex(
                """(?i)\b(?:to|from)\s+""" +
                    """((?:[A-Za-z][A-Za-z0-9 .'*+\-]{1,40})|(?:[0-9*]{6,15}))""",
            ),
            // Masked / full account numbers as last resort
            Regex("""(?i)(?:acct|account)\s*(?:no\.?|#)?\s*[:\-]?\s*([0-9*]{4,})"""),
        )

        for (p in patterns) {
            for (match in p.findAll(body)) {
                match.groupValues.getOrNull(1)?.let { clean(it) }?.let { return it }
            }
        }
        return null
    }

    /** Known bill / product labels from Telebirr and similar SMS. */
    fun detectKnownLabel(body: String): String? {
        val lower = body.lowercase(Locale.US)
        return when {
            "cashback" in lower -> "Cashback"
            "bundle" in lower -> "Safaricom bundle"
            "airtime" in lower || "air time" in lower -> "Airtime"
            "atm irraa" in lower || "cbe atm" in lower -> "ATM"
            else -> null
        }
    }

    /** Voucher reminders and failed attempts are not movements of money. */
    fun isNonTransaction(body: String): Boolean {
        val lower = body.lowercase(Locale.US)
        val notice = listOf(
            "voucher",
            "wrong pin",
            "entered the wrong",
            "hin danda",
            "dogongoraa",
            "please try again",
        ).any { lower.contains(it) }
        if (!notice) return false
        return balancePhrases.none { lower.contains(it) }
    }

    fun isJunkLabel(value: String): Boolean {
        val lower = value.lowercase(Locale.US).trim()
        if (lower.isEmpty()) return true
        val bannedContains = listOf(
            "download",
            "payment information",
            "click this",
            "please click",
            "transaction number",
            "current balance",
            "your balance",
            "ethio telecom",
            "thank you",
        )
        if (bannedContains.any { lower.contains(it) }) return true
        val bannedStarts = listOf("your ", "the ", "this ", "use ", "pay ", "to ", "for ")
        return bannedStarts.any { lower.startsWith(it) }
    }

    fun listTitle(counterparty: String?, reason: String?, institution: String): String {
        listOfNotNull(counterparty, reason).forEach { field ->
            detectKnownLabel(field)?.let { return it }
        }
        counterparty?.takeIf { it.isNotBlank() && !isJunkLabel(it) }?.let { return it }
        reason?.takeIf { it.isNotBlank() && !isJunkLabel(it) }?.let { label ->
            return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
        return institution
    }

    fun fingerprint(sender: String, body: String, date: Long): String {
        var h = 1125899906842597L
        val key = "$sender|$date|$body"
        for (ch in key) {
            h = (h * 31) + ch.code
        }
        return h.toULong().toString(16)
    }

    fun detectType(body: String): String? {
        val lower = body.lowercase(Locale.US)
        val debitHits = listOf(
            "debited", "withdrawn", "withdrawal", "purchase", "paid to", "paid for",
            "you have paid", "have paid", "paid etb", "paid birr",
            "transfer to", "transferred to", "you have transferred", "you have transfered",
            "transfered to", "transfered etb", "transfered birr",
            "transferred etb", "transferred birr", "sent to", "you have sent", "you sent",
            "cashed out", "cash out", "cash-out",
            "payment of", "bought", "purchased",
            "baastaniirtu", "baastanii", "baaftan", "baafattan", "atm irraa",
        ).count { lower.contains(it) }
        val creditHits = listOf(
            "credited", "deposited", "deposit", "received from", "you have received",
            "has been received", "transfer from", "transferred from", "transfered from",
            "sent you", "salary", "cash in", "cashed in", "cashback",
            "galeera", "galfame", "tti gale",
        ).count { lower.contains(it) }
        return when {
            debitHits > creditHits -> TransactionEntity.TYPE_DEBIT
            creditHits > debitHits -> TransactionEntity.TYPE_CREDIT
            debitHits > 0 -> TransactionEntity.TYPE_DEBIT
            creditHits > 0 -> TransactionEntity.TYPE_CREDIT
            else -> null
        }
    }
}

class CbeBirrParser @Inject constructor() : SmsParser {
    override fun canParse(sender: String, body: String): Boolean =
        EthiopianInstitutions.match(sender, body)?.id == "cbe_birr"

    override fun parse(sender: String, body: String, date: Long): ParsedSms? {
        val type = MoneyParse.detectType(body) ?: return null
        val amount = MoneyParse.transactionAmount(body) ?: return null
        val fingerprint = MoneyParse.fingerprint(sender, body, date)
        return ParsedSms(
            type = type,
            amount = amount,
            statedBalance = MoneyParse.balance(body),
            institution = "CBE Birr",
            counterparty = MoneyParse.findCounterparty(body),
            reference = MoneyParse.findReference(body) ?: "cbebirr-$fingerprint",
            reason = MoneyParse.findReason(body),
            feeAmount = MoneyParse.fee(body, amount),
        )
    }
}

class CbeParser @Inject constructor() : SmsParser {
    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase(Locale.US)
        val b = body.uppercase(Locale.US)
        return s.contains("CBE") || s == "8906" || s.contains("COMMERCIAL") ||
            b.contains("CBE BIRR") || b.contains("COMMERCIAL BANK OF ETHIOPIA") ||
            (b.contains("CBE") && (b.contains("DEBIT") || b.contains("CREDIT") || b.contains("ETB")))
    }

    override fun parse(sender: String, body: String, date: Long): ParsedSms? {
        val type = MoneyParse.detectType(body) ?: return null
        val amount = MoneyParse.transactionAmount(body) ?: return null
        val matched = EthiopianInstitutions.match(sender, body)
        val institution = when {
            matched?.id == "cbe_birr" -> "CBE Birr"
            else -> "CBE"
        }
        return ParsedSms(
            type = type,
            amount = amount,
            statedBalance = MoneyParse.balance(body),
            institution = institution,
            counterparty = MoneyParse.findCounterparty(body),
            reference = MoneyParse.findReference(body)
                ?: "${matched?.id ?: "cbe"}-${MoneyParse.fingerprint(sender, body, date)}",
            reason = MoneyParse.findReason(body),
            feeAmount = MoneyParse.fee(body, amount),
        )
    }
}

class TelebirrParser @Inject constructor() : SmsParser {
    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase(Locale.US).replace(" ", "")
        val b = body.uppercase(Locale.US)
        return s.contains("TELEBIRR") || s == "127" || s == "800" ||
            s.endsWith("127") || b.contains("TELEBIRR") || b.contains("ETHIO TELECOM")
    }

    override fun parse(sender: String, body: String, date: Long): ParsedSms? {
        val type = MoneyParse.detectType(body) ?: return null
        val amount = MoneyParse.transactionAmount(body) ?: return null
        return ParsedSms(
            type = type,
            amount = amount,
            statedBalance = MoneyParse.balance(body),
            institution = "Telebirr",
            counterparty = MoneyParse.findCounterparty(body),
            reference = MoneyParse.findReference(body)
                ?: "telebirr-${MoneyParse.fingerprint(sender, body, date)}",
            reason = MoneyParse.findReason(body),
            feeAmount = MoneyParse.fee(body, amount),
        )
    }
}

class GenericParser @Inject constructor() : SmsParser {
    override fun canParse(sender: String, body: String): Boolean {
        val lower = body.lowercase(Locale.US)
        val hasMoney = lower.contains("etb") || lower.contains("birr") || lower.contains("br.")
        val hasTxn = MoneyParse.detectType(body) != null
        return hasMoney && hasTxn
    }

    override fun parse(sender: String, body: String, date: Long): ParsedSms? {
        val type = MoneyParse.detectType(body) ?: return null
        val amount = MoneyParse.transactionAmount(body) ?: return null
        val matched = EthiopianInstitutions.match(sender, body)
        val institution = matched?.displayName
            ?: sender.takeIf { it.isNotBlank() }
            ?: "Bank SMS"
        val prefix = matched?.id ?: "sms"
        return ParsedSms(
            type = type,
            amount = amount,
            statedBalance = MoneyParse.balance(body),
            institution = institution,
            counterparty = MoneyParse.findCounterparty(body),
            reference = MoneyParse.findReference(body)
                ?: "$prefix-${MoneyParse.fingerprint(sender, body, date)}",
            reason = MoneyParse.findReason(body),
            feeAmount = MoneyParse.fee(body, amount),
        )
    }
}

/**
 * Matches any catalogued Ethiopian bank/wallet SMS that CBE/Telebirr parsers did not claim.
 */
class EthiopianInstitutionParser @Inject constructor() : SmsParser {
    override fun canParse(sender: String, body: String): Boolean {
        return EthiopianInstitutions.match(sender, body) != null &&
            MoneyParse.detectType(body) != null
    }

    override fun parse(sender: String, body: String, date: Long): ParsedSms? {
        val institution = EthiopianInstitutions.match(sender, body) ?: return null
        val type = MoneyParse.detectType(body) ?: return null
        val amount = MoneyParse.transactionAmount(body) ?: return null
        return ParsedSms(
            type = type,
            amount = amount,
            statedBalance = MoneyParse.balance(body),
            institution = institution.displayName,
            counterparty = MoneyParse.findCounterparty(body),
            reference = MoneyParse.findReference(body)
                ?: "${institution.id}-${MoneyParse.fingerprint(sender, body, date)}",
            reason = MoneyParse.findReason(body),
            feeAmount = MoneyParse.fee(body, amount),
        )
    }
}

@Singleton
class SmsParserPipeline @Inject constructor(
    cbeBirrParser: CbeBirrParser,
    cbeParser: CbeParser,
    telebirrParser: TelebirrParser,
    ethiopianInstitutionParser: EthiopianInstitutionParser,
) {
    private val parsers: List<SmsParser> = listOf(
        cbeBirrParser,
        cbeParser,
        telebirrParser,
        ethiopianInstitutionParser,
        GenericParser(),
    )

    fun parse(sender: String, body: String, date: Long): ParsedSms? {
        if (MoneyParse.isNonTransaction(body)) return null
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                parser.parse(sender, body, date)?.let { return it }
            }
        }
        return null
    }
}
