package com.example.made.ui.splash

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.made.databinding.ActivitySplashBinding
import com.example.made.ui.admin.AdminDashboardActivity
import com.example.made.ui.auth.LoginActivity
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.util.Constants
import com.example.made.util.SessionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (intent?.data != null) {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
            return
        }

        animateSplash()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNext()
        }, Constants.SPLASH_DELAY_MS)
    }

    private fun animateSplash() {
        val logoAnim = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 800 })
            addAnimation(TranslateAnimation(0f, 0f, 50f, 0f).apply { duration = 800 })
        }
        binding.ivLogo.startAnimation(logoAnim)

        val nameAnim = AlphaAnimation(0f, 1f).apply { duration = 800; startOffset = 300 }
        binding.tvAppName.startAnimation(nameAnim)

        val taglineAnim = AlphaAnimation(0f, 1f).apply { duration = 800; startOffset = 600 }
        binding.tvTagline.startAnimation(taglineAnim)
    }

    private fun navigateToNext() {
        val destination = if (sessionManager.isLoggedIn && sessionManager.isAdminUser) {
            Intent(this, AdminDashboardActivity::class.java)
        } else if (sessionManager.isLoggedIn) {
            Intent(this, DashboardActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        val options = ActivityOptions
            .makeSceneTransitionAnimation(this, binding.ivLogo, "app_logo_transition")
            .toBundle()
        startActivity(destination, options)
        finish()
    }
}
