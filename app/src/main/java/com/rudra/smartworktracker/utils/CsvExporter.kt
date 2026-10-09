package com.rudra.smartworktracker.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.data.entity.Income
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    suspend fun exportAll(context: Context): File {
        val db = AppDatabase.getDatabase(context)
        val incomes = db.incomeDao().getAllIncomes().first()
        val expenses = db.expenseDao().getAllExpenses().first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "smartworktracker_export_${fileDateFormat.format(Date())}.csv"

        val file = File(context.cacheDir, fileName)
        file.bufferedWriter().use { writer ->
            writer.write("Type,Date,Amount,Category,Description/Notes,Source/Merchant\n")

            incomes.forEach { income ->
                writer.write(
                    "INCOME,${dateFormat.format(Date(income.timestamp))},${income.amount}," +
                    "${csv(income.category)},${csv(income.description)},${csv(income.source)}\n"
                )
            }

            expenses.forEach { expense ->
                writer.write(
                    "EXPENSE,${dateFormat.format(Date(expense.timestamp))},${expense.amount}," +
                    "${csv(expense.category.displayName)},${csv(expense.notes)},${csv(expense.merchant)}\n"
                )
            }
        }

        return file
    }

    /** Quotes a field and doubles embedded quotes so commas/quotes in notes don't break columns. */
    private fun csv(value: String?): String = "\"" + (value ?: "").replace("\"", "\"\"") + "\""

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Data"))
    }
}
