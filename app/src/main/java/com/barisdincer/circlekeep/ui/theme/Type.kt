package com.barisdincer.circlekeep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barisdincer.circlekeep.R

val PlusJakarta = FontFamily(
  Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

val Typography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
      )
  )
