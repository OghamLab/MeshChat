
package com.oghamlab.meshchat.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix

@Composable
fun NostrQrCode(
    npub: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(npub) {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2
        )

        val matrix: BitMatrix =
            MultiFormatWriter().encode(
                "nostr:$npub",
                BarcodeFormat.QR_CODE,
                512,
                512,
                hints
            )

        Bitmap.createBitmap(
            matrix.width,
            matrix.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    setPixel(
                        x,
                        y,
                        if (matrix[x, y]) {
                            android.graphics.Color.BLACK
                        } else {
                            android.graphics.Color.WHITE
                        }
                    )
                }
            }
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Nostr public key QR code",
        modifier = modifier
            .background(Color.White)
            .padding(12.dp)
    )
}
