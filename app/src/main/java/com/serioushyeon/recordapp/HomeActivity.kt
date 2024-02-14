package com.serioushyeon.recordapp
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.serioushyeon.recordapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityHomeBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.recordVideoBtn.setOnClickListener {
            val intent = Intent(this, RecordVideoActivity::class.java)
            startActivity(intent)
        }
        viewBinding.recordSoundBtn.setOnClickListener {
            val intent = Intent(this, RecordVoiceActivity::class.java)
            startActivity(intent)
        }
        viewBinding.logout.setOnClickListener {
            // SharedPreferences 인스턴스 가져오기
            val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // 저장된 데이터 삭제
            editor.clear()

            editor.apply() // 변경사항 적용
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}