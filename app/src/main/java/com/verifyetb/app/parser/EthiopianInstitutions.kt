package com.verifyetb.app.parser

import java.util.Locale

enum class InstitutionKind {
    BANK,
    WALLET,
    CASH,
}

data class EthiopianInstitution(
    val id: String,
    val displayName: String,
    val kind: InstitutionKind,
    /** Matched against SMS sender / shortcode (case-insensitive). */
    val senderHints: List<String>,
    /** Matched against SMS body (case-insensitive). */
    val bodyHints: List<String> = emptyList(),
)

/**
 * Catalog of Ethiopian banks and mobile wallets Verifyetb can label from SMS.
 * Matching is hint-based — new shortcodes still parse via the generic money path.
 */
object EthiopianInstitutions {

    val all: List<EthiopianInstitution> = listOf(
        // —— Cash (manual only) ——
        EthiopianInstitution(
            id = "cash",
            displayName = "Cash",
            kind = InstitutionKind.CASH,
            senderHints = emptyList(),
            bodyHints = emptyList(),
        ),
        // —— Mobile wallets ——
        EthiopianInstitution(
            id = "telebirr",
            displayName = "Telebirr",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("TELEBIRR", "127", "800"),
            bodyHints = listOf("telebirr", "ethio telecom"),
        ),
        EthiopianInstitution(
            id = "cbe_birr",
            displayName = "CBE Birr",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("CBEBIRR", "CBE BIRR", "8907"),
            bodyHints = listOf("cbe birr", "cbebirr"),
        ),
        EthiopianInstitution(
            id = "mpesa",
            displayName = "M-Pesa",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("MPESA", "M-PESA", "SAFARICOM"),
            bodyHints = listOf("m-pesa", "mpesa", "safaricom"),
        ),
        EthiopianInstitution(
            id = "kacha",
            displayName = "Kacha",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("KACHA"),
            bodyHints = listOf("kacha"),
        ),
        EthiopianInstitution(
            id = "yaya",
            displayName = "Yaya Wallet",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("YAYA"),
            bodyHints = listOf("yaya wallet", "yaya"),
        ),
        EthiopianInstitution(
            id = "ebirr",
            displayName = "Ebirr",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("EBIRR"),
            bodyHints = listOf("ebirr"),
        ),
        EthiopianInstitution(
            id = "amole",
            displayName = "Amole",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("AMOLE"),
            bodyHints = listOf("amole"),
        ),
        EthiopianInstitution(
            id = "hellocash",
            displayName = "HelloCash",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("HELLOCASH", "HELLO CASH"),
            bodyHints = listOf("hellocash", "hello cash"),
        ),
        EthiopianInstitution(
            id = "mbirr",
            displayName = "M-Birr",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("MBIRR", "M-BIRR"),
            bodyHints = listOf("m-birr", "mbirr"),
        ),
        EthiopianInstitution(
            id = "coopay",
            displayName = "Coopay",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("COOPAY", "COOP-AY"),
            bodyHints = listOf("coopay"),
        ),
        EthiopianInstitution(
            id = "awashbirr",
            displayName = "AwashBIRR",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("AWASHBIRR", "AWASH BIRR"),
            bodyHints = listOf("awashbirr", "awash birr"),
        ),
        EthiopianInstitution(
            id = "owepay",
            displayName = "OwePay",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("OWEPAY"),
            bodyHints = listOf("owepay"),
        ),
        EthiopianInstitution(
            id = "hibir",
            displayName = "HIBIR",
            kind = InstitutionKind.WALLET,
            senderHints = listOf("HIBIR"),
            bodyHints = listOf("hibir wallet", "hibir"),
        ),

        // —— Banks ——
        EthiopianInstitution(
            id = "cbe",
            displayName = "CBE",
            kind = InstitutionKind.BANK,
            senderHints = listOf("CBE", "8906", "COMMERCIAL BANK"),
            bodyHints = listOf("commercial bank of ethiopia", "cbe acct", "cbe account"),
        ),
        EthiopianInstitution(
            id = "awash",
            displayName = "Awash Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("AWASH"),
            bodyHints = listOf("awash bank"),
        ),
        EthiopianInstitution(
            id = "dashen",
            displayName = "Dashen Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("DASHEN"),
            bodyHints = listOf("dashen bank"),
        ),
        EthiopianInstitution(
            id = "boa",
            displayName = "Bank of Abyssinia",
            kind = InstitutionKind.BANK,
            senderHints = listOf("BOA", "ABYSSINIA"),
            bodyHints = listOf("bank of abyssinia", "abyssinia bank"),
        ),
        EthiopianInstitution(
            id = "wegagen",
            displayName = "Wegagen Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("WEGAGEN"),
            bodyHints = listOf("wegagen bank"),
        ),
        EthiopianInstitution(
            id = "united",
            displayName = "United Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("UNITED BANK", "UNITEDBANK"),
            bodyHints = listOf("united bank"),
        ),
        EthiopianInstitution(
            id = "nib",
            displayName = "NIB International Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("NIB", "NIBTERA"),
            bodyHints = listOf("nib international", "nib bank"),
        ),
        EthiopianInstitution(
            id = "coop",
            displayName = "Cooperative Bank of Oromia",
            kind = InstitutionKind.BANK,
            senderHints = listOf("COOP", "COOPERATIVE BANK", "CBO"),
            bodyHints = listOf("cooperative bank of oromia", "coop bank"),
        ),
        EthiopianInstitution(
            id = "lion",
            displayName = "Lion International Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("LION"),
            bodyHints = listOf("lion international bank", "lion bank"),
        ),
        EthiopianInstitution(
            id = "zemen",
            displayName = "Zemen Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("ZEMEN"),
            bodyHints = listOf("zemen bank"),
        ),
        EthiopianInstitution(
            id = "hibret",
            displayName = "Hibret Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("HIBRET", "OIB"),
            bodyHints = listOf("hibret bank", "oromia international bank"),
        ),
        EthiopianInstitution(
            id = "bunna",
            displayName = "Bunna Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("BUNNA", "ABOL"),
            bodyHints = listOf("bunna bank", "bunna international"),
        ),
        EthiopianInstitution(
            id = "abay",
            displayName = "Abay Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("ABAY"),
            bodyHints = listOf("abay bank"),
        ),
        EthiopianInstitution(
            id = "addis",
            displayName = "Addis International Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("ADDIS BANK", "ADDIS INT"),
            bodyHints = listOf("addis international bank"),
        ),
        EthiopianInstitution(
            id = "enat",
            displayName = "Enat Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("ENAT"),
            bodyHints = listOf("enat bank"),
        ),
        EthiopianInstitution(
            id = "debub",
            displayName = "Debub Global Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("DEBUB"),
            bodyHints = listOf("debub global bank", "debub bank"),
        ),
        EthiopianInstitution(
            id = "gadaa",
            displayName = "Gadaa Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("GADAA"),
            bodyHints = listOf("gadaa bank"),
        ),
        EthiopianInstitution(
            id = "hijra",
            displayName = "Hijra Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("HIJRA"),
            bodyHints = listOf("hijra bank"),
        ),
        EthiopianInstitution(
            id = "siinqee",
            displayName = "Siinqee Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("SIINQEE", "SINQEE"),
            bodyHints = listOf("siinqee bank"),
        ),
        EthiopianInstitution(
            id = "tsehay",
            displayName = "Tsehay Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("TSEHAY"),
            bodyHints = listOf("tsehay bank"),
        ),
        EthiopianInstitution(
            id = "amhara",
            displayName = "Amhara Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("AMHARA BANK", "AMHARA"),
            bodyHints = listOf("amhara bank"),
        ),
        EthiopianInstitution(
            id = "ahadu",
            displayName = "Ahadu Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("AHADU"),
            bodyHints = listOf("ahadu bank"),
        ),
        EthiopianInstitution(
            id = "goh",
            displayName = "Goh Betoch Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("GOH"),
            bodyHints = listOf("goh betoch", "goh bank"),
        ),
        EthiopianInstitution(
            id = "shabelle",
            displayName = "Shabelle Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("SHABELLE"),
            bodyHints = listOf("shabelle bank"),
        ),
        EthiopianInstitution(
            id = "oromia",
            displayName = "Oromia Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("OROMIA BANK"),
            bodyHints = listOf("oromia bank"),
        ),
        EthiopianInstitution(
            id = "anbesa",
            displayName = "Anbesa Bank",
            kind = InstitutionKind.BANK,
            senderHints = listOf("ANBESA"),
            bodyHints = listOf("anbesa bank"),
        ),
        EthiopianInstitution(
            id = "global",
            displayName = "Global Bank Ethiopia",
            kind = InstitutionKind.BANK,
            senderHints = listOf("GLOBAL BANK"),
            bodyHints = listOf("global bank ethiopia", "global bank"),
        ),
        EthiopianInstitution(
            id = "dbe",
            displayName = "Development Bank of Ethiopia",
            kind = InstitutionKind.BANK,
            senderHints = listOf("DBE", "DEVELOPMENT BANK"),
            bodyHints = listOf("development bank of ethiopia"),
        ),
    )

