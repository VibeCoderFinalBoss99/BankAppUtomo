package com.example.bankapputomo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.bankapputomo.util.AppStorage
import com.example.bankapputomo.util.ViewExtensions.bounceClick

class OnboardingActivity : BaseActivity() {

    data class OnboardingItem(val imageRes: Int, val titleRes: Int, val subtitleRes: Int)

    private val slides = listOf(
        OnboardingItem(
            R.drawable.img_onboard_slide1,
            R.string.onboarding_slide_one_title,
            R.string.onboarding_slide_one_subtitle
        ),
        OnboardingItem(
            R.drawable.img_onboard_slide2,
            R.string.onboarding_slide_two_title,
            R.string.onboarding_slide_two_subtitle
        ),
        OnboardingItem(
            R.drawable.img_onboard_slide3,
            R.string.onboarding_slide_three_title,
            R.string.onboarding_slide_three_subtitle
        )
    )

    private lateinit var pager: ViewPager2
    private lateinit var btnNext: TextView
    private lateinit var btnSkip: TextView
    private lateinit var layoutAuthButtons: View
    private lateinit var btnRegister: TextView
    private lateinit var btnLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.pagerOnboarding)
        btnNext = findViewById(R.id.btnNextOnboarding)
        btnSkip = findViewById(R.id.btnSkipOnboarding)
        layoutAuthButtons = findViewById(R.id.layoutAuthButtons)
        btnRegister = findViewById(R.id.btnRegisterOnboarding)
        btnLogin = findViewById(R.id.btnLoginOnboarding)

        pager.adapter = OnboardingAdapter(slides)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == slides.size - 1) {
                    btnNext.visibility = View.GONE
                    btnSkip.visibility = View.GONE
                    layoutAuthButtons.visibility = View.VISIBLE
                } else {
                    btnNext.visibility = View.VISIBLE
                    btnSkip.visibility = View.VISIBLE
                    layoutAuthButtons.visibility = View.GONE
                }
            }
        })

        btnNext.bounceClick {
            if (pager.currentItem < slides.size - 1) {
                pager.currentItem += 1
            }
        }

        btnSkip.bounceClick {
            proceedToLogin()
        }

        btnRegister.bounceClick {
            proceedToRegister()
        }

        btnLogin.bounceClick {
            proceedToLogin()
        }
    }

    private fun proceedToRegister() {
        AppStorage.get().hasSeenOnboarding = true
        AppStorage.get().isLoggedIn = false
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }

    private fun proceedToLogin() {
        AppStorage.get().hasSeenOnboarding = true
        AppStorage.get().isLoggedIn = false
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    inner class OnboardingAdapter(private val items: List<OnboardingItem>) :
        RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val img: ImageView = itemView.findViewById(R.id.imgOnboarding)
            val title: TextView = itemView.findViewById(R.id.textOnboardingTitle)
            val subtitle: TextView = itemView.findViewById(R.id.textOnboardingSubtitle)
            val dot0: View = itemView.findViewById(R.id.slideDot0)
            val dot1: View = itemView.findViewById(R.id.slideDot1)
            val dot2: View = itemView.findViewById(R.id.slideDot2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_slide, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.img.setImageResource(item.imageRes)
            holder.title.text = holder.itemView.context.getString(item.titleRes)
            holder.subtitle.text = holder.itemView.context.getString(item.subtitleRes)

            val dots = listOf(holder.dot0, holder.dot1, holder.dot2)
            dots.forEachIndexed { index, dot ->
                val layoutParams = dot.layoutParams
                if (index == position) {
                    layoutParams.width = (24 * holder.itemView.resources.displayMetrics.density).toInt()
                    dot.setBackgroundResource(R.drawable.bg_figma_button_primary)
                } else {
                    layoutParams.width = (8 * holder.itemView.resources.displayMetrics.density).toInt()
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_inactive)
                }
                dot.layoutParams = layoutParams
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
