package com.michaelzhou.viewdemo.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.michaelzhou.viewdemo.R
import com.michaelzhou.viewdemo.todo.TodoActivity

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        viewModel.isLoginEnabled.observe(this) { btnLogin.isEnabled = it }

        etEmail.addTextChangedListener(onChanged { viewModel.onEmailChanged(it) })
        etPassword.addTextChangedListener(onChanged { viewModel.onPasswordChanged(it) })

        btnLogin.setOnClickListener {
            startActivity(Intent(this, TodoActivity::class.java))
        }
    }

    private fun onChanged(action: (String) -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { action(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}
