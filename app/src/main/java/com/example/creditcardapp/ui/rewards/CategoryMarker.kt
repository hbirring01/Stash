package com.example.creditcardapp.ui.rewards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.creditcardapp.domain.model.RewardCategory

/**
 * Color used to tint markers for each spending category. Kept here so map
 * markers and the list-side category chip can share the palette.
 */
internal fun categoryColor(category: RewardCategory): Int = when (category) {
    RewardCategory.DINING -> 0xFFE53935.toInt()        // red
    RewardCategory.GROCERIES -> 0xFF43A047.toInt()     // green
    RewardCategory.GAS -> 0xFFFB8C00.toInt()           // orange
    RewardCategory.TRAVEL -> 0xFF1E88E5.toInt()        // blue
    RewardCategory.SHOPPING -> 0xFF8E24AA.toInt()      // purple
    RewardCategory.ENTERTAINMENT -> 0xFFD81B60.toInt() // pink
    RewardCategory.OTHER -> 0xFF546E7A.toInt()         // slate
}

/**
 * Returns a 36dp drop-shadowed colored circle drawable suitable for use as an
 * osmdroid Marker icon. We render programmatically to avoid maintaining 7
 * separate vector assets.
 */
internal fun categoryMarkerIcon(context: Context, category: RewardCategory, selected: Boolean = false): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = ((if (selected) 44 else 32) * density).toInt()
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val center = sizePx / 2f
    val outerRadius = center - (2 * density)
    val innerRadius = outerRadius - (3 * density)

    // White outline
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(center, center, outerRadius, outline)

    // Colored fill
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = categoryColor(category) }
    canvas.drawCircle(center, center, innerRadius, fill)

    if (selected) {
        // Inner white ring to mark selection
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
        }
        canvas.drawCircle(center, center, innerRadius - (3 * density), ring)
    }

    return BitmapDrawable(context.resources, bmp)
}

/**
 * Google Maps-style "you are here" blue dot with a white ring. Drawn programmatically
 * so we don't ship a separate asset.
 */
internal fun userLocationIcon(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (22 * density).toInt()
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val center = sizePx / 2f

    // Soft outer halo
    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x331E88E5.toInt()
    }
    canvas.drawCircle(center, center, center, halo)

    // White ring
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(center, center, 7 * density, ring)

    // Blue dot
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E88E5.toInt() }
    canvas.drawCircle(center, center, 5 * density, dot)

    return BitmapDrawable(context.resources, bmp)
}
