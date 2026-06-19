package com.obsidianpay.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * ObsidianPay Material 3 theme. The product is dark-only by design (a premium
 * fintech "obsidian" look), so both light and dark system settings resolve to
 * the same coherent dark scheme.
 */
private val ObsidianColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = VioletPrimaryDeep,
    onPrimaryContainer = TextPrimary,
    secondary = LavenderSecondary,
    onSecondary = TextOnAccent,
    secondaryContainer = ObsidianSurfaceHigh,
    onSecondaryContainer = TextPrimary,
    tertiary = LavenderSoft,
    onTertiary = TextOnAccent,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline,
    error = ErrorRed,
    onError = TextOnAccent,
)

@Composable
fun ObsidianPayTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = ObsidianTypography,
        content = content,
    )
}
