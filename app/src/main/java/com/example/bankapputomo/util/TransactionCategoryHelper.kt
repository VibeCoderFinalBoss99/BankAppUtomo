package com.example.bankapputomo.util

import com.example.bankapputomo.R

data class CategoryMeta(
    val id: String,
    val nameId: String,
    val nameEn: String,
    val iconRes: Int,
    val bgRes: Int,
    val colorRes: Int
)

object TransactionCategoryHelper {

    val CATEGORIES = listOf(
        CategoryMeta("ALL", "Semua", "All", R.drawable.ic_receipt, R.drawable.bg_tile_soft_blue, R.color.figma_primary),
        CategoryMeta("TRANSFER", "Transfer", "Transfer", R.drawable.ic_figma_sent, R.drawable.bg_tile_soft_blue, R.color.figma_primary),
        CategoryMeta("RECEIVE", "Terima Dana", "Money Received", R.drawable.ic_figma_received, R.drawable.bg_tile_soft_green, R.color.bank_success),
        CategoryMeta("TOPUP", "Isi Saldo", "Top Up", R.drawable.ic_figma_topup, R.drawable.bg_tile_soft_purple, R.color.bank_purple),
        CategoryMeta("QRIS", "QRIS", "QRIS Payment", R.drawable.ic_qr, R.drawable.bg_tile_soft_blue, R.color.figma_primary),
        CategoryMeta("PLN", "Listrik PLN", "Electricity (PLN)", R.drawable.ic_bulb, R.drawable.bg_tile_soft_orange, R.color.bank_orange),
        CategoryMeta("PDAM", "Air PDAM", "Water (PDAM)", R.drawable.ic_drop, R.drawable.bg_tile_soft_cyan, R.color.bank_blue),
        CategoryMeta("INTERNET", "Internet & TV", "Internet & TV", R.drawable.ic_wifi, R.drawable.bg_tile_soft_blue, R.color.figma_primary),
        CategoryMeta("PULSA", "Pulsa & Data", "Mobile & Data", R.drawable.ic_phone, R.drawable.bg_tile_soft_yellow, R.color.bank_accent),
        CategoryMeta("EWALLET", "E-Wallet", "E-Wallet", R.drawable.ic_wallet, R.drawable.bg_tile_soft_cyan, R.color.bank_cyan),
        CategoryMeta("BPJS", "BPJS & Asuransi", "BPJS & Insurance", R.drawable.ic_shield, R.drawable.bg_tile_soft_green, R.color.bank_success),
        CategoryMeta("LOAN", "Cicilan & Pinjaman", "Loans & Installments", R.drawable.ic_figma_loan, R.drawable.bg_tile_soft_orange, R.color.bank_orange),
        CategoryMeta("EDUCATION", "Pendidikan", "Education", R.drawable.ic_figma_document, R.drawable.bg_tile_soft_blue, R.color.figma_primary),
        CategoryMeta("FOOD", "Kuliner & Belanja", "Food & Shopping", R.drawable.ic_gift, R.drawable.bg_tile_soft_pink, R.color.figma_soft_pink),
        CategoryMeta("ENTERTAINMENT", "Hiburan", "Entertainment", R.drawable.ic_heart, R.drawable.bg_tile_soft_purple, R.color.bank_purple),
        CategoryMeta("WITHDRAW", "Tarik Tunai", "Cash Withdrawal", R.drawable.ic_figma_atm, R.drawable.bg_tile_soft_cyan, R.color.bank_blue),
        CategoryMeta("FEE", "Biaya Admin", "Fees & Admin", R.drawable.ic_receipt, R.drawable.bg_tile_soft_yellow, R.color.bank_accent),
        CategoryMeta("OTHER", "Lainnya", "Other", R.drawable.ic_more, R.drawable.bg_tile_soft_blue, R.color.figma_text_muted)
    )

    /**
     * Automatically maps a transaction title, category, or counterparty to one of the canonical categories.
     */
    fun resolveCategory(tx: TransactionModel): CategoryMeta {
        val text = "${tx.category} ${tx.title} ${tx.counterparty}".lowercase()

        return when {
            tx.isIncome -> CATEGORIES.first { it.id == "RECEIVE" }
            text.contains("pln") || text.contains("listrik") || text.contains("electric") -> CATEGORIES.first { it.id == "PLN" }
            text.contains("pdam") || text.contains("air") || text.contains("water") -> CATEGORIES.first { it.id == "PDAM" }
            text.contains("pulsa") || text.contains("data") || text.contains("paket") || text.contains("telkomsel") || text.contains("indosat") -> CATEGORIES.first { it.id == "PULSA" }
            text.contains("internet") || text.contains("indihome") || text.contains("biznet") || text.contains("wifi") -> CATEGORIES.first { it.id == "INTERNET" }
            text.contains("qris") || text.contains("qr") -> CATEGORIES.first { it.id == "QRIS" }
            text.contains("tarik") || text.contains("withdraw") || text.contains("atm") -> CATEGORIES.first { it.id == "WITHDRAW" }
            text.contains("topup") || text.contains("top up") || text.contains("isi saldo") -> CATEGORIES.first { it.id == "TOPUP" }
            text.contains("gopay") || text.contains("ovo") || text.contains("dana") || text.contains("shopeepay") || text.contains("linkaja") -> CATEGORIES.first { it.id == "EWALLET" }
            text.contains("bpjs") || text.contains("asuransi") || text.contains("prudential") -> CATEGORIES.first { it.id == "BPJS" }
            text.contains("pinjaman") || text.contains("cicilan") || text.contains("loan") -> CATEGORIES.first { it.id == "LOAN" }
            text.contains("kuliah") || text.contains("sekolah") || text.contains("spp") || text.contains("education") -> CATEGORIES.first { it.id == "EDUCATION" }
            text.contains("makan") || text.contains("kopi") || text.contains("starbucks") || text.contains("resto") || text.contains("food") -> CATEGORIES.first { it.id == "FOOD" }
            text.contains("bioskop") || text.contains("cinema") || text.contains("game") || text.contains("steam") -> CATEGORIES.first { it.id == "ENTERTAINMENT" }
            text.contains("admin") || text.contains("biaya") || text.contains("fee") -> CATEGORIES.first { it.id == "FEE" }
            text.contains("transfer") || text.contains("kirim") -> CATEGORIES.first { it.id == "TRANSFER" }
            else -> CATEGORIES.first { it.id == "OTHER" }
        }
    }
}
