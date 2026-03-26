package com.example.pillmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pillmate.authentication.SignUpOptionsActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Giả sử bạn có một nút Sign Out trong activity_main.xml có ID là buttonSignOut
        // Nếu ID của bạn khác, hãy đổi lại cho khớp nhé
        val btnSignOut = findViewById<Button>(R.id.buttonSignOut)
        btnSignOut?.setOnClickListener {
            performSignOut()
        }
    }

    // Hàm này tự động chạy mỗi khi MainActivity xuất hiện trên màn hình
    override fun onStart() {
        super.onStart()
        // Kiểm tra xem người dùng hiện tại có tồn tại không
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Nếu bằng null (đã đăng xuất), lập tức chuyển về màn hình đăng nhập
            val intent = Intent(this, SignUpOptionsActivity::class.java)
            // Xóa sạch lịch sử các màn hình trước đó để không thể bấm nút Back quay lại đây
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun performSignOut() {
        // 1. Đăng xuất khỏi Firebase Auth
        auth.signOut()

        // 2. Đăng xuất khỏi hệ thống Google Sign-In (Xóa cache)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // 3. Sau khi thoát Google thành công, chuyển về màn hình Login
            val intent = Intent(this, SignUpOptionsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}