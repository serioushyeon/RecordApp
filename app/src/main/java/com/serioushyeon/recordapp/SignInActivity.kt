package com.serioushyeon.recordapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.serioushyeon.recordapp.databinding.ActivitySignInBinding
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


class SignInActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivitySignInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnRegisterReg.setOnClickListener {
            val status: Int = NetworkStatus.getConnectivityStatus(applicationContext)
            if (status == NetworkStatus.TYPE_MOBILE || status == NetworkStatus.TYPE_WIFI) {

                // EditText값 예외처리
                if (viewBinding.editTextIdReg.text.toString().trim { it <= ' ' }.isNotEmpty()
                    || viewBinding.editTextPassReg.text.toString().trim { it <= ' ' }.isNotEmpty()
                    || viewBinding.editTextRePassReg.text.toString().trim { it <= ' ' }.isNotEmpty()
                ) {

                    // get방식 파라미터 추가
                    val urlBuilder = "http://10.0.2.2:5000/api/signup".toHttpUrlOrNull()!!.newBuilder()
                    //urlBuilder.addQueryParameter("v", "1.0") // 예시
                    val url = urlBuilder.build().toString()

                    val jsonInput = JSONObject()
                    jsonInput.put("id_receive", viewBinding.editTextIdReg.text.toString().trim { it <= ' ' })
                    jsonInput.put("pw_receive", viewBinding.editTextPassReg.text.toString().trim { it <= ' ' })
                    jsonInput.put("re_password_receive", viewBinding.editTextRePassReg.text.toString().trim { it <= ' ' })

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
                                try {
                                    if (!response.isSuccessful) {
                                        // 응답 실패
                                        Log.i("tag", "응답실패")
                                        runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                "네트워크 문제 발생 ${response.isSuccessful}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Log.i("tag", "응답 성공")
                                        val message: String = response.body!!.string()
                                        val jObject = JSONObject(message)
                                        val resp = jObject.getString("result")
                                        if (resp == "success") {
                                            runOnUiThread {
                                                val intent =
                                                    Intent(
                                                        applicationContext,
                                                        MainActivity::class.java
                                                    )
                                                startActivity(intent)
                                            }
                                        } else {
                                            runOnUiThread {
                                                Toast.makeText(
                                                    applicationContext,
                                                    "회원가입에 실패 했습니다.$resp",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                        }
                    })
                }
            } else {
                runOnUiThread {
                    Toast.makeText(applicationContext, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

}