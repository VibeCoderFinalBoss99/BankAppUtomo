package com.example.bankapputomo.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.R
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object BankSelectionBottomSheet {

    data class BankItem(
        val name: String,
        val fullName: String,
        val shortCode: String,
        val subtitle: String,
        val badgeBgColor: Int,
        val badgeTextColor: Int,
        val isPopular: Boolean = false
    )

    val ALL_BANKS: List<BankItem> = listOf(
        BankItem(
            name = "BCA",
            fullName = "PT Bank Central Asia Tbk",
            shortCode = "BCA",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#E7EFFC"),
            badgeTextColor = Color.parseColor("#005BAC"),
            isPopular = true
        ),
        BankItem(
            name = "Bank Mandiri",
            fullName = "PT Bank Mandiri (Persero) Tbk",
            shortCode = "MDR",
            subtitle = "Bank BUMN • Transfer Instan",
            badgeBgColor = Color.parseColor("#EBF1F8"),
            badgeTextColor = Color.parseColor("#003D79"),
            isPopular = true
        ),
        BankItem(
            name = "BNI",
            fullName = "PT Bank Negara Indonesia (Persero) Tbk",
            shortCode = "BNI",
            subtitle = "Bank BUMN • Transfer Instan",
            badgeBgColor = Color.parseColor("#E6F4F6"),
            badgeTextColor = Color.parseColor("#005E6A"),
            isPopular = true
        ),
        BankItem(
            name = "BRI",
            fullName = "PT Bank Rakyat Indonesia (Persero) Tbk",
            shortCode = "BRI",
            subtitle = "Bank BUMN • Transfer Instan",
            badgeBgColor = Color.parseColor("#E8F0FE"),
            badgeTextColor = Color.parseColor("#00529C"),
            isPopular = true
        ),
        BankItem(
            name = "BSI (Bank Syariah Indonesia)",
            fullName = "PT Bank Syariah Indonesia Tbk",
            shortCode = "BSI",
            subtitle = "Bank Syariah • Bebas Biaya",
            badgeBgColor = Color.parseColor("#E6F7F5"),
            badgeTextColor = Color.parseColor("#00A39D"),
            isPopular = true
        ),
        BankItem(
            name = "Bank Jago",
            fullName = "PT Bank Jago Tbk",
            shortCode = "JAGO",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#FFF8E6"),
            badgeTextColor = Color.parseColor("#D97706"),
            isPopular = true
        ),
        BankItem(
            name = "SeaBank",
            fullName = "PT Bank Seabank Indonesia",
            shortCode = "SEA",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#FFF0EB"),
            badgeTextColor = Color.parseColor("#EA580C"),
            isPopular = true
        ),
        BankItem(
            name = "Bank Permata",
            fullName = "PT Bank Permata Tbk",
            shortCode = "PRM",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#EAF6EE"),
            badgeTextColor = Color.parseColor("#00843D")
        ),
        BankItem(
            name = "CIMB Niaga",
            fullName = "PT Bank CIMB Niaga Tbk",
            shortCode = "CIMB",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FDEBEC"),
            badgeTextColor = Color.parseColor("#ED1B24")
        ),
        BankItem(
            name = "Bank Danamon",
            fullName = "PT Bank Danamon Indonesia Tbk",
            shortCode = "DNM",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FEF2EA"),
            badgeTextColor = Color.parseColor("#E95D0F")
        ),
        BankItem(
            name = "Jenius / BTPN",
            fullName = "PT Bank BTPN Tbk",
            shortCode = "JNS",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#E5F6FD"),
            badgeTextColor = Color.parseColor("#0099DA")
        ),
        BankItem(
            name = "Blu by BCA Digital",
            fullName = "PT Bank Digital BCA",
            shortCode = "BLU",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#E8F1FC"),
            badgeTextColor = Color.parseColor("#1A73E8")
        ),
        BankItem(
            name = "Allo Bank",
            fullName = "PT Allo Bank Indonesia Tbk",
            shortCode = "ALLO",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#F3E8F9"),
            badgeTextColor = Color.parseColor("#8E24AA")
        ),
        BankItem(
            name = "BTN",
            fullName = "PT Bank Tabungan Negara (Persero) Tbk",
            shortCode = "BTN",
            subtitle = "Bank BUMN • Transfer Instan",
            badgeBgColor = Color.parseColor("#EBF2FA"),
            badgeTextColor = Color.parseColor("#0B57D0")
        ),
        BankItem(
            name = "Bank Mega",
            fullName = "PT Bank Mega Tbk",
            shortCode = "MEGA",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FFF3E0"),
            badgeTextColor = Color.parseColor("#E65100")
        ),
        BankItem(
            name = "Panin Bank",
            fullName = "PT Bank Panin Tbk",
            shortCode = "PAN",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#E8EEF5"),
            badgeTextColor = Color.parseColor("#1565C0")
        ),
        BankItem(
            name = "OCBC NISP",
            fullName = "PT Bank OCBC NISP Tbk",
            shortCode = "OCBC",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FDEDED"),
            badgeTextColor = Color.parseColor("#D32F2F")
        ),
        BankItem(
            name = "Bank Sinarmas",
            fullName = "PT Bank Sinarmas Tbk",
            shortCode = "SNM",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FDF2F4"),
            badgeTextColor = Color.parseColor("#C53030")
        ),
        BankItem(
            name = "Maybank Indonesia",
            fullName = "PT Bank Maybank Indonesia Tbk",
            shortCode = "MBK",
            subtitle = "Bank Swasta • Transfer Instan",
            badgeBgColor = Color.parseColor("#FEFCE8"),
            badgeTextColor = Color.parseColor("#CA8A04")
        ),
        BankItem(
            name = "LINE Bank by KEB Hana",
            fullName = "PT Bank KEB Hana Indonesia",
            shortCode = "LINE",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#EBFBF3"),
            badgeTextColor = Color.parseColor("#059669")
        ),
        BankItem(
            name = "Superbank",
            fullName = "PT Super Bank Indonesia",
            shortCode = "SUPR",
            subtitle = "Bank Digital • Bebas Biaya",
            badgeBgColor = Color.parseColor("#F1F5F9"),
            badgeTextColor = Color.parseColor("#334155")
        )
    )

    fun show(
        activity: Activity,
        currentSelected: String,
        onSelected: (String) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_select_bank, null)
        dialog.setContentView(view)

        // Make background transparent for rounded top corners
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        val displayMetrics = activity.resources.displayMetrics
        val density = displayMetrics.density
        val maxHeight = (displayMetrics.heightPixels * 0.85).toInt()
        view.layoutParams = view.layoutParams?.apply {
            height = maxHeight
        } ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseBankPicker)
        val inputSearch = view.findViewById<EditText>(R.id.inputSearchBank)
        val btnClearSearch = view.findViewById<ImageView>(R.id.btnClearBankSearch)
        val layoutPopular = view.findViewById<LinearLayout>(R.id.layoutPopularBanks)
        val containerPopular = view.findViewById<LinearLayout>(R.id.containerPopularBanks)
        val textCountHeader = view.findViewById<TextView>(R.id.textBankCountHeader)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerBanks)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmptyBank)
        val textEmptyDesc = view.findViewById<TextView>(R.id.textEmptyDesc)

        // Close action
        btnClose.bounceClick {
            dialog.dismiss()
        }

        // Setup Popular Chips
        containerPopular.removeAllViews()
        val popularList = ALL_BANKS.filter { it.isPopular }
        popularList.forEach { bank ->
            val isSelected = bank.name.equals(currentSelected, ignoreCase = true)
            val chip = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(if (isSelected) R.drawable.bg_bank_chip_selected else R.drawable.bg_bank_chip)
                val padH = (14 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                val marginLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                layoutParams = marginLp
                isClickable = true
                isFocusable = true
            }

            val chipText = TextView(activity).apply {
                text = bank.name
                typeface = ResourcesCompat.getFont(activity, R.font.poppins_medium)
                textSize = 12f
                setTextColor(
                    ContextCompat.getColor(
                        activity,
                        if (isSelected) R.color.figma_primary else R.color.figma_text_title
                    )
                )
            }
            chip.addView(chipText)

            if (isSelected) {
                val checkIcon = ImageView(activity).apply {
                    val iconSize = (16 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        marginStart = (6 * density).toInt()
                    }
                    setImageResource(R.drawable.ic_figma_check)
                    setColorFilter(ContextCompat.getColor(activity, R.color.figma_primary))
                }
                chip.addView(checkIcon)
            }

            chip.bounceClick {
                onSelected(bank.name)
                dialog.dismiss()
            }

            containerPopular.addView(chip)
        }

        // Setup RecyclerView & Adapter
        val adapter = BankPickerAdapter(
            selectedBank = currentSelected,
            onItemClicked = { chosenBank ->
                onSelected(chosenBank.name)
                dialog.dismiss()
            }
        )
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter
        adapter.submitList(ALL_BANKS)
        textCountHeader.text = "${activity.getString(R.string.all_banks)} (${ALL_BANKS.size})"

        // Filter helper
        fun performFilter(query: String) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                btnClearSearch.visibility = View.GONE
                layoutPopular.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                adapter.submitList(ALL_BANKS)
                textCountHeader.text = "${activity.getString(R.string.all_banks)} (${ALL_BANKS.size})"
            } else {
                btnClearSearch.visibility = View.VISIBLE
                layoutPopular.visibility = View.GONE // Hide chips while actively searching for more space

                val filtered = ALL_BANKS.filter { bank ->
                    bank.name.contains(trimmed, ignoreCase = true) ||
                            bank.fullName.contains(trimmed, ignoreCase = true) ||
                            bank.shortCode.contains(trimmed, ignoreCase = true) ||
                            bank.subtitle.contains(trimmed, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    recycler.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                    textCountHeader.text = "Hasil (0)"
                    textEmptyDesc.text = "Tidak ada bank yang cocok dengan \"$trimmed\""
                } else {
                    recycler.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE
                    textCountHeader.text = "Hasil (${filtered.size})"
                    adapter.submitList(filtered)
                }
            }
        }

        // Clear search button
        btnClearSearch.bounceClick {
            inputSearch.setText("")
            performFilter("")
        }

        // Search text watcher
        inputSearch.addTextChangedListener { text ->
            performFilter(text?.toString() ?: "")
        }

        dialog.show()
    }

    private class BankPickerAdapter(
        private val selectedBank: String,
        private val onItemClicked: (BankItem) -> Unit
    ) : RecyclerView.Adapter<BankPickerAdapter.ViewHolder>() {

        private var items: List<BankItem> = emptyList()

        fun submitList(newItems: List<BankItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bank_picker, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], selectedBank, onItemClicked)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val rootLayout: LinearLayout = itemView.findViewById(R.id.bankItemRoot)
            private val textBadge: TextView = itemView.findViewById(R.id.textBankBadge)
            private val textName: TextView = itemView.findViewById(R.id.textBankName)
            private val textSubtitle: TextView = itemView.findViewById(R.id.textBankSubtitle)
            private val imageCheckSelected: ImageView = itemView.findViewById(R.id.imageCheckSelected)
            private val imageChevron: ImageView = itemView.findViewById(R.id.imageChevron)

            fun bind(
                bank: BankItem,
                selectedBank: String,
                onItemClicked: (BankItem) -> Unit
            ) {
                val isSelected = bank.name.equals(selectedBank, ignoreCase = true)
                val context = itemView.context
                val density = context.resources.displayMetrics.density

                // Badge styling
                val badgeShape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12 * density
                    setColor(bank.badgeBgColor)
                }
                textBadge.background = badgeShape
                textBadge.text = bank.shortCode
                textBadge.setTextColor(bank.badgeTextColor)

                // Text labels
                textName.text = bank.name
                textSubtitle.text = bank.subtitle

                // NO RADIO BUTTON: Modern selection state
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
                    onItemClicked(bank)
                }
            }
        }
    }
}
