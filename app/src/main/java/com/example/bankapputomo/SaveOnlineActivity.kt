package com.example.bankapputomo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.CustomConfirmationDialog
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.IdrFormatter
import com.example.bankapputomo.util.SavingGoalModel
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ListenerRegistration

class SaveOnlineActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var saveHeaderTitle: TextView
    private lateinit var btnHeaderNewSave: TextView

    private lateinit var viewSaveDashboard: View
    private lateinit var viewCreateSaveFlow: View
    private lateinit var viewSaveSuccess: View

    private lateinit var textTotalSavingsBalance: TextView
    private lateinit var textActiveGoalsSubtitle: TextView
    private lateinit var containerSavingGoals: LinearLayout
    private lateinit var layoutEmptyGoals: LinearLayout
    private lateinit var btnOpenNewSavings: TextView

    private lateinit var inputGoalName: EditText
    private lateinit var inputTargetAmount: EditText
    private lateinit var chipDuration3: TextView
    private lateinit var chipDuration6: TextView
    private lateinit var chipDuration12: TextView
    private lateinit var chipDuration24: TextView
    private lateinit var textMonthlyDebitCalculated: TextView
    private lateinit var cbAgreeTerms: CheckBox
    private lateinit var btnSubmitCreateGoal: TextView

    private lateinit var textSuccessGoalName: TextView
    private lateinit var textSuccessTarget: TextView
    private lateinit var textSuccessDuration: TextView
    private lateinit var btnDoneSave: TextView

    private var targetAmount: Long = 10000000
    private var durationMonths: Int = 6
    private var goalsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_online)
        registerBackHandler(::handleBack)

        btnBack = findViewById(R.id.btnBack)
        saveHeaderTitle = findViewById(R.id.saveHeaderTitle)
        btnHeaderNewSave = findViewById(R.id.btnHeaderNewSave)

        viewSaveDashboard = findViewById(R.id.viewSaveDashboard)
        viewCreateSaveFlow = findViewById(R.id.viewCreateSaveFlow)
        viewSaveSuccess = findViewById(R.id.viewSaveSuccess)

        textTotalSavingsBalance = findViewById(R.id.textTotalSavingsBalance)
        textActiveGoalsSubtitle = findViewById(R.id.textActiveGoalsSubtitle)
        containerSavingGoals = findViewById(R.id.containerSavingGoals)
        layoutEmptyGoals = findViewById(R.id.layoutEmptyGoals)
        btnOpenNewSavings = findViewById(R.id.btnOpenNewSavings)

        inputGoalName = findViewById(R.id.inputGoalName)
        inputTargetAmount = findViewById(R.id.inputTargetAmount)
        chipDuration3 = findViewById(R.id.chipDuration3)
        chipDuration6 = findViewById(R.id.chipDuration6)
        chipDuration12 = findViewById(R.id.chipDuration12)
        chipDuration24 = findViewById(R.id.chipDuration24)
        textMonthlyDebitCalculated = findViewById(R.id.textMonthlyDebitCalculated)
        cbAgreeTerms = findViewById(R.id.cbAgreeTerms)
        btnSubmitCreateGoal = findViewById(R.id.btnSubmitCreateGoal)

        textSuccessGoalName = findViewById(R.id.textSuccessGoalName)
        textSuccessTarget = findViewById(R.id.textSuccessTarget)
        textSuccessDuration = findViewById(R.id.textSuccessDuration)
        btnDoneSave = findViewById(R.id.btnDoneSave)

        // Initial render from local cache
        renderGoals(AppStorage.get().getSavingGoals())

        // Real-time synchronization from the user's Firestore subcollection.
        goalsListener = BackOfficeRepository.listenToSavingGoals(this) { updatedGoals ->
            runOnUiThread {
                renderGoals(updatedGoals)
            }
        }

        btnBack.bounceClick {
            handleBack()
        }

        btnHeaderNewSave.bounceClick {
            showCreateFlow()
        }

        btnOpenNewSavings.bounceClick {
            showCreateFlow()
        }

        val chips = listOf(
            chipDuration3 to 3,
            chipDuration6 to 6,
            chipDuration12 to 12,
            chipDuration24 to 24
        )

        chips.forEach { (chip, months) ->
            chip.bounceClick {
                durationMonths = months
                chips.forEach { (c, _) ->
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_figma_button_primary)
                        c.setTextColor(resources.getColor(android.R.color.white, theme))
                    } else {
                        c.setBackgroundResource(R.drawable.bg_figma_card)
                        c.setTextColor(resources.getColor(R.color.figma_text_title, theme))
                    }
                }
                recalculateMonthlyDebit()
            }
        }

        inputTargetAmount.addTextChangedListener(IdrFormatter.createWatcher(inputTargetAmount) { raw ->
            targetAmount = if (raw > 0) raw else 10000000
            recalculateMonthlyDebit()
        })

        btnSubmitCreateGoal.bounceClick {
            val name = inputGoalName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            if (!cbAgreeTerms.isChecked) {
                Toast.makeText(this, "Please agree to the savings terms", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val created = AppStorage.get().addSavingGoal(name, targetAmount, durationMonths)

            textSuccessGoalName.text = name
            textSuccessTarget.text = IdrFormatter.format(targetAmount)
            textSuccessDuration.text = "$durationMonths Months"

            viewSaveDashboard.visibility = View.GONE
            viewCreateSaveFlow.visibility = View.GONE
            viewSaveSuccess.visibility = View.VISIBLE
            saveHeaderTitle.text = "Goal Created"
            btnHeaderNewSave.visibility = View.GONE

            // Instantly refresh local list & trigger sync
            renderGoals(AppStorage.get().getSavingGoals())
        }

        btnDoneSave.bounceClick {
            showDashboard()
        }
    }

    private fun renderGoals(goals: List<SavingGoalModel>) {
        containerSavingGoals.removeAllViews()

        if (goals.isEmpty()) {
            layoutEmptyGoals.visibility = View.VISIBLE
            textTotalSavingsBalance.text = "Rp 0"
            textActiveGoalsSubtitle.text = "0 Active Goals • Up to 5.5% p.a. interest"
            return
        }

        layoutEmptyGoals.visibility = View.GONE

        val totalSavings = goals.sumOf { it.currentAmount }
        textTotalSavingsBalance.text = IdrFormatter.format(totalSavings)
        textActiveGoalsSubtitle.text = "${goals.size} Active Goals • Up to 5.5% p.a. interest"

        val inflater = LayoutInflater.from(this)
        goals.forEach { goal ->
            val itemView = inflater.inflate(R.layout.item_saving_goal, containerSavingGoals, false)

            val textName = itemView.findViewById<TextView>(R.id.textGoalItemName)
            val textPercentage = itemView.findViewById<TextView>(R.id.textGoalItemPercentage)
            val progressBar = itemView.findViewById<ProgressBar>(R.id.progressGoalItem)
            val textAmounts = itemView.findViewById<TextView>(R.id.textGoalItemAmounts)
            val textDuration = itemView.findViewById<TextView>(R.id.textGoalItemDuration)
            val btnTopUp = itemView.findViewById<View>(R.id.btnGoalTopUp)
            val btnEdit = itemView.findViewById<View>(R.id.btnGoalEdit)
            val btnDelete = itemView.findViewById<View>(R.id.btnGoalDelete)

            textName.text = goal.name
            val pct = if (goal.targetAmount > 0) {
                ((goal.currentAmount.toDouble() / goal.targetAmount) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            textPercentage.text = "$pct%"
            progressBar.progress = pct

            if (pct >= 80) {
                textPercentage.setTextColor(ContextCompat.getColor(this, R.color.figma_secondary))
                progressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.figma_secondary)
            } else {
                textPercentage.setTextColor(ContextCompat.getColor(this, R.color.figma_primary))
                progressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.figma_primary)
            }

            textAmounts.text = "${IdrFormatter.format(goal.currentAmount)} / ${IdrFormatter.format(goal.targetAmount)}"
            textDuration.text = "${goal.durationMonths} mos left"

            btnTopUp.bounceClick {
                showTopUpBottomSheet(goal)
            }

            btnEdit.bounceClick {
                showEditGoalBottomSheet(goal)
            }

            btnDelete.bounceClick {
                CustomConfirmationDialog.show(
                    context = this,
                    title = "Hapus Tujuan Tabungan",
                    message = "Apakah Anda yakin ingin menghapus '${goal.name}'?\n\nSaldo sebesar ${IdrFormatter.format(goal.currentAmount)} akan dikembalikan ke saldo utama Anda.",
                    positiveText = "Hapus & Kembalikan Saldo",
                    negativeText = "Batal",
                    iconRes = R.drawable.ic_figma_piggy,
                    isDestructive = true,
                    onConfirm = {
                        val refunded = AppStorage.get().deleteSavingGoal(goal.id)
                        Toast.makeText(
                            this,
                            "${IdrFormatter.format(refunded)} berhasil dikembalikan ke saldo utama",
                            Toast.LENGTH_LONG
                        ).show()
                        renderGoals(AppStorage.get().getSavingGoals())
                    }
                )
            }

            containerSavingGoals.addView(itemView)
        }
    }

    private fun showTopUpBottomSheet(goal: SavingGoalModel) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_topup_goal, null)
        dialog.setContentView(sheetView)

        val textTitle = sheetView.findViewById<TextView>(R.id.textTopUpGoalTitle)
        val textAvailable = sheetView.findViewById<TextView>(R.id.textTopUpAvailableBalance)
        val inputAmount = sheetView.findViewById<EditText>(R.id.inputTopUpAmount)
        val btnSubmit = sheetView.findViewById<View>(R.id.btnSubmitTopUpGoal)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelTopUpGoal)

        val chip100k = sheetView.findViewById<TextView>(R.id.chipTopUp100k)
        val chip500k = sheetView.findViewById<TextView>(R.id.chipTopUp500k)
        val chip1M = sheetView.findViewById<TextView>(R.id.chipTopUp1M)
        val chip5M = sheetView.findViewById<TextView>(R.id.chipTopUp5M)

        val storage = AppStorage.get()
        textTitle.text = "Setor ke: ${goal.name}"
        textAvailable.text = "Saldo utama: ${IdrFormatter.format(storage.debitBalance)}"

        var chosenAmount = 500000L
        inputAmount.setText(IdrFormatter.format(chosenAmount))
        inputAmount.addTextChangedListener(IdrFormatter.createWatcher(inputAmount) { raw ->
            if (raw > 0) chosenAmount = raw
        })

        fun selectChip(chip: TextView, amt: Long) {
            chosenAmount = amt
            inputAmount.setText(IdrFormatter.format(amt))
            listOf(chip100k, chip500k, chip1M, chip5M).forEach { c ->
                if (c == chip) {
                    c.setBackgroundResource(R.drawable.bg_figma_button_primary)
                    c.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                } else {
                    c.setBackgroundResource(R.drawable.bg_figma_card)
                    c.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
                }
            }
        }

        chip100k.bounceClick { selectChip(chip100k, 100000L) }
        chip500k.bounceClick { selectChip(chip500k, 500000L) }
        chip1M.bounceClick { selectChip(chip1M, 1000000L) }
        chip5M.bounceClick { selectChip(chip5M, 5000000L) }

        btnSubmit.bounceClick {
            if (chosenAmount <= 0) {
                Toast.makeText(this, "Masukkan nominal yang valid", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }
            if (storage.debitBalance < chosenAmount) {
                Toast.makeText(this, "Saldo utama tidak mencukupi", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val success = storage.topUpSavingGoal(goal.id, chosenAmount)
            if (success) {
                dialog.dismiss()
                Toast.makeText(this, "Berhasil menyetor ${IdrFormatter.format(chosenAmount)} ke ${goal.name}!", Toast.LENGTH_SHORT).show()
                renderGoals(storage.getSavingGoals())
            } else {
                Toast.makeText(this, "Gagal menyetor dana", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditGoalBottomSheet(goal: SavingGoalModel) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_goal, null)
        dialog.setContentView(sheetView)

        val inputName = sheetView.findViewById<EditText>(R.id.inputEditGoalName)
        val inputTarget = sheetView.findViewById<EditText>(R.id.inputEditGoalTarget)
        val btnSave = sheetView.findViewById<View>(R.id.btnSaveEditGoal)
        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelEditGoal)

        val chip3 = sheetView.findViewById<TextView>(R.id.chipEditDuration3)
        val chip6 = sheetView.findViewById<TextView>(R.id.chipEditDuration6)
        val chip12 = sheetView.findViewById<TextView>(R.id.chipEditDuration12)
        val chip24 = sheetView.findViewById<TextView>(R.id.chipEditDuration24)

        inputName.setText(goal.name)
        var editedTarget = goal.targetAmount
        inputTarget.setText(IdrFormatter.format(editedTarget))
        inputTarget.addTextChangedListener(IdrFormatter.createWatcher(inputTarget) { raw ->
            if (raw > 0) editedTarget = raw
        })

        var editedDuration = goal.durationMonths
        val durationChips = listOf(chip3 to 3, chip6 to 6, chip12 to 12, chip24 to 24)

        fun updateChips(selectedMonths: Int) {
            editedDuration = selectedMonths
            durationChips.forEach { (c, m) ->
                if (m == selectedMonths) {
                    c.setBackgroundResource(R.drawable.bg_figma_button_primary)
                    c.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                } else {
                    c.setBackgroundResource(R.drawable.bg_figma_card)
                    c.setTextColor(ContextCompat.getColor(this, R.color.figma_text_title))
                }
            }
        }

        updateChips(editedDuration)

        durationChips.forEach { (chip, months) ->
            chip.bounceClick { updateChips(months) }
        }

        btnSave.bounceClick {
            val newName = inputName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Nama tujuan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@bounceClick
            }

            val monthly = if (editedDuration > 0) editedTarget / editedDuration else editedTarget
            val updated = goal.copy(
                name = newName,
                targetAmount = editedTarget,
                durationMonths = editedDuration,
                monthlyDebit = monthly
            )

            AppStorage.get().updateSavingGoal(updated)
            dialog.dismiss()
            Toast.makeText(this, "Tujuan tabungan berhasil diperbarui", Toast.LENGTH_SHORT).show()
            renderGoals(AppStorage.get().getSavingGoals())
        }

        btnCancel.bounceClick {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun recalculateMonthlyDebit() {
        val monthly = targetAmount / durationMonths
        textMonthlyDebitCalculated.text = "${IdrFormatter.format(monthly)} / month"
    }

    private fun showDashboard() {
        renderGoals(AppStorage.get().getSavingGoals())
        viewSaveDashboard.visibility = View.VISIBLE
        viewCreateSaveFlow.visibility = View.GONE
        viewSaveSuccess.visibility = View.GONE
        saveHeaderTitle.text = "Save Online"
        btnHeaderNewSave.visibility = View.VISIBLE
    }

    private fun showCreateFlow() {
        viewSaveDashboard.visibility = View.GONE
        viewCreateSaveFlow.visibility = View.VISIBLE
        viewSaveSuccess.visibility = View.GONE
        saveHeaderTitle.text = "Create Saving Goal"
        btnHeaderNewSave.visibility = View.GONE
    }

    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            BackOfficeRepository.fetchSavingGoals(this) { list ->
                runOnUiThread { renderGoals(list) }
            }
            pollHandler.postDelayed(this, 3000)
        }
    }

    override fun onResume() {
        super.onResume()
        renderGoals(AppStorage.get().getSavingGoals())
        BackOfficeRepository.fetchSavingGoals(this) { list ->
            runOnUiThread { renderGoals(list) }
        }
        pollHandler.removeCallbacks(pollRunnable)
        pollHandler.postDelayed(pollRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    override fun onDestroy() {
        goalsListener?.remove()
        pollHandler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    private fun handleBack() {
        if (viewSaveSuccess.visibility == View.VISIBLE || viewCreateSaveFlow.visibility == View.VISIBLE) {
            showDashboard()
        } else {
            finish()
        }
    }
}
