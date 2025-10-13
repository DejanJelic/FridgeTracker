package com.example.fridgetracker.view.barcode
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class BarcodeScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BarcodeScannerScreen { code ->
                onBarcodeDetected(code)
            }
        }
    }

    private fun onBarcodeDetected(code: String) {
        Toast.makeText(this, "Scanned: $code", Toast.LENGTH_SHORT).show()

        val resultIntent = Intent().apply {
            putExtra("scanned_barcode", code)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()

        // If instead of returning the results you want to do a DB lookup,
        // use lifecycleScope and coroutines (example):
        // lifecycleScope.launch {
        //     val product = withContext(Dispatchers.IO) { /* vm.findByBarcode(code) */ }
        //     // navigacija ili otvaranje detalja
        // }
    }
}
