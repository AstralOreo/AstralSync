package net.astral.astralsync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private val PICK_FILES_REQUEST_CODE = 1
    private val HYDRUS_API_KEY = "becca96deaac42012b45bf2ea5ea715e12dd8262211a4ed3fa64ba0b2c37ab87"
    private val HYDRUS_URL = "http://192.168.1.7:45869/add_files/add_file"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectFilesButton = findViewById<Button>(R.id.selectFilesButton)
        selectFilesButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_FILES_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILES_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val clipData = data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val fileUri = clipData.getItemAt(i).uri
                    uploadFile(fileUri)
                }
            } else {
                data?.data?.let { uploadFile(it) }
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)

        if (inputStream == null) {
            runOnUiThread {
                Toast.makeText(this, "Error while opening the file", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val fileBytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = fileBytes.toRequestBody("application/octet-stream".toMediaType())

        val request = Request.Builder()
            .url(HYDRUS_URL)
            .post(requestBody)
            .addHeader("Hydrus-Client-API-Access-Key", HYDRUS_API_KEY)
            .build()


        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "No response"
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "File successfully uploaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "HTTP Error ${response.code}: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
