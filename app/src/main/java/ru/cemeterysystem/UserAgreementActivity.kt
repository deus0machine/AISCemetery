package ru.sevostyanov.aiscemetery

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Activity для отображения пользовательского соглашения
 */
class UserAgreementActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var agreementContent: TextView
    private lateinit var acceptButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement)

        initViews()
        setupToolbar()
        setupContent()
        setupButtons()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        agreementContent = findViewById(R.id.agreement_content)
        acceptButton = findViewById(R.id.accept_button)
    }

    private fun setupContent() {
        // Устанавливаем HTML текст соглашения
        val agreementText = getString(R.string.user_agreement_text)
        agreementContent.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(agreementText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(agreementText)
        }
    }

    private fun setupToolbar() {
        // Настройка toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        // Кнопка "Понятно" просто закрывает экран
        acceptButton.setOnClickListener {
            finish()
        }
    }
} 