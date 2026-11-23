package com.verifyetb.app.ui.navigation

object Routes {
    const val Onboarding = "onboarding"
    const val PickAccounts = "pick_accounts"
    const val Dashboard = "dashboard"
    const val Transactions =
        "transactions?from={from}&to={to}&institution={institution}&type={type}&feesOnly={feesOnly}"
    const val Detail = "detail/{id}"
    const val AddManual = "add_manual"
    const val Settings = "settings"
    const val Insights = "insights"
    const val Account = "account/{institutionId}"

    fun detail(id: Long) = "detail/$id"

    fun account(institutionId: String) = "account/$institutionId"

    fun transactions(
        from: Long = 0L,
        to: Long = 0L,
        institution: String = "",
        type: String = "",
        feesOnly: Boolean = false,
    ): String {
        val inst = java.net.URLEncoder.encode(institution, Charsets.UTF_8.name())
        val typ = java.net.URLEncoder.encode(type, Charsets.UTF_8.name())
        val fees = if (feesOnly) 1 else 0
        return "transactions?from=$from&to=$to&institution=$inst&type=$typ&feesOnly=$fees"
    }
}
