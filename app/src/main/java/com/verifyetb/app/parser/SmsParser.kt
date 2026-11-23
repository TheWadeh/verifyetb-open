package com.verifyetb.app.parser

data class ParsedSms(
    val type: String,
    val amount: Double,
    val statedBalance: Double?,
    val institution: String,
    val counterparty: String?,
    val reference: String?,
    val reason: String?,
    val feeAmount: Double = 0.0,
)

interface SmsParser {
    fun canParse(sender: String, body: String): Boolean
    fun parse(sender: String, body: String, date: Long): ParsedSms?
}
