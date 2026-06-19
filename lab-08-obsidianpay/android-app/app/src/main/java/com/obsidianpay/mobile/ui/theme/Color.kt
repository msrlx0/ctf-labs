package com.obsidianpay.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ObsidianPay color system — a premium dark "obsidian" fintech palette.
 *
 * Layered surfaces (background → surface → elevated) instead of pure black, a
 * rich violet primary with a soft lavender secondary, and restrained semantic
 * colors for success / warning / error states.
 */

// Base layers (near-black obsidian, never pure #000000).
val ObsidianBackground = Color(0xFF0B0B14)
val ObsidianSurface = Color(0xFF14141F)
val ObsidianSurfaceElevated = Color(0xFF1C1C2B)
val ObsidianSurfaceHigh = Color(0xFF24243A)
val ObsidianOutline = Color(0xFF2E2E45)

// Brand accents.
val VioletPrimary = Color(0xFF7C5CFF)
val VioletPrimaryDeep = Color(0xFF5B3FE0)
val LavenderSecondary = Color(0xFFB8A6FF)
val LavenderSoft = Color(0xFF8C7BD6)

// Semantic.
val SuccessGreen = Color(0xFF3FB950)
val WarningAmber = Color(0xFFE3A92C)
val ErrorRed = Color(0xFFF0506E)

// Text.
val TextPrimary = Color(0xFFECEAF6)
val TextSecondary = Color(0xFF9A98B8)
val TextOnAccent = Color(0xFF0B0B14)
