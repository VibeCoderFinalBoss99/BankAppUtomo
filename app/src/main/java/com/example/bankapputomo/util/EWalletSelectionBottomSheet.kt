package com.example.bankapputomo.util

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object EWalletSelectionBottomSheet {

    data class EWalletItem(
        val name: String,
        val subtitle: String,
        val logoRes: Int
    )

    val ALL_EWALLETS: List<EWalletItem> = listOf(
        EWalletItem(
            name = "GoPay",
            subtitle = "GoPay Indonesia • Transfer Instan",
            logoRes = R.drawable.ic_ewallet_gopay
        ),
        EWalletItem(
            name = "DANA",
            subtitle = "DANA Indonesia • Transfer Instan",
            logoRes = R.drawable.ic_ewallet_dana
        ),
        EWalletItem(
            name = "OVO",
            subtitle = "OVO Indonesia • Transfer Instan",
            logoRes = R.drawable.ic_ewallet_ovo
        ),
        EWalletItem(
            name = "ShopeePay",
            subtitle = "ShopeePay Indonesia • Bebas Biaya",
            logoRes = R.drawable.ic_ewallet_shopeepay
        ),
        EWalletItem(
            name = "LinkAja",
            subtitle = "LinkAja Indonesia • Transfer Instan",
            logoRes = R.drawable.ic_ewallet_linkaja
        )
    )

    fun show(
        activity: Activity,
        currentSelected: String,
        onSelected: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_select_ewallet, null)
        dialog.setContentView(view)

        // Make background transparent for rounded top corners
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseEWalletPicker)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerEWallets)

        btnClose.bounceClick {
            dialog.dismiss()
        }

        val adapter = EWalletPickerAdapter(
            items = ALL_EWALLETS,
            selectedName = currentSelected,
            onItemClicked = { chosen ->
                onSelected(chosen.name)
                dialog.dismiss()
            }
        )

        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter

        dialog.show()
    }

    private class EWalletPickerAdapter(
        private val items: List<EWalletItem>,
        private val selectedName: String,
        private val onItemClicked: (EWalletItem) -> Unit
    ) : RecyclerView.Adapter<EWalletPickerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ewallet_picker, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], selectedName, onItemClicked)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val rootLayout: LinearLayout = itemView.findViewById(R.id.ewalletItemRoot)
            private val imageLogo: ImageView = itemView.findViewById(R.id.imageEWalletLogo)
            private val textName: TextView = itemView.findViewById(R.id.textEWalletName)
            private val textSubtitle: TextView = itemView.findViewById(R.id.textEWalletSubtitle)
            private val imageCheckSelected: ImageView = itemView.findViewById(R.id.imageEWalletCheckSelected)
            private val imageChevron: ImageView = itemView.findViewById(R.id.imageEWalletChevron)

            fun bind(
                item: EWalletItem,
                selectedName: String,
                onItemClicked: (EWalletItem) -> Unit
            ) {
                val isSelected = item.name.equals(selectedName, ignoreCase = true)
                val context = itemView.context

                imageLogo.setImageResource(item.logoRes)
                textName.text = item.name
                textSubtitle.text = item.subtitle

                if (isSelected) {
                    rootLayout.setBackgroundResource(R.drawable.bg_bank_item_selected)
                    imageCheckSelected.visibility = View.VISIBLE
                    imageChevron.visibility = View.GONE
                    textName.setTextColor(ContextCompat.getColor(context, R.color.figma_primary))
                } else {
                    rootLayout.setBackgroundResource(R.drawable.bg_bank_item_normal)
                    imageCheckSelected.visibility = View.GONE
                    imageChevron.visibility = View.VISIBLE
                    textName.setTextColor(ContextCompat.getColor(context, R.color.figma_text_title))
                }

                itemView.bounceClick {
                    onItemClicked(item)
                }
            }
        }
    }
}
