package com.example.eventglow.payment

import android.app.Application
import android.util.Log
import com.example.eventglow.BuildConfig
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.Transaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt

class PayStackPaymentViewModel(application: Application) : BaseViewModel(application) {

    // MutableStateFlow variables for authorization and verification results
    private val _authorizationResult = MutableStateFlow<AuthorizationResult>(AuthorizationResult.Idle)
    val authorizationResult: StateFlow<AuthorizationResult> = _authorizationResult

    private val _verificationResult = MutableStateFlow<VerificationResult>(VerificationResult.Idle)
    val verificationResult: StateFlow<VerificationResult> = _verificationResult
    private val _latestVerifiedTransaction = MutableStateFlow<Transaction?>(null)
    val latestVerifiedTransaction: StateFlow<Transaction?> = _latestVerifiedTransaction.asStateFlow()
    private val _purchaseFlowState = MutableStateFlow(PurchaseFlowState.IDLE)
    val purchaseFlowState: StateFlow<PurchaseFlowState> = _purchaseFlowState.asStateFlow()
    private val _pendingTransactionReference = MutableStateFlow("")
    val pendingTransactionReference: StateFlow<String> = _pendingTransactionReference.asStateFlow()
    private val _purchaseOutcome = MutableStateFlow<PurchaseOutcome>(PurchaseOutcome.Idle)
    val purchaseOutcome: StateFlow<PurchaseOutcome> = _purchaseOutcome.asStateFlow()

    // Transaction reference
    var transactionReference: String? = null
    private val auth = FirebaseAuth.getInstance()
    private var pendingPurchase: PendingPurchase? = null

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val supabaseBaseUrl = BuildConfig.SUPABASE_FUNCTIONS_BASE_URL.trimEnd('/')
    private val supabaseAnonKey = BuildConfig.SUPABASE_FUNCTIONS_ANON_KEY
    private val paystackInitPath = BuildConfig.SUPABASE_FUNCTIONS_PAYSTACK_INIT_PATH
    private val paystackVerifyPath = BuildConfig.SUPABASE_FUNCTIONS_PAYSTACK_VERIFY_PATH

    private fun functionUrl(path: String): String {
        if (supabaseBaseUrl.isBlank() || path.isBlank()) {
            throw IllegalStateException("Supabase function URL configuration is missing.")
        }
        return "$supabaseBaseUrl/${path.trimStart('/')}"
    }

    fun beginAuthorization() {
        _purchaseFlowState.value = PurchaseFlowState.AUTHORIZING
    }

    fun markAwaitingVerification(reference: String) {
        _pendingTransactionReference.value = reference
        _purchaseFlowState.value = PurchaseFlowState.AWAITING_VERIFICATION
    }

    fun beginVerification(reference: String? = null) {
        if (!reference.isNullOrBlank()) {
            transactionReference = reference
            _pendingTransactionReference.value = reference
        }
        _purchaseFlowState.value = PurchaseFlowState.VERIFYING
    }

    fun beginCommitting() {
        _purchaseFlowState.value = PurchaseFlowState.COMMITTING
    }

    fun markFailed(message: String? = null) {
        _purchaseFlowState.value = PurchaseFlowState.FAILED
        if (!message.isNullOrBlank()) setFailure(message)
    }

    fun resetPurchaseFlow() {
        _purchaseFlowState.value = PurchaseFlowState.IDLE
        _pendingTransactionReference.value = ""
        pendingPurchase = null
    }

    fun clearPurchaseOutcome() {
        _purchaseOutcome.value = PurchaseOutcome.Idle
    }

