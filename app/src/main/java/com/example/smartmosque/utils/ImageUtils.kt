package com.example.smartmosque.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    fun compressImage(context: Context, imageUri: Uri): File? {
        return try {
            // 1. Buka Stream dari URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 2. Tentukan ukuran maksimal (misal lebar 1024px)
            val maxWidth = 1024
            val maxHeight = 1024
            var width = originalBitmap.width
            var height = originalBitmap.height

            // 3. Hitung rasio resize jika gambar terlalu besar
            if (width > maxWidth || height > maxHeight) {
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

                var finalWidth = maxWidth
                var finalHeight = maxHeight

                if (ratioMax > ratioBitmap) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }

                width = finalWidth
                height = finalHeight
            }

            // 4. Resize Bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            // 5. Simpan ke File Sementara (Cache)
            val file = File(context.cacheDir, "upload_bukti_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            // 6. Kompres ke JPEG dengan kualitas 70% (Cukup untuk bukti transfer)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            outputStream.flush()
            outputStream.close()

            file // Kembalikan file yang sudah dikompres
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}