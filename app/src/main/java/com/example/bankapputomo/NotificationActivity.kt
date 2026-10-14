package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.bankapputomo.model.InAppNotificationModel
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.BackOfficeRepository
import com.example.bankapputomo.util.ViewExtensions.bounceClick
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var messageHeaderTitle: TextView
    private lateinit var btnMarkAllRead: TextView

    private lateinit var tabNotifications: TextView
    private lateinit var tabBankAnnouncements: TextView

    private lateinit var viewMessageList: View
    private lateinit var viewMessageDetail: View

    private lateinit var textDetailSubject: TextView
    private lateinit var textDetailTimestamp: TextView
    private lateinit var textDetailBody: TextView
    private lateinit var btnCloseDetail: View

    private lateinit var containerDynamicNotifications: LinearLayout
    private var notifListener: ListenerRegistration? = null
    private var currentNotifications: List<InAppNotificationModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        registerBackHandler(::handleBack)

        btnBack = findViewById(R.id.btnBack)
        messageHeaderTitle = findViewById(R.id.messageHeaderTitle)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        tabNotifications = findViewById(R.id.tabNotifications)
        tabBankAnnouncements = findViewById(R.id.tabBankAnnouncements)

        viewMessageList = findViewById(R.id.viewMessageList)
        viewMessageDetail = findViewById(R.id.viewMessageDetail)

        textDetailSubject = findViewById(R.id.detailMessageTitle)
        textDetailTimestamp = findViewById(R.id.detailMessageTime)
        textDetailBody = findViewById(R.id.detailMessageBody)
        btnCloseDetail = findViewById(R.id.btnCloseDetail)

        containerDynamicNotifications = findViewById(R.id.containerDynamicNotifications)

        val textLogs = findViewById<TextView>(R.id.textLogs)
        textLogs?.visibility = View.GONE

        btnBack.bounceClick {
            handleBack()
        }


        val emptyLayout = findViewById<View>(R.id.layoutNotificationEmpty)
        val lottieEmpty = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieNotificationEmpty)

        btnMarkAllRead.bounceClick {
            findViewById<View>(R.id.itemMessage1)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage2)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage3)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage4)?.visibility = View.GONE

            for (notif in currentNotifications) {
                if (!notif.read) {
                    BackOfficeRepository.markNotificationRead(this, notif.id)
                }
            }

            emptyLayout?.visibility = View.VISIBLE
            if (lottieEmpty != null) {
                lottieEmpty.setAnimation("anim_empty_state.json")
                com.example.bankapputomo.util.LottieMotionHelper.recolor(
                    lottieEmpty,
                    androidx.core.content.ContextCompat.getColor(this, R.color.figma_primary)
                )
                com.example.bankapputomo.util.LottieMotionHelper.playOnce(lottieEmpty)
            }
            Toast.makeText(this, getString(R.string.notification_marked_read), Toast.LENGTH_SHORT).show()
        }

        tabNotifications.bounceClick {
            tabNotifications.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabNotifications.setTextColor(getColor(R.color.white))
            tabBankAnnouncements.setBackgroundResource(android.R.color.transparent)
            tabBankAnnouncements.setTextColor(getColor(R.color.figma_text_muted))
            containerDynamicNotifications.visibility = View.VISIBLE
            findViewById<View>(R.id.itemMessage1)?.visibility = View.VISIBLE
            findViewById<View>(R.id.itemMessage2)?.visibility = View.VISIBLE
            findViewById<View>(R.id.itemMessage3)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage4)?.visibility = View.GONE
        }

        tabBankAnnouncements.bounceClick {
            tabBankAnnouncements.setBackgroundResource(R.drawable.bg_figma_tab_active)
            tabBankAnnouncements.setTextColor(getColor(R.color.white))
            tabNotifications.setBackgroundResource(android.R.color.transparent)
            tabNotifications.setTextColor(getColor(R.color.figma_text_muted))
            containerDynamicNotifications.visibility = View.GONE
            findViewById<View>(R.id.itemMessage1)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage2)?.visibility = View.GONE
            findViewById<View>(R.id.itemMessage3)?.visibility = View.VISIBLE
            findViewById<View>(R.id.itemMessage4)?.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.itemMessage1)?.bounceClick {
            showDetail(
                getString(R.string.notification_transfer_title),
                getString(R.string.notification_transfer_detail_time),
                getString(R.string.notification_transfer_detail_body)
            )
        }

        findViewById<View>(R.id.itemMessage2)?.bounceClick {
            showDetail(
                getString(R.string.notification_security_title),
                getString(R.string.notification_security_detail_time),
                logs ?: getString(R.string.notification_security_detail_body)
            )
        }

        findViewById<View>(R.id.itemMessage3)?.bounceClick {
            showDetail(
                getString(R.string.notification_deposit_title),
                getString(R.string.notification_deposit_detail_time),
                getString(R.string.notification_deposit_detail_body)
            )
        }

        findViewById<View>(R.id.itemMessage4)?.bounceClick {
            showDetail(
                getString(R.string.notification_statement_title),
                getString(R.string.notification_statement_detail_time),
                getString(R.string.notification_statement_detail_body)
            )
        }

        btnCloseDetail.bounceClick {
            showList()
        }

        // Real-time Firestore notification subscription
        notifListener = BackOfficeRepository.listenToNotifications(this) { list ->
            renderDynamicNotifications(list)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notifListener?.remove()
        notifListener = null
    }

    private fun renderDynamicNotifications(list: List<InAppNotificationModel>) {
        currentNotifications = list
        containerDynamicNotifications.removeAllViews()
        if (list.isEmpty()) return

        val inflater = LayoutInflater.from(this)
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

        for (notif in list) {
            val itemView = inflater.inflate(R.layout.item_dynamic_notification, containerDynamicNotifications, false)
            val imgIcon = itemView.findViewById<ImageView>(R.id.imgNotifIcon)
            val textTitle = itemView.findViewById<TextView>(R.id.textNotifTitle)
            val textTime = itemView.findViewById<TextView>(R.id.textNotifTime)
            val textBody = itemView.findViewById<TextView>(R.id.textNotifBody)
            val viewDot = itemView.findViewById<View>(R.id.viewUnreadDot)

            viewDot.visibility = if (notif.read) View.GONE else View.VISIBLE
            itemView.alpha = if (notif.read) 0.75f else 1.0f

            val formattedTime = notif.createdAt?.toDate()?.let { sdf.format(it) } ?: "Recent"
            textTime.text = formattedTime

            val lang = AppStorage.get(this).language
            val isIndo = lang.equals("in", ignoreCase = true) || lang.equals("id", ignoreCase = true)

            when (notif.type) {
                "ticket_reply" -> {
                    imgIcon.setImageResource(R.drawable.ic_support_headset)
                    textTitle.text = if (isIndo) "Balasan Dukungan Nasabah" else "Customer Support Reply"
                    val replyText = notif.params["text"] as? String ?: if (isIndo) "Pesan baru dari tim dukungan" else "New reply from support team"
                    textBody.text = replyText
                    itemView.bounceClick {
                        BackOfficeRepository.markNotificationRead(this, notif.id)
                        val ticketId = (notif.params["ticketId"] as? String).orEmpty()
                        val ticketSubject = notif.params["ticketSubject"] as? String ?: "Support Ticket"
                        val intent = Intent(this, CustomerSupportActivity::class.java).apply {
                            putExtra(CustomerSupportActivity.EXTRA_TICKET_ID, ticketId)
                            putExtra(CustomerSupportActivity.EXTRA_TICKET_SUBJECT, ticketSubject)
                        }
                        startActivity(intent)
                    }
                }
                "card_status_changed" -> {
                    imgIcon.setImageResource(R.drawable.ic_lock_closed)
                    textTitle.text = if (isIndo) "Pembaruan Status Kartu" else "Card Status Update"
                    val status = notif.params["newStatus"] as? String ?: "locked"
                    val reason = notif.params["reasonCode"] as? String ?: "Security"
                    textBody.text = if (isIndo) "Status kartu Anda diperbarui: $status ($reason)" else "Your card status changed: $status ($reason)"
                    itemView.bounceClick {
                        BackOfficeRepository.markNotificationRead(this, notif.id)
                        startActivity(Intent(this, CardDetailActivity::class.java))
                    }
                }
                "account_status_changed" -> {
                    imgIcon.setImageResource(R.drawable.ic_alert_triangle)
                    textTitle.text = if (isIndo) "Pemberitahuan Akun" else "Account Notice"
                    val reason = notif.params["reason"] as? String ?: "Account status updated"
                    textBody.text = reason
                    itemView.bounceClick {
                        BackOfficeRepository.markNotificationRead(this, notif.id)
                        showDetail(textTitle.text.toString(), formattedTime, reason)
                    }
                }
                else -> {
                    imgIcon.setImageResource(R.drawable.ic_figma_document)
                    textTitle.text = if (isIndo) "Pemberitahuan Bank" else "Bank Notification"
                    textBody.text = notif.params["text"] as? String ?: "Important bank update"
                    itemView.bounceClick {
                        BackOfficeRepository.markNotificationRead(this, notif.id)
                        showDetail(textTitle.text.toString(), formattedTime, textBody.text.toString())
                    }
                }
            }

            containerDynamicNotifications.addView(itemView)
        }
    }

    private fun showDetail(subject: String, timestamp: String, body: String) {
        textDetailSubject.text = subject
        textDetailTimestamp.text = timestamp
        textDetailBody.text = body

        viewMessageList.visibility = View.GONE
        viewMessageDetail.visibility = View.VISIBLE
        messageHeaderTitle.text = getString(R.string.notification_detail_header)
        btnMarkAllRead.visibility = View.GONE
    }

    private fun showList() {
        viewMessageList.visibility = View.VISIBLE
        viewMessageDetail.visibility = View.GONE
        messageHeaderTitle.text = getString(R.string.title_messages)
        btnMarkAllRead.visibility = View.VISIBLE
    }

    private fun handleBack() {
        if (viewMessageDetail.visibility == View.VISIBLE) {
            showList()
        } else {
            finish()
        }
    }
}
