package com.app.stash.android.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Material 3 "Google blue" seed palette ---------------------------------
//
// Hand-tuned from a Material Theme Builder export with seed #0B57D0 (the Google
// Maps / Workspace primary blue). When dynamic color isn't available we fall
// back to these. With dynamic color on Android 12+ Material You overrides them
// with the user's wallpaper-derived palette automatically.

// Light scheme
val Primary = Color(0xFF0B57D0)            // Google blue 700
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD3E3FD)
val OnPrimaryContainer = Color(0xFF001A41)

val Secondary = Color(0xFF00639B)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFCFE5FF)
val OnSecondaryContainer = Color(0xFF001D33)

val Tertiary = Color(0xFF7B4FFF)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFE9DDFF)
val OnTertiaryContainer = Color(0xFF22005D)

val Background = Color(0xFFFDFCFF)
val OnBackground = Color(0xFF1A1B20)
val SurfaceL = Color(0xFFFDFCFF)
val OnSurface = Color(0xFF1A1B20)
val SurfaceVariant = Color(0xFFE0E2EC)
val OnSurfaceVariant = Color(0xFF44464F)
val Outline = Color(0xFF74777F)
val OutlineVariant = Color(0xFFC4C6D0)

val ErrorL = Color(0xFFB3261E)
val OnErrorL = Color(0xFFFFFFFF)

// Dark scheme
val PrimaryDark = Color(0xFFA8C7FA)
val OnPrimaryDark = Color(0xFF002F69)
val PrimaryContainerDark = Color(0xFF004494)
val OnPrimaryContainerDark = Color(0xFFD3E3FD)

val SecondaryDark = Color(0xFF95CCFF)
val OnSecondaryDark = Color(0xFF003353)
val SecondaryContainerDark = Color(0xFF004A77)
val OnSecondaryContainerDark = Color(0xFFCFE5FF)

val TertiaryDark = Color(0xFFCFBCFF)
val OnTertiaryDark = Color(0xFF3B1893)
val TertiaryContainerDark = Color(0xFF5535C4)
val OnTertiaryContainerDark = Color(0xFFE9DDFF)

val BackgroundDark = Color(0xFF111318)
val OnBackgroundDark = Color(0xFFE3E2E9)
val SurfaceLDark = Color(0xFF111318)
val OnSurfaceDark = Color(0xFFE3E2E9)
val SurfaceVariantDark = Color(0xFF44464F)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44464F)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

// ---- Legacy aliases (kept so older call-sites still compile) ---------------
// Some places in the UI reference these names directly. New code should use the
// MaterialTheme.colorScheme tokens instead.
val Ink = OnBackground
val InkMuted = OnSurfaceVariant
val Paper = Background
val Surface = SurfaceL
val Accent = Primary
val AccentOn = OnPrimary
val Danger = ErrorL

val InkDark = OnBackgroundDark
val InkMutedDark = OnSurfaceVariantDark
val PaperDark = BackgroundDark
val SurfaceDark = SurfaceLDark
val AccentDark = PrimaryDark
val AccentOnDark = OnPrimaryDark
