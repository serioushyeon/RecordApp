package com.serioushyeon.recordapp

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale


class RecordVoiceActivity : AppCompatActivity() {
    var file: File? = null
    var filename: String? = null

    private val soundVisualizerView: SoundVisualizerView by lazy {
        findViewById(R.id.soundVisualizerView)
    }

    private val recordTimeTextView: CountUpView by lazy {
        findViewById(R.id.recordTimeTextView)
    }

    private val resetButton: Button by lazy {
        findViewById(R.id.resetButton)
    }

    private val recordButton: RecordButton by lazy {
        findViewById(R.id.recordVoiceButton)
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val recordingFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/recording.3gp"
    }
    private var state = State.BEFORE_RECORDING
        set(value) { // setter 설정
            field = value // 실제 프로퍼티에 대입
            resetButton.isEnabled = (value == State.AFTER_RECORDING || value == State.ON_PLAYING)
            recordButton.updateIconWithState(value)
        }

    private var recorder: MediaRecorder? = null // 사용 하지 않을 때는 메모리해제 및  null 처리
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_voice)

        requestAudioPermission()
        initViews()
        bindViews()
        initVariables()

        file = getOutputFile();
        if (file != null) {
            filename = file!!.absolutePath;
        }
    }
    private fun getOutputFile(): File? {
        var mediaFile: File? = null
        try {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "RecordAppAudio"
            )
            println("asd mediaStorageDir : $mediaStorageDir")
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
            val name = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA)
                .format(System.currentTimeMillis())
            mediaFile = File(mediaStorageDir.path + File.separator + "${name}.mp4")
            println("asd mediaFile : $mediaFile")
            filename = mediaStorageDir.path + File.separator + "${System.currentTimeMillis()}.mp4"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaFile
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 요청한 권한에 대한 결과

        val audioRecordPermissionGranted =
            requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                    grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (!audioRecordPermissionGranted) {
            finish() // 거절 할 경우 앱 종료
        }
    }

    private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun initViews() {
        recordButton.updateIconWithState(state)
    }

    private fun bindViews() {

        soundVisualizerView.onRequestCurrentAmplitude = {
            recorder?.maxAmplitude ?: 0
        }


        recordButton.setOnClickListener {
            when (state) {
                State.BEFORE_RECORDING -> {
                    startRecoding()
                }
                State.ON_RECORDING -> {
                    stopRecording()
                }
                State.AFTER_RECORDING -> {
                    startPlaying()
                }
                State.ON_PLAYING -> {
                    stopPlaying()
                }
            }
        }

        resetButton.setOnClickListener {
            stopPlaying()
            // clear
            soundVisualizerView.clearVisualization()
            recordTimeTextView.clearCountTime()
            state = State.BEFORE_RECORDING
        }
    }

    private fun initVariables() {
        state = State.BEFORE_RECORDING
    }

    private fun startRecoding() {
        // 녹음 시작 시 초기화
        recorder = MediaRecorder()
            .apply {
                setAudioSource(MediaRecorder.AudioSource.MIC);
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                setOutputFile(filename);
                prepare()
            }
        recorder?.start()
        recordTimeTextView.startCountup()
        soundVisualizerView.startVisualizing(false)
        state = State.ON_RECORDING
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null

        val values = ContentValues(10)

        values.put(MediaStore.MediaColumns.TITLE, "Recorded")
        values.put(MediaStore.Audio.Media.ALBUM, "Audio Album")
        values.put(MediaStore.Audio.Media.ARTIST, "Mike")
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Recorded Audio")
        values.put(MediaStore.Audio.Media.IS_RINGTONE, 1)
        values.put(MediaStore.Audio.Media.IS_MUSIC, 1)
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
        values.put(MediaStore.Audio.Media.DATA, filename)

        val audioUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        filename?.let { uploadAudioToServerWithToken(it) }
        if (audioUri == null) {
            Log.d("SampleAudioRecorder", "Audio insert failed.");
        }

        Toast.makeText(this, filename,Toast.LENGTH_LONG ).show()
        soundVisualizerView.stopVisualizing()
        recordTimeTextView.stopCountup()
        state = State.AFTER_RECORDING
    }

    private fun startPlaying() {
        // MediaPlayer
        player = MediaPlayer()
            .apply {
                setDataSource(recordingFilePath)
                prepare() // 재생 할 수 있는 상태 (큰 파일 또는 네트워크로 가져올 때는 prepareAsync() )
            }

        // 전부 재생 했을 때
        player?.setOnCompletionListener {
            stopPlaying()
            state = State.AFTER_RECORDING
        }

        player?.start() // 재생
        recordTimeTextView.startCountup()

        soundVisualizerView.startVisualizing(true)

        state = State.ON_PLAYING
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        soundVisualizerView.stopVisualizing()
        recordTimeTextView.stopCountup()

        state = State.AFTER_RECORDING
    }
    private fun uploadAudioToServerWithToken(audioFilePath: String) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwtToken", null)
        val userId = sharedPreferences.getString("userId", null)
        val file = File(audioFilePath)
        val fileRequestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", file.name, fileRequestBody)
            .addFormDataPart("id_receive", userId!!)
            .build()

        val request = Request.Builder()
            .url("${BaseUrl.BASE_URL}/api/audioUpload") // 실제 서버 엔드포인트 URL로 교체하세요
            .post(multipartBody)
            .addHeader("Authorization", "Bearer $jwtToken") // JWT 토큰을 헤더에 추가
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 메인 스레드에서 UI 업데이트를 실행하기 위해 runOnUiThread 사용
                runOnUiThread {
                    Toast.makeText(applicationContext, "Audio upload failed: $e", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Audio uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Audio upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}