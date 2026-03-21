package com.example.pillmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.pillmate.authentication.SignUpOptionsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Assuming you add a button with the ID 'buttonSignOut' in your activity_main.xml
        val btnSignOut = findViewById<Button>(R.id.buttonSignOut)

        btnSignOut.setOnClickListener {
            // 1. Đăng xuất khỏi Firebase
            FirebaseAuth.getInstance().signOut()

            // 2. Đăng xuất khỏi Google Sign-In (Bước khóa chốt)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)

            googleSignInClient.signOut().addOnCompleteListener {
                // 3. Sau khi Google đã thoát hẳn, mới chuyển hướng về màn hình ngoài
                val intent = Intent(this, SignUpOptionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}