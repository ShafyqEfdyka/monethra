package com.example.monethra.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.ui.theme.DeepIndigo
import com.example.monethra.ui.theme.PastelPeriwinkle
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.utils.DateUtils

@Composable
fun ExportPdfRecapDialog(
    currentMonth: Int,
    currentYear: Int,
    paydayStartDay: Int,
    isExporting: Boolean = false,
    onConfirmExport: (month: Int, year: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val cycleLabel = DateUtils.getPaydayCycleLabel(selectedMonth, selectedYear, paydayStartDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFFF8FAFC),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Recap as PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryText)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select period cycle for the financial recap PDF export:",
                    fontSize = 13.sp,
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Cycle Period: $cycleLabel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    }) {
                        Text("< Prev")
                    }

                    Text(
                        text = "${monthNames[selectedMonth - 1]} $selectedYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryText
                    )

                    TextButton(onClick = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    }) {
                        Text("Next >")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmExport(selectedMonth, selectedYear)
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelPeriwinkle,
                    contentColor = DeepIndigo
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = DeepIndigo,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export & Share PDF",
                        color = DeepIndigo,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
