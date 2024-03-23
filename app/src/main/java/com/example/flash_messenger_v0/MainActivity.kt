package com.example.flash_messenger_v0

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import java.io.IOException
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val INTERNET_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        // Initialisation des boutons
        val btnFetch: Button = findViewById(R.id.btnFetch)
        val btnClear: Button = findViewById(R.id.btnClear)

        btnFetch.setOnClickListener {
            // Vérifier si la permission INTERNET est accordée
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
                // Demander la permission INTERNET
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.INTERNET),
                    INTERNET_PERMISSION_REQUEST_CODE)
            } else {
                // La permission est déjà accordée
                // on lance fetchIPAddress
                fetchIPAddress()
            }
        }

        // On efface l'écran
        btnClear.setOnClickListener {
            textView.text = "Your IP Address will appear here"
        }
    }

    private fun fetchIPAddress() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.myip.com/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    textView.text = "Failed to fetch IP address"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)

                    var ipAddress = jsonObject.optString("ip")
                    val country = jsonObject.optString("country")
                    val countryCode = jsonObject.optString("cc")

                    // Masquer les deux derniers octets de l'adresse IP
                    val ipParts = ipAddress.split(".")
                    if (ipParts.size == 4) {
                        ipAddress = "${ipParts[0]}.${ipParts[1]}.XXX.XXX"
                    }
                    // Supprimer les deux derniers octets de l'adresse IP de la réponse JSON brute
                    val sanitizedResponse = responseData?.replace(ipParts[2] + "." + ipParts[3], "XXX.XXX")

                    val delimiter = "-----------------------------------------\n"

                    runOnUiThread {
                        textView.text = buildString {
                            append("IP Address: $ipAddress (masqued)\n")
                            append("Country: $country\n")
                            append("Country Code: $countryCode\n")
                            append(delimiter)
                            append("Full JSON response:\n")
                            append(sanitizedResponse) // Afficher la réponse JSON brute avec l'IP masquée
                            append("\n")
                            append(delimiter)
                            append("Application Info:\n")
                            append("App Name: ${applicationInfo.name}\n")
                            append("Package Name: ${applicationInfo.packageName}\n")
                            append("SDK Version: ${android.os.Build.VERSION.SDK_INT}\n")
                            append("Device Model: ${android.os.Build.MODEL}\n")
                            append("Device Manufacturer: ${android.os.Build.MANUFACTURER}\n")
                            append("System Language: ${Locale.getDefault().language}\n")
                        }
                    }
                }
            }
        })
    }

    // Gérer la réponse de demande de permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            INTERNET_PERMISSION_REQUEST_CODE -> {
                // Vérifier si la permission a été accordée
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // La permission INTERNET a été accordée
                    // on lance fetchIPAddress
                    fetchIPAddress()
                } else {
                    // La permission INTERNET a été refusée
                    // on informe l'utilisateur.
                    Toast.makeText(this, "Internet access is required to use this app", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}