    suspend fun startTicketPurchase(
        event: Event,
        ticketType: TicketType,
        email: String,
        onCheckAvailability: suspend (String, TicketType) -> Boolean,
        onCommit: suspend (BoughtTicket, Transaction) -> Result<Unit>
    ) {
        clearPurchaseOutcome()

        val safeEmail = email.trim()
        if (safeEmail.isBlank() && !ticketType.isFree && ticketType.price > 0.0) {
            markFailed("No account email available for payment.")
            _purchaseOutcome.value = PurchaseOutcome.Failure(message = "No account email available for payment.")
            return
        }

        val available = onCheckAvailability(event.id, ticketType)
        if (!available) {
            markFailed("Ticket is sold out.")
            _purchaseOutcome.value = PurchaseOutcome.Failure(message = "Ticket is sold out.")
            return
        }

        val isFreeTicket = ticketType.isFree || ticketType.price <= 0.0
        if (isFreeTicket) {
            beginCommitting()
            val freeReference = "FREE-${UUID.randomUUID()}"
            val boughtTicket = buildBoughtTicket(
                event = event,
                ticketType = ticketType,
                ticketReference = freeReference,
                paymentTransaction = null,
                isFreeTicket = true
            )
            val freeTransaction = buildFreeTransaction(freeReference, boughtTicket)
            onCommit(boughtTicket, freeTransaction)
                .onSuccess {
                    resetPurchaseFlow()
                    _purchaseOutcome.value = PurchaseOutcome.Success(
                        reference = freeReference,
                        message = "Free ticket issued successfully."
                    )
                }
                .onFailure { error ->
                    markFailed(error.message ?: "Failed to issue free ticket.")
                    _purchaseOutcome.value = PurchaseOutcome.Failure(
                        reference = freeReference,
                        message = error.message ?: "Failed to issue free ticket."
                    )
                }
            return
        }

        pendingPurchase = PendingPurchase(event = event, ticketType = ticketType, email = safeEmail)
        beginAuthorization()
        initiatePayment(
            email = safeEmail,
            amount = (ticketType.price * 100).roundToInt().toString()
        )

        if (_authorizationResult.value is AuthorizationResult.Error) {
            val message = (_authorizationResult.value as AuthorizationResult.Error).message
            markFailed(message)
            _purchaseOutcome.value = PurchaseOutcome.Failure(message = message)
        }
    }

    suspend fun completePendingTicketPurchase(
        onCheckAvailability: suspend (String, TicketType) -> Boolean,
        onCommit: suspend (BoughtTicket, Transaction) -> Result<Unit>
    ) {
        clearPurchaseOutcome()
        val pending = pendingPurchase
        if (pending == null) {
            markFailed("No pending purchase found.")
            _purchaseOutcome.value = PurchaseOutcome.Failure(message = "No pending purchase found.")
            return
        }

        val pendingRef = transactionReference?.trim().orEmpty().ifBlank {
            _pendingTransactionReference.value.trim()
        }
        if (pendingRef.isBlank()) {
            markFailed("Missing transaction reference.")
            _purchaseOutcome.value = PurchaseOutcome.Failure(message = "Missing transaction reference.")
            return
        }

        beginVerification(pendingRef)
        verifyTransaction()

        when (val state = _verificationResult.value) {
            is VerificationResult.Success -> {
                val transaction = _latestVerifiedTransaction.value?.copy(
                    reference = _latestVerifiedTransaction.value?.reference?.ifBlank { pendingRef } ?: pendingRef
                )
                if (transaction == null) {
                    markFailed("Missing verified transaction reference.")
                    _purchaseOutcome.value =
                        PurchaseOutcome.Failure(message = "Missing verified transaction reference.")
                    return
                }

                val available = onCheckAvailability(pending.event.id, pending.ticketType)
                if (!available) {
                    markFailed("Ticket is sold out. Verification succeeded but purchase was not completed.")
                    _purchaseOutcome.value = PurchaseOutcome.Failure(
                        reference = transaction.reference,
                        message = "Ticket is sold out. Verification succeeded but purchase was not completed."
                    )
                    return
                }

                beginCommitting()
                val boughtTicket = buildBoughtTicket(
                    event = pending.event,
                    ticketType = pending.ticketType,
                    ticketReference = transaction.reference,
                    paymentTransaction = transaction,
                    isFreeTicket = false
                )
                onCommit(boughtTicket, transaction)
                    .onSuccess {
                        resetPurchaseFlow()
                        _purchaseOutcome.value = PurchaseOutcome.Success(
                            reference = transaction.reference,
                            message = "Your ticket purchase is complete."
                        )
                    }
                    .onFailure { error ->
                        markFailed(error.message ?: "Failed to complete purchase.")
                        _purchaseOutcome.value = PurchaseOutcome.Failure(
                            reference = transaction.reference,
                            message = error.message ?: "Failed to complete purchase."
                        )
                    }
            }

            is VerificationResult.Error -> {
                markFailed(state.message)
                _purchaseOutcome.value = PurchaseOutcome.Failure(
                    reference = pendingRef,
                    message = state.message
                )
            }

            else -> {
                markFailed("Unable to verify payment.")
                _purchaseOutcome.value = PurchaseOutcome.Failure(
                    reference = pendingRef,
                    message = "Unable to verify payment."
                )
            }
        }
    }

    private fun supabasePostRequest(url: String, jsonBody: String): Request {
        return Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Content-Type", "application/json")
            .build()
    }

