package com.example.bankapputomo.util

import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.bankapputomo.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavLottieHelper(private val bottomNav: BottomNavigationView) {

    fun setup() {
        val colorStateList = ContextCompat.getColorStateList(
            bottomNav.context,
            R.color.color_bottom_nav_item
        )
        if (colorStateList != null) {
            bottomNav.itemIconTintList = colorStateList
            bottomNav.itemTextColor = colorStateList
        }

        bottomNav.post {
            for (i in 0 until bottomNav.menu.size()) {
                val item = bottomNav.menu.getItem(i)
                if (item.itemId == R.id.bottom_qr) continue

                val itemView = bottomNav.findViewById<View>(item.itemId) ?: continue
                val iconView = itemView.findViewById<ImageView>(
                    com.google.android.material.R.id.navigation_bar_item_icon_view
                ) ?: continue

                iconView.alpha = 1f
                iconView.visibility = View.VISIBLE
                if (colorStateList != null && iconView.imageTintList == null) {
                    iconView.imageTintList = colorStateList
                }
            }
        }
    }

    fun onItemSelected(selectedItemId: Int) {
        if (selectedItemId == R.id.bottom_qr) return

        val itemView = bottomNav.findViewById<View>(selectedItemId) ?: return
        val iconView = itemView.findViewById<ImageView>(
            com.google.android.material.R.id.navigation_bar_item_icon_view
        ) ?: itemView

        iconView.animate()
            .scaleX(1.18f)
            .scaleY(1.18f)
            .setDuration(120)
            .withEndAction {
                iconView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160)
                    .start()
            }
            .start()
    }
}
