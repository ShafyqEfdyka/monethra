package com.example.monethra.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object EmailRecapHelper {

    fun generateEmailSubject(cycleLabel: String): String {
        return "[Monethra] Financial Recap - $cycleLabel"
    }

    fun generateEmailBody(
        userName: String,
        cycleLabel: String,
        totalIncome: Long,
        totalExpense: Long,
        netSavings: Long,
        budgetUtilization: Int = 0,
        topCategory: String = "",
        insight: String = ""
    ): String {
        val budgetPart = if (budgetUtilization > 0) "\n• Budget Utilization: $budgetUtilization%" else ""
        val categoryPart = if (topCategory.isNotBlank()) "\n• Top Spending Category: $topCategory" else ""
        val detailPart = if (insight.isNotBlank()) "\n\nFinancial Insight:\n$insight" else ""
        return """
            Hi $userName,

            Please find attached your financial recap for $cycleLabel from MONETHRA.

            Summary:
            • Total Income: ${CurrencyFormatter.formatIdr(totalIncome)}
            • Total Expenses: ${CurrencyFormatter.formatIdr(totalExpense)}
            • Net Savings: ${CurrencyFormatter.formatIdr(netSavings)}$budgetPart$categoryPart$detailPart

            Best regards,
            MONETHRA Team
        """.trimIndent()
    }

    fun sendEmailRecap(context: Context, userEmail: String, subject: String, body: String) {
        val mailtoUri = if (userEmail.isNotBlank()) Uri.parse("mailto:$userEmail") else Uri.parse("mailto:")
        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(intent, "Send Email Recap")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback if no email client application is available
        }
    }

    fun sendEmailRecapWithPdf(
        context: Context,
        userEmail: String,
        subject: String,
        body: String,
        pdfFile: File
    ) {
        val pdfUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (userEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export Financial Recap PDF via Email")
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback: send text email if no PDF application handler found
            sendEmailRecap(context, userEmail, subject, body)
        }
    }
}
