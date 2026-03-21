package com.example.pillmate.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pillmate.MainActivity
import com.example.pillmate.R
import com.example.pillmate.dataconnect.PillmateConnector
import com.example.pillmate.dataconnect.createUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.dataconnect.FirebaseDataConnect
import kotlinx.coroutines.launch

class SignUpWithEmailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_with_email)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etFullname = findViewById<EditText>(R.id.editTextFullname)
        val etEmail = findViewById<EditText>(R.id.editTextEmail)
        val etPassword = findViewById<EditText>(R.id.editTextPassword)
        val btnSignUp = findViewById<Button>(R.id.buttonSignUp)

        btnSignUp.setOnClickListener {
            val fullname = etFullname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Basic Validation
            if (fullname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create User in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        // LẤY UID TỪ USER Ở ĐÂY
                        val uid = user?.uid ?: return@addOnCompleteListener

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullname)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            // DÙNG LIFECYCLESCOPE ĐỂ CHẠY DATA CONNECT
                            lifecycleScope.launch {
                                try {
                                    val dataConnect = FirebaseDataConnect.getInstance(PillmateConnector.instance)
                                    // Hàm execute() giờ đã được chạy an toàn trong Coroutine
                                    dataConnect.createUserProfile(accountId = uid, fullName = fullname).execute()

                                    Toast.makeText(this@SignUpWithEmailActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this@SignUpWithEmailActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)

                                } catch (e: Exception) {
                                    Toast.makeText(this@SignUpWithEmailActivity, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}