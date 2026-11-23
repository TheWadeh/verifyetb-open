package com.verifyetb.app.activation

/**
 * Lifetime activation pricing & how users reach you.
 * Update these before shipping — they appear on the activation screen.
 */
object ActivationConfig {
    const val PRICE_ETB = 50

    /** Telebirr / wallet number customers pay to. */
    const val PAYMENT_TELEBIRR = "09XXXXXXXX"

    /** Optional CBE / bank account (leave blank to hide). */
    const val PAYMENT_BANK_NAME = "CBE"
    const val PAYMENT_BANK_ACCOUNT = ""

    /** Landing site — submit tempo code + phone + payment receipt. */
    const val ACTIVATE_URL = "https://verifyetb.smarterp.space/activate"

    /** Fallback contacts if they cannot open the site. */
    const val CONTACT_TELEGRAM = "@YourTelegram"
    const val CONTACT_WHATSAPP = "09XXXXXXXX"
    const val CONTACT_SMS = "09XXXXXXXX"
}