    suspend fun initiatePayment(email: String, amount: String) {
        setLoading()
        return withContext(Dispatchers.IO) {
            val initUrl = functionUrl(paystackInitPath)
            val payload = buildJsonObject {
                put("email", email)
                put("amount", amount)
                put("currency", "GHS")
            }.toString()
            val request = supabasePostRequest(initUrl, payload)

            try {
                Log.d("PayStackPaymentViewModel", "Initiating payment with email: $email and amount: $amount")
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val jsonObject = Json.parseToJsonElement(responseBody).jsonObject
                        val authorizationUrl = jsonObject["authorizationUrl"]?.jsonPrimitive?.contentOrNull
                            ?: jsonObject["data"]?.jsonObject?.get("authorization_url")?.jsonPrimitive?.contentOrNull
                        transactionReference = jsonObject["reference"]?.jsonPrimitive?.contentOrNull
                            ?: jsonObject["data"]?.jsonObject?.get("reference")?.jsonPrimitive?.contentOrNull

                        Log.d("PayStackPaymentViewModel", "Authorization URL: $authorizationUrl")
                        Log.d("PayStackPaymentViewModel", "Transaction Reference: $transactionReference")

                        authorizationUrl?.let { url ->
                            _authorizationResult.value = AuthorizationResult.Success(url)
                            if (!transactionReference.isNullOrBlank()) {
                                _pendingTransactionReference.value = transactionReference.orEmpty()
                            }
                            _purchaseFlowState.value = PurchaseFlowState.AWAITING_VERIFICATION
                            setSuccess()
                            Log.d("PayStackPaymentViewModel", "Authorization result set to success with URL: $url")
                        }
                    } else {
                        Log.e(
                            "PayStackPaymentViewModel",
                            "Failed to fetch authorization URL. Response code: ${response.code}; body=$responseBody"
                        )
                        _authorizationResult.value =
                            AuthorizationResult.Error(
                                "Failed to fetch authorization URL. Response code: ${response.code}. $responseBody"
                            )
                        _purchaseFlowState.value = PurchaseFlowState.FAILED
                        setFailure("Failed to fetch authorization URL. Response code: ${response.code}")
                        Log.d(
                            "PayStackPaymentViewModel",
                            "Authorization result set to error: Failed to fetch authorization URL. Response code: ${response.code}"
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e("PayStackPaymentViewModel", "Network error: ${e.message}")
                _authorizationResult.value = AuthorizationResult.Error("Network error: ${e.message}")
                _purchaseFlowState.value = PurchaseFlowState.FAILED
                setFailure("Network error: ${e.message}")
                Log.d("PayStackPaymentViewModel", "Authorization result set to error: Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("PayStackPaymentViewModel", "Error fetching authorization URL: ${e.message}")
                _authorizationResult.value = AuthorizationResult.Error("Error fetching authorization URL: ${e.message}")
                _purchaseFlowState.value = PurchaseFlowState.FAILED
                setFailure(e.message)
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
        if (_purchaseFlowState.value != PurchaseFlowState.COMMITTING &&
            _purchaseFlowState.value != PurchaseFlowState.AWAITING_VERIFICATION
        ) {
            _purchaseFlowState.value = PurchaseFlowState.IDLE
        }
    }

    suspend fun verifyTransaction() {
        setLoading()
        _verificationResult.value = VerificationResult.Loading
        _purchaseFlowState.value = PurchaseFlowState.VERIFYING
        Log.d("PayStackPaymentViewModel", "Verifying transaction with reference: $transactionReference")
        return withContext(Dispatchers.IO) {
            val reference = transactionReference?.trim().orEmpty()
            if (reference.isBlank()) {
                _verificationResult.value = VerificationResult.Error("Missing transaction reference.")
                _purchaseFlowState.value = PurchaseFlowState.FAILED
                setFailure("Missing transaction reference.")
                return@withContext
            }

            val verifyUrl = functionUrl(paystackVerifyPath)
            val payload = buildJsonObject { put("reference", reference) }.toString()
            val request = supabasePostRequest(verifyUrl, payload)

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val jsonObject = Json.parseToJsonElement(responseBody).jsonObject
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

                            Log.d("Paystack viewModel: ", " Transaction status: $transactionStatus")
                            if (transactionStatus == "success") {
                                _latestVerifiedTransaction.value = transaction
                                _verificationResult.value = VerificationResult.Success
                                _purchaseFlowState.value = PurchaseFlowState.COMMITTING
                                setSuccess()
                                Log.d("PayStackPaymentViewModel", "Verification result set to success")
                            } else {
                                _verificationResult.value = VerificationResult.Error("Payment was unsuccessful")
                                _purchaseFlowState.value = PurchaseFlowState.FAILED
                                setFailure("Payment was unsuccessful")
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
                                _purchaseFlowState.value = PurchaseFlowState.FAILED
                                setFailure("Transaction verification failed.")
                                Log.d(
                                    "PayStackPaymentViewModel",
                                    "Verification result set to error: Transaction verification failed. Status: $status"
                                )
                            }
                        }
                    } else {
                        Log.e(
                            "PayStackPaymentViewModel",
                            "Failed to fetch verification details. Response code: ${response.code}; body=$responseBody"
                        )
                        withContext(Dispatchers.Main) {
                            _verificationResult.value =
                                VerificationResult.Error(
                                    "Failed to fetch verification details. Response code: ${response.code}. $responseBody"
                                )
                            _purchaseFlowState.value = PurchaseFlowState.FAILED
                            setFailure("Failed to fetch verification details. Response code: ${response.code}")
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
                    _purchaseFlowState.value = PurchaseFlowState.FAILED
                    setFailure("Network error: ${e.message}")
                    Log.d("PayStackPaymentViewModel", "Verification result set to error: Network error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("PayStackPaymentViewModel", "Error fetching verification details: ${e.message}")
                withContext(Dispatchers.Main) {
                    _verificationResult.value =
                        VerificationResult.Error("Error fetching verification details: ${e.message}")
                    _purchaseFlowState.value = PurchaseFlowState.FAILED
                    setFailure(e.message)
                    Log.d(
                        "PayStackPaymentViewModel",
                        "Verification result set to error: Error fetching verification details: ${e.message}"
                    )
                }
            }
        }
    }

    private fun buildBoughtTicket(
        event: Event,
        ticketType: TicketType,
        ticketReference: String,
        paymentTransaction: Transaction?,
        isFreeTicket: Boolean
    ): BoughtTicket {
        val authUser = auth.currentUser
        val qrData = "eventglow_ticket|$ticketReference|${event.id}|${authUser?.uid.orEmpty()}"
        return BoughtTicket(
            transactionReference = ticketReference,
            paymentProvider = if (isFreeTicket) "free" else "paystack",
            paymentStatus = if (isFreeTicket) "success" else (paymentTransaction?.status ?: ""),
            paymentGatewayResponse = if (isFreeTicket) "Free ticket issued" else (paymentTransaction?.gatewayResponse
                ?: ""),
            paymentAmount = if (isFreeTicket) "0" else (paymentTransaction?.amount ?: ticketType.price.toString()),
            paymentCurrency = if (isFreeTicket) "GHS" else (paymentTransaction?.currency ?: "GHS"),
            paymentChannel = if (isFreeTicket) "free" else (paymentTransaction?.channel ?: ""),
            paymentAuthorizationCode = paymentTransaction?.authorizationCode ?: "",
            paymentCardType = paymentTransaction?.cardType ?: "",
            paymentBank = paymentTransaction?.bank ?: "",
            paymentCustomerEmail = paymentTransaction?.customerEmail ?: "",
            paymentPaidAt = paymentTransaction?.paidAt ?: "",
            paymentCreatedAt = paymentTransaction?.createdAt ?: "",
            isFreeTicket = isFreeTicket,
            eventOrganizer = event.eventOrganizer,
            eventId = event.id,
            eventName = event.eventName,
            startDate = event.startDate,
            eventStatus = event.eventStatus,
            endDate = event.endDate,
            imageUrl = event.imageUri,
            ticketName = ticketType.name,
            ticketPrice = ticketType.price.toString(),
            qrCodeData = qrData,
            isScanned = false,
            scannedAt = "",
            scannedByAdminId = "",
            scannedByAdminName = ""
        )
    }

    private fun buildFreeTransaction(
        ticketReference: String,
        boughtTicket: BoughtTicket
    ): Transaction {
        val authUser = auth.currentUser
        return Transaction(
            id = ticketReference,
            userId = authUser?.uid.orEmpty(),
            status = "success",
            reference = ticketReference,
            amount = "0",
            gatewayResponse = "Free ticket issued",
            paidAt = boughtTicket.paymentPaidAt,
            createdAt = boughtTicket.paymentCreatedAt,
            channel = "free",
            currency = "GHS",
            customerEmail = boughtTicket.paymentCustomerEmail.ifBlank { authUser?.email.orEmpty() }
        )
    }
}

data class PendingPurchase(
    val event: Event,
    val ticketType: TicketType,
    val email: String
)

sealed class PurchaseOutcome {
    data object Idle : PurchaseOutcome()
    data class Success(val reference: String, val message: String) : PurchaseOutcome()
    data class Failure(val reference: String? = null, val message: String) : PurchaseOutcome()
}

enum class PurchaseFlowState {
    IDLE,
    AUTHORIZING,
    AWAITING_VERIFICATION,
    VERIFYING,
    COMMITTING,
    FAILED
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
