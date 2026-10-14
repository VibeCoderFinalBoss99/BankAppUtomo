package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bankapputomo.model.TicketModel
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class SupportTicketsActivity : BaseActivity() {

    private lateinit var recyclerTickets: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var tabAll: TextView
    private lateinit var tabOpen: TextView
    private lateinit var tabProgress: TextView
    private lateinit var tabResolved: TextView

    private var allTickets: List<TicketModel> = emptyList()
    private var currentFilter: String = "ALL"
    private var ticketsListener: ListenerRegistration? = null
    private val adapter = TicketsAdapter { ticket ->
        val intent = Intent(this, CustomerSupportActivity::class.java).apply {
            putExtra(CustomerSupportActivity.EXTRA_TICKET_ID, ticket.id)
            putExtra(CustomerSupportActivity.EXTRA_TICKET_SUBJECT, ticket.subject)
            putExtra(CustomerSupportActivity.EXTRA_TICKET_CATEGORY, ticket.category)
            putExtra(CustomerSupportActivity.EXTRA_TICKET_STATUS, ticket.status)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_tickets)

        findViewById<View>(R.id.btnTicketsBack).bounceClick { finish() }

        recyclerTickets = findViewById(R.id.recyclerTickets)
        layoutEmpty = findViewById(R.id.layoutTicketsEmpty)
        tabAll = findViewById(R.id.tabTicketsAll)
        tabOpen = findViewById(R.id.tabTicketsOpen)
        tabProgress = findViewById(R.id.tabTicketsProgress)
        tabResolved = findViewById(R.id.tabTicketsResolved)

        recyclerTickets.layoutManager = LinearLayoutManager(this)
        recyclerTickets.adapter = adapter

        setupFilterTabs()

        val openChatSupport = {
            val chatIntent = Intent(this, CustomerSupportActivity::class.java)
            val prefillCategory = intent.getStringExtra("prefill_category")
            val prefillSubject = intent.getStringExtra("prefill_subject")
            if (!prefillCategory.isNullOrEmpty()) {
                chatIntent.putExtra(CustomerSupportActivity.EXTRA_PREFILL_CATEGORY, prefillCategory)
            }
            if (!prefillSubject.isNullOrEmpty()) {
                chatIntent.putExtra(CustomerSupportActivity.EXTRA_PREFILL_SUBJECT, prefillSubject)
            }
            startActivity(chatIntent)
        }

        findViewById<View>(R.id.btnCreateTicket).bounceClick { openChatSupport() }
        findViewById<View>(R.id.btnBottomNewTicket).bounceClick { openChatSupport() }
        findViewById<View>(R.id.btnEmptyCreateTicket).bounceClick { openChatSupport() }

        // Check if prefill was requested (e.g. from Blocked Card)
        val prefillCategory = intent.getStringExtra("prefill_category")
        val prefillSubject = intent.getStringExtra("prefill_subject")
        if (!prefillCategory.isNullOrEmpty() || !prefillSubject.isNullOrEmpty()) {
            openChatSupport()
            finish()
            return
        }

        listenToTickets()
    }

    private fun listenToTickets() {
        ticketsListener?.remove()
        ticketsListener = BackOfficeRepository.listenToTickets(this) { list ->
            allTickets = list
            applyFilter()
        }
    }

    private fun setupFilterTabs() {
        tabAll.bounceClick {
            currentFilter = "ALL"
            updateTabStyles()
            applyFilter()
        }
        tabOpen.bounceClick {
            currentFilter = "open"
            updateTabStyles()
            applyFilter()
        }
        tabProgress.bounceClick {
            currentFilter = "in_progress"
            updateTabStyles()
            applyFilter()
        }
        tabResolved.bounceClick {
            currentFilter = "resolved"
            updateTabStyles()
            applyFilter()
        }
    }

    private fun updateTabStyles() {
        val activeBg = R.drawable.bg_figma_tab_active
        val inactiveBg = R.drawable.bg_figma_tab_inactive
        val white = ContextCompat.getColor(this, R.color.white)
        val muted = ContextCompat.getColor(this, R.color.figma_text_muted)

        tabAll.setBackgroundResource(if (currentFilter == "ALL") activeBg else inactiveBg)
        tabAll.setTextColor(if (currentFilter == "ALL") white else muted)

        tabOpen.setBackgroundResource(if (currentFilter == "open") activeBg else inactiveBg)
        tabOpen.setTextColor(if (currentFilter == "open") white else muted)

        tabProgress.setBackgroundResource(if (currentFilter == "in_progress") activeBg else inactiveBg)
        tabProgress.setTextColor(if (currentFilter == "in_progress") white else muted)

        tabResolved.setBackgroundResource(if (currentFilter == "resolved") activeBg else inactiveBg)
        tabResolved.setTextColor(if (currentFilter == "resolved") white else muted)
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "ALL" -> allTickets
            "open" -> allTickets.filter { it.status == "open" || it.status == "waiting_customer" }
            "in_progress" -> allTickets.filter { it.status == "in_progress" }
            "resolved" -> allTickets.filter { it.status == "resolved" || it.status == "closed" }
            else -> allTickets
        }

        if (filtered.isEmpty()) {
            recyclerTickets.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            recyclerTickets.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        ticketsListener?.remove()
    }

    // RecyclerView Adapter
    class TicketsAdapter(
        private val onClick: (TicketModel) -> Unit
    ) : RecyclerView.Adapter<TicketsAdapter.TicketViewHolder>() {

        private var items: List<TicketModel> = emptyList()

        fun submitList(newList: List<TicketModel>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket_card, parent, false)
            return TicketViewHolder(v)
        }

        override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size

        class TicketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val textCategory: TextView = view.findViewById(R.id.textTicketCategory)
            private val textStatus: TextView = view.findViewById(R.id.textTicketStatus)
            private val textSubject: TextView = view.findViewById(R.id.textTicketSubject)
            private val textDate: TextView = view.findViewById(R.id.textTicketDate)
            private val dotUnread: View = view.findViewById(R.id.dotUnreadIndicator)

            fun bind(ticket: TicketModel, onClick: (TicketModel) -> Unit) {
                textCategory.text = ticket.category
                textSubject.text = ticket.subject

                val ctx = itemView.context
                val statusText = when (ticket.status) {
                    "open" -> ctx.getString(R.string.ticket_status_open)
                    "in_progress" -> ctx.getString(R.string.ticket_status_in_progress)
                    "waiting_customer" -> ctx.getString(R.string.ticket_status_waiting)
                    "resolved" -> ctx.getString(R.string.ticket_status_resolved)
                    "closed" -> ctx.getString(R.string.ticket_status_closed)
                    else -> ticket.status
                }
                textStatus.text = statusText

                when (ticket.status) {
                    "resolved" -> {
                        textStatus.setTextColor(ContextCompat.getColor(ctx, R.color.bank_success))
                        textStatus.setBackgroundResource(R.drawable.bg_tile_soft_green)
                    }
                    "closed" -> {
                        textStatus.setTextColor(ContextCompat.getColor(ctx, R.color.figma_text_muted))
                        textStatus.setBackgroundResource(R.drawable.bg_tile_soft_blue)
                    }
                    "in_progress" -> {
                        textStatus.setTextColor(ContextCompat.getColor(ctx, R.color.figma_primary))
                        textStatus.setBackgroundResource(R.drawable.bg_tile_soft_blue)
                    }
                    else -> {
                        textStatus.setTextColor(ContextCompat.getColor(ctx, R.color.figma_warning))
                        textStatus.setBackgroundResource(R.drawable.bg_tile_soft_yellow)
                    }
                }

                if (ticket.createdAt != null) {
                    val date = ticket.createdAt.toDate()
                    val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    textDate.text = fmt.format(date)
                } else {
                    textDate.text = "-"
                }

                dotUnread.visibility = if (ticket.unreadForUser) View.VISIBLE else View.GONE

                itemView.bounceClick { onClick(ticket) }
            }
        }
    }
}
