package com.example.pillmate.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.example.pillmate.MainActivity
import com.example.pillmate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpOptionsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Bộ khởi chạy cho Google Sign In (Thay thế cho startActivityForResult cũ)
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Đăng nhập Google thành công, lấy tài khoản ra
                val account = task.getResult(ApiException::class.java)
                // Tiến hành liên kết với Firebase Auth
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_options)

        // 1. Khởi tạo Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 2. Cấu hình Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // R.string.default_web_client_id sẽ tự động được tạo ra từ file google-services.json
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Setup WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- CÁC NÚT BẤM CHUYỂN MÀN HÌNH ---
        val btnContinueEmail = findViewById<Button>(R.id.buttonContinueWithEmail)
        btnContinueEmail.setOnClickListener {
            startActivity(Intent(this, SignUpWithEmailActivity::class.java))
        }

        val txtHaveAccount = findViewById<TextView>(R.id.textViewHaveAnAccount)
        txtHaveAccount.setOnClickListener {
            startActivity(Intent(this, SignInWithEmailActivity::class.java))
        }

        // --- NÚT ĐĂNG NHẬP BẰNG GOOGLE ---
        val btnContinueGoogle = findViewById<Button>(R.id.buttonContinueWithGoogle)
        btnContinueGoogle.setOnClickListener {
            // Mở hộp thoại chọn tài khoản Google
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        setupSignInHyperlink()
    }

    // Hàm phụ xử lý liên kết token Google với Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Đăng nhập thành công -> Chuyển vào MainActivity
                    Toast.makeText(this, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupSignInHyperlink() {
        val tvHaveAccount = findViewById<TextView>(R.id.textViewHaveAnAccount)
        val fullText = "Have an account? Sign in"
        val spannableString = SpannableString(fullText)

        // Tạo một ClickableSpan để xử lý sự kiện click và đổi màu chữ
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Chuyển hướng sang màn hình Đăng nhập
                val intent = Intent(this@SignUpOptionsActivity, SignInWithEmailActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                // Đổi màu chữ thành xanh lá (bạn có thể đổi mã màu hex khác nếu muốn)
                ds.color = Color.parseColor("#878CEDFF")
                // Bật gạch chân (hyperlink)
                ds.isUnderlineText = true
                // In đậm chữ (tùy chọn)
                ds.isFakeBoldText = true
            }
        }

        // Đếm vị trí chữ: "Have an account? " dài 17 ký tự.
        // Chữ "Sign in" bắt đầu từ vị trí 17 đến 24.
        spannableString.setSpan(clickableSpan, 17, 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Đưa chuỗi đã xử lý vào TextView
        tvHaveAccount.text = spannableString

        // BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ BẤM ĐƯỢC
        tvHaveAccount.movementMethod = LinkMovementMethod.getInstance()

        // (Tùy chọn) Xóa màu nền mặc định hơi xấu của Android khi người dùng chạm vào text
        tvHaveAccount.highlightColor = Color.TRANSPARENT
    }
}