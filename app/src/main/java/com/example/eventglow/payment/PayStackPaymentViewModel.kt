package com.example.eventglow.payment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.eventglow.dataClass.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PayStackPaymentViewModel(application: Application) : AndroidViewModel(application) {

    // MutableStateFlow variables for authorization and verification results
    private val _authorizationResult = MutableStateFlow<AuthorizationResult>(AuthorizationResult.Idle)
    val authorizationResult: StateFlow<AuthorizationResult> = _authorizationResult

    private val _verificationResult = MutableStateFlow<VerificationResult>(VerificationResult.Idle)
    val verificationResult: StateFlow<VerificationResult> = _verificationResult

    // Transaction reference
    var transactionReference: String? = null

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    suspend fun initiatePayment(email: String, amount: String) {
        return withContext(Dispatchers.IO) {
            val url = "https://api.paystack.co/transaction/initialize"
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("email", email)
                .add("amount", amount)
                .add("currency", "GHS") // Assuming GHS for Ghana
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer sk_test_9391f4990ba13feeb62f9b0ae25ec9d6dd72ebe1")
                .build()

            try {
                Log.d("PayStackPaymentViewModel", "Initiating payment with email: $email and amount: $amount")
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonObject = Json.parseToJsonElement(responseBody ?: "").jsonObject
                        val authorizationUrl =
                            jsonObject["data"]?.jsonObject?.get("authorization_url")?.jsonPrimitive?.content
                        transactionReference = jsonObject["data"]?.jsonObject?.get("reference")?.jsonPrimitive?.content

                        Log.d("PayStackPaymentViewModel", "Authorization URL: $authorizationUrl")
                        Log.d("PayStackPaymentViewModel", "Transaction Reference: $transactionReference")

                        authorizationUrl?.let { url ->
                            _authorizationResult.value = AuthorizationResult.Success(url)
                            Log.d("PayStackPaymentViewModel", "Authorization result set to success with URL: $url")
                        }
                    } else {
                        Log.e(
                            "PayStackPaymentViewModel",
                            "Failed to fetch authorization URL. Response code: ${response.code}"
                        )
                        _authorizationResult.value =
                            AuthorizationResult.Error("Failed to fetch authorization URL. Response code: ${response.code}")
                        Log.d(
                            "PayStackPaymentViewModel",
                            "Authorization result set to error: Failed to fetch authorization URL. Response code: ${response.code}"
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e("PayStackPaymentViewModel", "Network error: ${e.message}")
                _authorizationResult.value = AuthorizationResult.Error("Network error: ${e.message}")
                Log.d("PayStackPaymentViewModel", "Authorization result set to error: Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("PayStackPaymentViewModel", "Error fetching authorization URL: ${e.message}")
                _authorizationResult.value = AuthorizationResult.Error("Error fetching authorization URL: ${e.message}")
                Log.d(
                    "PayStackPaymentViewModel",
                    "Authorization result set to error: Error fetching authorization URL: ${e.message}"
                )
            }
        }
    }

    fun resetAuthorizationResult() {
        Log.d("PayStackPaymentViewModel", "Resetting authorization result")
        _authorizationResult.value = AuthorizationResult.Idle
    }

    fun saveTransactionToFirestore(transaction: Transaction) {
        Log.d("PayStackPaymentViewModel", "Saving transaction to Firestore: $transaction")

        val transactionData = hashMapOf(
            "id" to transaction.id,
            "status" to transaction.status,
            "reference" to transaction.reference,
            "amount" to transaction.amount,
            "gatewayResponse" to transaction.gatewayResponse,
            "paidAt" to transaction.paidAt,
            "createdAt" to transaction.createdAt,
            "channel" to transaction.channel,
            "currency" to transaction.currency,
            "authorizationCode" to transaction.authorizationCode,
            "cardType" to transaction.cardType,
            "bank" to transaction.bank,
            "customerEmail" to transaction.customerEmail,
            "customerCode" to transaction.customerCode,
            "customerPhone" to transaction.customerPhoneNumber
        )

        db.collection("transactions")
            .add(transactionData)
            .addOnSuccessListener {
                Log.d("PayStackPaymentViewModel", "Transaction saved successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("PayStackPaymentViewModel", "Failed to save transaction: ${exception.message}")
            }
    }

    suspend fun verifyTransaction() {
        _verificationResult.value = VerificationResult.Loading
        Log.d("PayStackPaymentViewModel", "Verifying transaction with reference: $transactionReference")
        return withContext(Dispatchers.IO) {
            val url = "https://api.paystack.co/transaction/verify/$transactionReference"
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer sk_test_9391f4990ba13feeb62f9b0ae25ec9d6dd72ebe1")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonObject = Json.parseToJsonElement(responseBody ?: "").jsonObject
                        val status = jsonObject["status"]?.jsonPrimitive?.boolean ?: false
                        val data = jsonObject["data"]?.jsonObject

                        Log.d("PayStackPaymentViewModel", "Transaction verification jsonObject: $jsonObject")

                        if (status && data != null) {
                            val id = data["id"]?.jsonPrimitive?.jsonPrimitive?.content ?: ""
                            val transactionStatus = data["status"]?.jsonPrimitive?.content ?: ""
                            val reference = data["reference"]?.jsonPrimitive?.content ?: ""
                            val amount = data["amount"]?.jsonPrimitive?.content ?: ""
                            val gatewayResponse = data["gateway_response"]?.jsonPrimitive?.content ?: ""
                            val paidAt = data["paid_at"]?.jsonPrimitive?.content ?: ""
                            val createdAt = data["created_at"]?.jsonPrimitive?.content ?: ""
                            val channel = data["channel"]?.jsonPrimitive?.content ?: ""
                            val currency = data["currency"]?.jsonPrimitive?.content ?: ""
                            val authorizationCode =
                                data["authorization"]?.jsonObject?.get("authorization_code")?.jsonPrimitive?.content
                            val cardType = data["authorization"]?.jsonObject?.get("card_type")?.jsonPrimitive?.content
                            val bank = data["authorization"]?.jsonObject?.get("bank")?.jsonPrimitive?.content
                            val customerEmail = data["customer"]?.jsonObject?.get("email")?.jsonPrimitive?.content ?: ""
                            val customerCode =
                                data["customer"]?.jsonObject?.get("customer_code")?.jsonPrimitive?.content ?: ""
                            val customerPhone = data["customer"]?.jsonObject?.get("phone")?.jsonPrimitive?.content ?: ""

                            val transaction = Transaction(
                                id = id,
                                status = transactionStatus,
                                reference = reference,
                                amount = amount,
                                gatewayResponse = gatewayResponse,
                                paidAt = paidAt,
                                createdAt = createdAt,
                                channel = channel,
                                currency = currency,
                                authorizationCode = authorizationCode,
                                cardType = cardType,
                                bank = bank,
                                customerEmail = customerEmail,
                                customerCode = customerCode,
                                customerPhoneNumber = customerPhone
                            )

                            try {
                                saveTransactionToFirestore(transaction)
                                Log.d("PaymentViewModel", "Transaction details saved to Firestore")
                            } catch (e: Exception) {
                                Log.d(
                                    "PaymentViewModel",
                                    "Could not save transaction details to firestore ${e.message}"
                                )
                            }

                            Log.d("Paystack viewModel: ", " Transaction status: $transactionStatus")
                            if (transactionStatus == "success") {
                                _verificationResult.value = VerificationResult.Success
                                Log.d("PayStackPaymentViewModel", "Verification result set to success")
                            } else {
                                _verificationResult.value = VerificationResult.Error("Payment was unsuccessful")
                                Log.d(
                                    "PayStackPaymentViewModel",
                                    "Verification result set to error: Payment was unsuccessful"
                                )
                            }
                        } else {
                            Log.e("PayStackPaymentViewModel", "Transaction verification failed. Status: $status")
                            withContext(Dispatchers.Main) {
                                _verificationResult.value =
                                    VerificationResult.Error("Transaction verification failed. Status: $status")
                                Log.d(
                                    "PayStackPaymentViewModel",
                                    "Verification result set to error: Transaction verification failed. Status: $status"
                                )
                            }
                        }
                    } else {
                        Log.e(
                            "PayStackPaymentViewModel",
                            "Failed to fetch verification details. Response code: ${response.code}"
                        )
                        withContext(Dispatchers.Main) {
                            _verificationResult.value =
                                VerificationResult.Error("Failed to fetch verification details. Response code: ${response.code}")
                            Log.d(
                                "PayStackPaymentViewModel",
                                "Verification result set to error: Failed to fetch verification details. Response code: ${response.code}"
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("PayStackPaymentViewModel", "Network error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _verificationResult.value = VerificationResult.Error("Network error: ${e.message}")
                    Log.d("PayStackPaymentViewModel", "Verification result set to error: Network error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("PayStackPaymentViewModel", "Error fetching verification details: ${e.message}")
                withContext(Dispatchers.Main) {
                    _verificationResult.value =
                        VerificationResult.Error("Error fetching verification details: ${e.message}")
                    Log.d(
                        "PayStackPaymentViewModel",
                        "Verification result set to error: Error fetching verification details: ${e.message}"
                    )
                }
            }
        }
    }
}

sealed class VerificationResult {
    data object Success : VerificationResult()
    data class Error(val message: String) : VerificationResult()
    data object Idle : VerificationResult()
    data object Loading : VerificationResult()
}

sealed class AuthorizationResult {
    data class Success(val authorizationUrl: String) : AuthorizationResult()
    data class Error(val message: String) : AuthorizationResult()
    data object Idle : AuthorizationResult()
}