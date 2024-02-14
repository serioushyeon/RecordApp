package com.serioushyeon.recordapp


import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.serioushyeon.recordapp.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwtToken", null)

        if (jwtToken != null) {
            println(jwtToken)
            // 토큰이 존재하는 경우, 토큰 유효성 검증을 요청하거나
            validateTokenAndProceed(jwtToken)
            // 바로 메인 화면으로 이동 등의 로그인 처리를 수행
            //val intent = Intent(this, HomeActivity::class.java)
            //startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            val status: Int = NetworkStatus.getConnectivityStatus(applicationContext)
            if (status == NetworkStatus.TYPE_MOBILE || status == NetworkStatus.TYPE_WIFI) {
                // get방식 파라미터 추가
                val urlBuilder = "${BaseUrl.BASE_URL}api/login".toHttpUrlOrNull()!!.newBuilder()
                val url = urlBuilder.build().toString()

                val jsonInput = JSONObject()
                jsonInput.put("id_receive", binding.editTextId.text.toString().trim { it <= ' ' })
                jsonInput.put(
                    "pw_receive",
                    binding.editTextPassword.text.toString().trim { it <= ' ' })

                val reqBody = RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    jsonInput.toString()
                )

                // 요청 만들기
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .build()

                // 응답 콜백
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            try {
                                if (response.isSuccessful) {
                                    val message: String = response.body!!.string()
                                    val jObject = JSONObject(message)
                                    val result = jObject.getString("result")
                                    if (result == "200") {
                                        val jwtToken = jObject.getString("token")
                                        val sharedPreferences =
                                            getSharedPreferences(
                                                "MyAppPreferences",
                                                Context.MODE_PRIVATE
                                            )
                                        sharedPreferences.edit().putString("jwtToken", jwtToken).apply()
                                        sharedPreferences.edit().putString("userId", binding.editTextId.text.toString().trim { it <= ' ' })
                                            .apply()
                                        val intent = Intent(applicationContext, HomeActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(applicationContext, "오류 발생", Toast.LENGTH_LONG).show()
                                    }
                                }else {
                                    val message: String = response.body!!.string()
                                    val jObject = JSONObject(message)
                                    val msg = jObject.getString("msg")
                                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
            } else {
                Toast.makeText(applicationContext, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
    private fun validateTokenAndProceed(jwtToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BaseUrl.BASE_URL}token/validate") // baseUrl과 엔드포인트 조합
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // 메인 스레드에서 UI 작업을 실행하기 위해 runOnUiThread 사용
                runOnUiThread {
                    if (response.isSuccessful) {
                        if (response.code == 200) {
                            // 토큰이 유효한 경우, 메인 액티비티로 이동
                            val intent = Intent(applicationContext, HomeActivity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        // 토큰 검증 실패 시 메시지 표시
                        Toast.makeText(applicationContext, "토큰 검증 실패. 재로그인 해주세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // 네트워크 오류 처리
                runOnUiThread {
                    Toast.makeText(applicationContext, "네트워크 오류 발생", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

}