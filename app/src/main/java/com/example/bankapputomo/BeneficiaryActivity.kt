package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BankSelectionBottomSheet
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class BeneficiaryActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var beneficiaryHeaderTitle: TextView
    private lateinit var btnAddBeneficiaryHeader: TextView

    private lateinit var viewBeneficiaryList: View
    private lateinit var viewBeneficiaryAdd: View
    private lateinit var viewBeneficiaryDetail: View

    private lateinit var inputSearchBeneficiary: EditText
    private lateinit var containerBeneficiaries: LinearLayout

    private lateinit var inputAddName: EditText
    private lateinit var btnSelectAddBank: View
    private lateinit var textSelectedBank: TextView
    private var selectedAddBank: String = ""
    private lateinit var inputAddAccount: EditText
    private lateinit var btnSaveNewBeneficiary: TextView

    private lateinit var textDetailName: TextView
    private lateinit var textDetailBank: TextView
    private lateinit var textDetailAccount: TextView
    private lateinit var imageDetailAvatar: ImageView
    private lateinit var btnTransferToBeneficiary: TextView
    private lateinit var btnEditBeneficiary: TextView
    private lateinit var btnDeleteBeneficiary: TextView

    private var currentDetailName = ""
    private var currentDetailBank = ""
    private var currentDetailAccount = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beneficiary)
        registerBackHandler(::handleBackNavigation)

        btnBack = findViewById(R.id.btnBack)
        beneficiaryHeaderTitle = findViewById(R.id.beneficiaryHeaderTitle)
        btnAddBeneficiaryHeader = findViewById(R.id.btnAddBeneficiaryHeader)

        viewBeneficiaryList = findViewById(R.id.viewBeneficiaryList)
        viewBeneficiaryAdd = findViewById(R.id.viewBeneficiaryAdd)
        viewBeneficiaryDetail = findViewById(R.id.viewBeneficiaryDetail)

        inputSearchBeneficiary = findViewById(R.id.inputSearchBeneficiary)
        containerBeneficiaries = findViewById(R.id.containerBeneficiaries)

        inputAddName = findViewById(R.id.inputAddName)
        btnSelectAddBank = findViewById(R.id.btnSelectAddBank)
        textSelectedBank = findViewById(R.id.textSelectedBank)
        inputAddAccount = findViewById(R.id.inputAddAccount)
        btnSaveNewBeneficiary = findViewById(R.id.btnSaveNewBeneficiary)

        btnSelectAddBank.bounceClick {
            showBankSelectionDialog(selectedAddBank) { chosen ->
                selectedAddBank = chosen
                textSelectedBank.text = chosen
                textSelectedBank.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
            }
        }

        textDetailName = findViewById(R.id.textDetailName)
        textDetailBank = findViewById(R.id.textDetailBank)
        textDetailAccount = findViewById(R.id.textDetailAccount)
        imageDetailAvatar = findViewById(R.id.imageDetailAvatar)
        btnTransferToBeneficiary = findViewById(R.id.btnTransferToBeneficiary)
        btnEditBeneficiary = findViewById(R.id.btnEditBeneficiary)
        btnDeleteBeneficiary = findViewById(R.id.btnDeleteBeneficiary)

        btnBack.bounceClick {
            handleBackNavigation()
        }

        btnAddBeneficiaryHeader.bounceClick {
            showAddView()
        }

        btnSaveNewBeneficiary.bounceClick {
            val name = inputAddName.text.toString().trim()
            val bank = selectedAddBank.trim()
            val account = inputAddAccount.text.toString().trim()

            if (name.isEmpty() || bank.isEmpty() || account.isEmpty()) {
                val msg = if (bank.isEmpty()) getString(R.string.select_bank_prompt) else "Mohon lengkapi seluruh data penerima"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            AppStorage.get().addBeneficiary(name, account, bank)
            Toast.makeText(this, "Beneficiary saved successfully", Toast.LENGTH_SHORT).show()
            renderBeneficiaries()
            showListView()
        }

        btnTransferToBeneficiary.bounceClick {
            val intent = Intent(this, TransferActivity::class.java).apply {
                putExtra("prefill_name", currentDetailName)
                putExtra("prefill_account", currentDetailAccount)
            }
            startActivity(intent)
            finish()
        }

        btnEditBeneficiary.bounceClick {
            showEditBeneficiaryBottomSheet()
        }

        btnDeleteBeneficiary.bounceClick {
            com.example.bankapputomo.util.CustomConfirmationDialog.show(
                context = this,
                title = "Hapus Penerima",
                message = "Apakah Anda yakin ingin menghapus $currentDetailName dari daftar penerima tersimpan?",
                positiveText = "Hapus",
                negativeText = "Batal",
                iconRes = R.drawable.ic_figma_beneficiary,
                isDestructive = true,
                onConfirm = {
                    AppStorage.get().deleteBeneficiary(currentDetailName)
                    Toast.makeText(this, "$currentDetailName dihapus", Toast.LENGTH_SHORT).show()
                    renderBeneficiaries()
                    showListView()
                }
            )
        }

        inputSearchBeneficiary.addTextChangedListener { s ->
            val query = s?.toString()?.trim() ?: ""
            renderBeneficiaries(query)
        }

        renderBeneficiaries()
    }

    private fun getAvatarForName(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("push") -> R.drawable.avatar_push
            lower.contains("olivia") -> R.drawable.avatar_olivia
            lower.contains("emma") -> R.drawable.avatar_emma
            lower.contains("justin") -> R.drawable.avatar_justin
            lower.contains("alexander") -> R.drawable.avatar_alexander
            else -> {
                val avatars = listOf(
                    R.drawable.avatar_push,
                    R.drawable.avatar_olivia,
                    R.drawable.avatar_emma,
                    R.drawable.avatar_justin,
                    R.drawable.avatar_alexander
                )
                avatars[kotlin.math.abs(name.hashCode()) % avatars.size]
            }
        }
    }

    private fun renderBeneficiaries(query: String = "") {
        containerBeneficiaries.removeAllViews()
        val beneficiaries = AppStorage.get().getBeneficiaries()

        val filtered = if (query.isEmpty()) beneficiaries else beneficiaries.filter {
            it.name.contains(query, ignoreCase = true) || it.bank.contains(query, ignoreCase = true) || it.account.contains(query)
        }

        filtered.forEach { item ->
            val itemView = layoutInflater.inflate(R.layout.item_beneficiary_row, containerBeneficiaries, false)
            itemView.findViewById<TextView>(R.id.itemBenName).text = item.name
            itemView.findViewById<TextView>(R.id.itemBenBank).text = item.bank
            itemView.findViewById<TextView>(R.id.itemBenAccount).text = item.account
            itemView.findViewById<ImageView>(R.id.itemBenAvatar)?.setImageResource(getAvatarForName(item.name))

            itemView.bounceClick {
                showDetailView(item.name, item.bank, item.account)
            }
            containerBeneficiaries.addView(itemView)
        }
    }

    private fun showListView() {
        viewBeneficiaryList.visibility = View.VISIBLE
        viewBeneficiaryAdd.visibility = View.GONE
        viewBeneficiaryDetail.visibility = View.GONE
        beneficiaryHeaderTitle.text = "Beneficiary"
        btnAddBeneficiaryHeader.visibility = View.VISIBLE
    }

    private fun showAddView() {
        viewBeneficiaryList.visibility = View.GONE
        viewBeneficiaryAdd.visibility = View.VISIBLE
        viewBeneficiaryDetail.visibility = View.GONE
        beneficiaryHeaderTitle.text = "Add Beneficiary"
        btnAddBeneficiaryHeader.visibility = View.GONE

        inputAddName.setText("")
        selectedAddBank = ""
        textSelectedBank.setText(R.string.select_bank_prompt)
        textSelectedBank.setTextColor(ContextCompat.getColor(this, R.color.figma_text_muted))
        inputAddAccount.setText("")
    }

    private fun showDetailView(name: String, bank: String, account: String) {
        currentDetailName = name
        currentDetailBank = bank
        currentDetailAccount = account

        textDetailName.text = name
        textDetailBank.text = bank
        textDetailAccount.text = account
        imageDetailAvatar.setImageResource(getAvatarForName(name))

        viewBeneficiaryList.visibility = View.GONE
        viewBeneficiaryAdd.visibility = View.GONE
        viewBeneficiaryDetail.visibility = View.VISIBLE
        beneficiaryHeaderTitle.text = "Beneficiary Details"
        btnAddBeneficiaryHeader.visibility = View.GONE
    }

    private fun showEditBeneficiaryBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_beneficiary, null)
        dialog.setContentView(sheetView)

        val inputName = sheetView.findViewById<EditText>(R.id.inputEditBenName)
        val btnSelectBank = sheetView.findViewById<View>(R.id.btnSelectEditBank)
        val textEditBank = sheetView.findViewById<TextView>(R.id.textEditSelectedBank)
        val inputAccount = sheetView.findViewById<EditText>(R.id.inputEditBenAccount)
        val btnSave = sheetView.findViewById<View>(R.id.btnSaveEditBeneficiary)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelEditBeneficiary)

        inputName.setText(currentDetailName)
        var selectedEditBank = currentDetailBank
        textEditBank.text = currentDetailBank
        textEditBank.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
        inputAccount.setText(currentDetailAccount)

        btnSelectBank.bounceClick {
            showBankSelectionDialog(selectedEditBank) { chosen ->
                selectedEditBank = chosen
                textEditBank.text = chosen
                textEditBank.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
            }
        }

        btnSave.bounceClick {
            val newName = inputName.text.toString().trim()
            val newBank = selectedEditBank.trim()
            val newAccount = inputAccount.text.toString().trim()

            if (newName.isEmpty() || newBank.isEmpty() || newAccount.isEmpty()) {
                val msg = if (newBank.isEmpty()) getString(R.string.select_bank_prompt) else "Mohon lengkapi seluruh data penerima"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val updated = com.example.bankapputomo.util.BeneficiaryModel(
                id = "",
                name = newName,
                bank = newBank,
                account = newAccount
            )

            AppStorage.get().updateBeneficiary(currentDetailName, updated)
            dialog.dismiss()
            Toast.makeText(this, "Data penerima berhasil diperbarui", Toast.LENGTH_SHORT).show()

            currentDetailName = newName
            currentDetailBank = newBank
            currentDetailAccount = newAccount
            textDetailName.text = newName
            textDetailBank.text = newBank
            textDetailAccount.text = newAccount
            imageDetailAvatar.setImageResource(getAvatarForName(newName))

            renderBeneficiaries()
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showBankSelectionDialog(currentSelected: String, onSelected: (String) -> Unit) {
        BankSelectionBottomSheet.show(this, currentSelected, onSelected)
    }

    companion object {
        val BANK_LIST = BankSelectionBottomSheet.ALL_BANKS.map { it.name }
    }

    private fun handleBackNavigation() {
        if (viewBeneficiaryDetail.visibility == View.VISIBLE || viewBeneficiaryAdd.visibility == View.VISIBLE) {
            showListView()
        } else {
            finish()
        }
    }
}
