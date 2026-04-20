package com.example.made.data.repository

import android.content.Context
import android.net.Uri
import com.example.made.data.remote.SupabaseConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DocumentVaultRepository {

    companion object {
        const val BUCKET = "tenant-documents"
    }

    private val client = OkHttpClient()

    suspend fun uploadTenantDocument(
        context: Context,
        token: String,
        userId: String,
        tenantId: String,
        type: String,
        fileUri: Uri
    ): Result<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("Unable to read file"))
            val ext = guessExtension(context, fileUri)
            val path = "$userId/$tenantId/${type}_${System.currentTimeMillis()}.$ext"
            val mime = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

            val req = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}storage/v1/object/$BUCKET/$path")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("x-upsert", "true")
                .post(bytes.toRequestBody(mime.toMediaType()))
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) Result.success(path)
            else Result.failure(IllegalStateException("Upload failed: ${res.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSignedUrl(token: String, path: String, expiresInSec: Int = 86400): Result<String> {
        return try {
            val body = JSONObject().put("expiresIn", expiresInSec).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}storage/v1/object/sign/$BUCKET/$path")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()
            val res = client.newCall(req).execute()
            val json = res.body?.string().orEmpty()
            if (!res.isSuccessful) return Result.failure(IllegalStateException("Sign failed: ${res.code}"))
            val obj = JSONObject(json)
            val raw = obj.optString("signedURL", obj.optString("signedUrl", ""))
            if (raw.isBlank()) return Result.failure(IllegalStateException("No signed URL"))
            val fullUrl = if (raw.startsWith("http")) raw else "${SupabaseConfig.BASE_URL.trimEnd('/')}$raw"
            Result.success(fullUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri).orEmpty()
        return when {
            type.contains("pdf") -> "pdf"
            type.contains("jpeg") || type.contains("jpg") -> "jpg"
            type.contains("png") -> "png"
            else -> "bin"
        }
    }
}