    val wallets: List<EthiopianInstitution>
        get() = all.filter { it.kind == InstitutionKind.WALLET }

    val banks: List<EthiopianInstitution>
        get() = all.filter { it.kind == InstitutionKind.BANK }

    val cash: EthiopianInstitution
        get() = all.first { it.id == "cash" }

    /** Banks + wallets shown in pickers (Cash is always included automatically). */
    val pickable: List<EthiopianInstitution>
        get() = banks + wallets

    val selectable: List<EthiopianInstitution>
        get() = listOf(cash) + banks + wallets

    fun withCashAlways(ids: Set<String>): Set<String> = ids + cash.id

    val displayNames: List<String>
        get() = all.map { it.displayName }

    fun byId(id: String): EthiopianInstitution? = all.find { it.id == id }

    fun byDisplayName(name: String): EthiopianInstitution? =
        all.find { it.displayName.equals(name, ignoreCase = true) }

    /**
     * Prefer longer / more specific sender hits, then body hits.
     * CBE Birr is checked before plain CBE via list order + scoring.
     */
    fun match(sender: String, body: String): EthiopianInstitution? {
        val s = sender.uppercase(Locale.US)
        val b = body.lowercase(Locale.US)
        val compactSender = s.replace(Regex("[^A-Z0-9]"), "")
        // A bank debit "to CBE Birr" is still the CBE account. Only the wallet's own SMS belong here.
        if (isCbeBirrWallet(sender, body)) return byId("cbe_birr")
        // Handset shows the sender as MPESA, with no hyphen.
        if (compactSender == "MPESA" || compactSender.startsWith("MPESA")) return byId("mpesa")

        var best: EthiopianInstitution? = null
        var bestScore = 0

        for (inst in all) {
            var score = 0
            for (hint in inst.senderHints) {
                val h = hint.uppercase(Locale.US)
                if (s == h || s.contains(h) || s.replace(" ", "").contains(h.replace(" ", ""))) {
                    score = maxOf(score, 100 + h.length)
                }
            }
            for (hint in inst.bodyHints) {
                // "for CBE Birr" on a bank SMS must not pull that debit onto the wallet.
                if (inst.id == "cbe_birr") continue
                if (b.contains(hint.lowercase(Locale.US))) {
                    score = maxOf(score, 40 + hint.length)
                }
            }
            // Avoid weak "UNITED" / "ABA" false positives unless body also agrees
            if (score in 1..105 && inst.bodyHints.isNotEmpty()) {
                val bodyHit = inst.bodyHints.any { b.contains(it.lowercase(Locale.US)) }
                if (!bodyHit && inst.senderHints.any { it.length <= 4 && s.contains(it.uppercase(Locale.US)) }) {
                    // short ambiguous sender alone — keep only if exact shortcode match
                    val exact = inst.senderHints.any { s == it.uppercase(Locale.US) }
                    if (!exact) score = 0
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = inst
            }
        }
        return if (bestScore > 0) best else null
    }

    /** Wallet SMS, not a CBE bank transfer that merely names CBE Birr. */
    fun isCbeBirrWallet(sender: String, body: String): Boolean {
        val compactSender = sender.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "")
        // The phone shows this sender as CBEBirr — one word, no space.
        if (compactSender.contains("CBEBIRR") || compactSender == "8907") return true
        val b = body.lowercase(Locale.US)
        if (Regex("(?<![a-z0-9])cbebirr(?![a-z0-9])").containsMatchIn(b)) return true
        // Afaan Oromo wallet texts name the account. A bank "transfer to CBE Birr" does not.
        if (b.contains("herreega cbe birr") || b.contains("herrega cbe birr")) return true
        if (b.contains("lakkoofsa nagahee") && b.contains("cbe birr")) return true
        if (b.contains("hafteen") && b.contains("cbe birr")) return true
        return b.contains("cbepay") ||
            b.contains("using cbe birr") ||
            b.contains("cbe birr balance") ||
            b.contains("cbe birr account") ||
            b.contains("your cbe birr")
    }
}
