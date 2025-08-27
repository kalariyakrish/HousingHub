package com.example.housinghub.managers

import android.content.Context
import android.util.Log
import com.example.housinghub.model.Booking
import com.example.housinghub.model.Property
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class BookingManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val userSessionManager = UserSessionManager(context)
    
    companion object {
        private const val TAG = "BookingManager"
        private const val COLLECTION_BOOKINGS = "bookings"
        private const val COLLECTION_TENANTS = "tenants"
        private const val COLLECTION_OWNERS = "owners"
    }
    
    /**
     * Create a new booking record
     */
    suspend fun createBooking(
        property: Property,
        startDate: Date,
        durationMonths: Int,
        numberOfOccupants: Int,
        specialNotes: String
    ): Result<Booking> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            val tenantName = userSessionManager.getFullName()
            val tenantPhone = userSessionManager.getPhone()
            
            if (tenantEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            // Calculate end date
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.MONTH, durationMonths)
            val endDate = calendar.time
            
            // Generate booking ID
            val bookingId = firestore.collection(COLLECTION_BOOKINGS).document().id
            
            // Create booking object
            val booking = Booking(
                id = bookingId,
                tenantEmail = tenantEmail,
                tenantName = tenantName,
                tenantPhone = tenantPhone,
                ownerEmail = property.ownerId,
                propertyId = property.id,
                propertyTitle = property.title,
                propertyLocation = property.location.ifEmpty { property.address },
                propertyPrice = property.price,
                startDate = Timestamp(startDate),
                endDate = Timestamp(endDate),
                durationMonths = durationMonths,
                numberOfOccupants = numberOfOccupants,
                specialNotes = specialNotes,
                totalAmount = property.price * durationMonths,
                securityDeposit = property.price * Booking.SECURITY_DEPOSIT_MONTHS,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            Log.d(TAG, "Creating booking: $bookingId for property: ${property.id}")
            Result.success(booking)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save booking to Firestore after successful payment
     */
    suspend fun saveBooking(booking: Booking): Result<Unit> {
        return try {
            Log.d(TAG, "Saving booking: ${booking.id}")
            Log.d(TAG, "Booking details: tenantEmail=${booking.tenantEmail}, ownerEmail=${booking.ownerEmail}, propertyId=${booking.propertyId}")
            
            // Ensure booking has all required data
            if (booking.tenantEmail.isEmpty() || booking.ownerEmail.isEmpty() || booking.propertyId.isEmpty()) {
                throw Exception("Missing required booking data: tenantEmail, ownerEmail, or propertyId")
            }
            
            // Save main booking record
            firestore.collection(COLLECTION_BOOKINGS)
                .document(booking.id)
                .set(booking)
                .await()
            
            Log.d(TAG, "Main booking record saved successfully")
            
            // Save reference in tenant's bookings
            firestore.collection(COLLECTION_TENANTS)
                .document(booking.tenantEmail)
                .collection("bookings")
                .document(booking.id)
                .set(mapOf(
                    "bookingId" to booking.id,
                    "propertyTitle" to booking.propertyTitle,
                    "propertyLocation" to booking.propertyLocation,
                    "startDate" to booking.startDate,
                    "endDate" to booking.endDate,
                    "status" to booking.bookingStatus,
                    "amountPaid" to booking.amountPaid,
                    "createdAt" to booking.createdAt
                ))
                .await()
            
            Log.d(TAG, "Tenant booking reference saved successfully")
            
            // Save reference in owner's property bookings
            firestore.collection("owner_properties")
                .document(booking.ownerEmail)
                .collection("properties")
                .document(booking.propertyId)
                .collection("bookings")
                .document(booking.id)
                .set(mapOf(
                    "bookingId" to booking.id,
                    "tenantName" to booking.tenantName,
                    "tenantEmail" to booking.tenantEmail,
                    "tenantPhone" to booking.tenantPhone,
                    "startDate" to booking.startDate,
                    "endDate" to booking.endDate,
                    "status" to booking.bookingStatus,
                    "amountPaid" to booking.amountPaid,
                    "numberOfOccupants" to booking.numberOfOccupants,
                    "createdAt" to booking.createdAt
                ))
                .await()
            
            Log.d(TAG, "Owner booking reference saved successfully")
            Log.d(TAG, "Booking saved successfully: ${booking.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving booking: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update booking after payment success
     */
    suspend fun updateBookingPayment(
        bookingId: String,
        paymentId: String,
        paymentSignature: String,
        razorpayOrderId: String,
        amountPaid: Double
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating booking payment: $bookingId")
            
            val updates = mapOf(
                "paymentId" to paymentId,
                "paymentSignature" to paymentSignature,
                "razorpayOrderId" to razorpayOrderId,
                "amountPaid" to amountPaid,
                "bookingStatus" to Booking.STATUS_CONFIRMED,
                "paymentDate" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Booking payment updated successfully: $bookingId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking payment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get tenant's bookings
     */
    suspend fun getTenantBookings(): Result<List<Booking>> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            if (tenantEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("tenantEmail", tenantEmail)
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${bookings.size} bookings for tenant: $tenantEmail")
            Result.success(bookings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tenant bookings: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get owner's property bookings
     */
    suspend fun getOwnerBookings(): Result<List<Booking>> {
        return try {
            val ownerEmail = userSessionManager.getEmail()
            if (ownerEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("ownerEmail", ownerEmail)
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${bookings.size} bookings for owner: $ownerEmail")
            Result.success(bookings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving owner bookings: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling booking: $bookingId")
            
            val updates = mapOf(
                "bookingStatus" to Booking.STATUS_CANCELLED,
                "cancellationReason" to reason,
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Booking cancelled successfully: $bookingId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking: ${e.message}", e)
            Result.failure(e)
        }
    }
}